// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

#include "result_sink_operator.h"

#include <fmt/format.h>
#include <sys/select.h>

#include <memory>

#include "common/config.h"
#include "exec/rowid_fetcher.h"
#include "pipeline/exec/operator.h"
#include "runtime/exec_env.h"
#include "runtime/result_block_buffer.h"
#include "runtime/result_buffer_mgr.h"
#include "util/arrow/row_batch.h"
#include "vec/exprs/vexpr.h"
#include "vec/exprs/vexpr_context.h"
#include "vec/sink/varrow_flight_result_writer.h"
#include "vec/sink/vmysql_result_writer.h"

namespace doris::pipeline {
#include "common/compile_check_begin.h"

Status ResultSinkLocalState::init(RuntimeState* state, LocalSinkStateInfo& info) {
    RETURN_IF_ERROR(Base::init(state, info));
    SCOPED_TIMER(exec_time_counter());
    SCOPED_TIMER(_init_timer);
    _fetch_row_id_timer = ADD_TIMER(custom_profile(), "FetchRowIdTime");
    _write_data_timer = ADD_TIMER(custom_profile(), "WriteDataTime");
    static const std::string timer_name = "WaitForDependencyTime";
    _wait_for_dependency_timer = ADD_TIMER_WITH_LEVEL(custom_profile(), timer_name, 1);
    auto fragment_instance_id = state->fragment_instance_id();

    auto& p = _parent->cast<ResultSinkOperatorX>();
    _output_vexpr_ctxs.resize(p._output_vexpr_ctxs.size());
    for (size_t i = 0; i < _output_vexpr_ctxs.size(); i++) {
        RETURN_IF_ERROR(p._output_vexpr_ctxs[i]->clone(state, _output_vexpr_ctxs[i]));
    }
    if (state->query_options().enable_parallel_result_sink) {
        _sender = _parent->cast<ResultSinkOperatorX>()._sender;
    } else {
        std::shared_ptr<arrow::Schema> arrow_schema;
        if (p._sink_type == TResultSinkType::ARROW_FLIGHT_PROTOCAL) {
            RETURN_IF_ERROR(get_arrow_schema_from_expr_ctxs(_output_vexpr_ctxs, &arrow_schema,
                                                            state->timezone()));
        }
        VLOG_DEBUG << "create sender in INIT with instance id " << fragment_instance_id;
        RETURN_IF_ERROR(state->exec_env()->result_mgr()->create_sender(
                fragment_instance_id, p._result_sink_buffer_size_rows, &_sender, state,
                p._sink_type == TResultSinkType::ARROW_FLIGHT_PROTOCAL, arrow_schema));
    }
    _sender->set_dependency(fragment_instance_id, _dependency->shared_from_this());
    return Status::OK();
}

Status ResultSinkLocalState::open(RuntimeState* state) {
    SCOPED_TIMER(exec_time_counter());
    SCOPED_TIMER(_open_timer);
    RETURN_IF_ERROR(Base::open(state));
    auto& p = _parent->cast<ResultSinkOperatorX>();
    // create writer based on sink type
    switch (p._sink_type) {
    case TResultSinkType::MYSQL_PROTOCAL: {
        if (state->mysql_row_binary_format()) {
            _writer.reset(new (std::nothrow) vectorized::VMysqlResultWriter<true>(
                    _sender, _output_vexpr_ctxs, custom_profile()));
        } else {
            _writer.reset(new (std::nothrow) vectorized::VMysqlResultWriter<false>(
                    _sender, _output_vexpr_ctxs, custom_profile()));
        }
        break;
    }
    case TResultSinkType::ARROW_FLIGHT_PROTOCAL: {
        _writer.reset(new (std::nothrow) vectorized::VArrowFlightResultWriter(
                _sender, _output_vexpr_ctxs, custom_profile()));
        break;
    }
    default:
        return Status::InternalError("Unknown result sink type");
    }

    RETURN_IF_ERROR(_writer->init(state));
    return Status::OK();
}

ResultSinkOperatorX::ResultSinkOperatorX(int operator_id, const RowDescriptor& row_desc,
                                         const std::vector<TExpr>& t_output_expr,
                                         const TResultSink& sink)
        : DataSinkOperatorX(operator_id, std::numeric_limits<int>::max(),
                            std::numeric_limits<int>::max()),
          _sink_type(!sink.__isset.type || sink.type == TResultSinkType::MYSQL_PROTOCAL
                             ? TResultSinkType::MYSQL_PROTOCAL
                             : sink.type),
          _result_sink_buffer_size_rows(_sink_type == TResultSinkType::ARROW_FLIGHT_PROTOCAL
                                                ? config::arrow_flight_result_sink_buffer_size_rows
                                                : RESULT_SINK_BUFFER_SIZE),
          _row_desc(row_desc),
          _t_output_expr(t_output_expr),
          _fetch_option(sink.fetch_option) {
    _name = "ResultSink";
}

Status ResultSinkOperatorX::prepare(RuntimeState* state) {
    RETURN_IF_ERROR(DataSinkOperatorX<ResultSinkLocalState>::prepare(state));
    // prepare output_expr
    // From the thrift expressions create the real exprs.
    RETURN_IF_ERROR(vectorized::VExpr::create_expr_trees(_t_output_expr, _output_vexpr_ctxs));
    if (_fetch_option.use_two_phase_fetch) {
        for (auto& expr_ctx : _output_vexpr_ctxs) {
            // Must materialize if it a slot, or the slot column id will be -1
            expr_ctx->set_force_materialize_slot();
        }
    }
    // Prepare the exprs to run.
    RETURN_IF_ERROR(vectorized::VExpr::prepare(_output_vexpr_ctxs, state, _row_desc));

    if (state->query_options().enable_parallel_result_sink) {
        std::shared_ptr<arrow::Schema> arrow_schema;
        if (_sink_type == TResultSinkType::ARROW_FLIGHT_PROTOCAL) {
            RETURN_IF_ERROR(get_arrow_schema_from_expr_ctxs(_output_vexpr_ctxs, &arrow_schema,
                                                            state->timezone()));
        }
        VLOG_DEBUG << "create sender in prepare with query id " << state->query_id();
        RETURN_IF_ERROR(state->exec_env()->result_mgr()->create_sender(
                state->query_id(), _result_sink_buffer_size_rows, &_sender, state,
                _sink_type == TResultSinkType::ARROW_FLIGHT_PROTOCAL, arrow_schema));
    }
    return vectorized::VExpr::open(_output_vexpr_ctxs, state);
}

Status ResultSinkOperatorX::sink(RuntimeState* state, vectorized::Block* block, bool eos) {
    auto& local_state = get_local_state(state);
    SCOPED_TIMER(local_state.exec_time_counter());
    COUNTER_UPDATE(local_state.rows_input_counter(), (int64_t)block->rows());
    if (_fetch_option.use_two_phase_fetch && block->rows() > 0) {
        SCOPED_TIMER(local_state._fetch_row_id_timer);
        RETURN_IF_ERROR(_second_phase_fetch_data(state, block));
    }
    {
        SCOPED_TIMER(local_state._write_data_timer);
        RETURN_IF_ERROR(local_state._writer->write(state, *block));
    }
    if (_fetch_option.use_two_phase_fetch) {
        // Block structure may be changed by calling _second_phase_fetch_data().
        // So we should clear block in case of unmatched columns
        block->clear();
    }
    return Status::OK();
}

Status ResultSinkOperatorX::_second_phase_fetch_data(RuntimeState* state,
                                                     vectorized::Block* final_block) {
    auto row_id_col = final_block->get_by_position(final_block->columns() - 1);
    CHECK(row_id_col.name == BeConsts::ROWID_COL);
    auto* tuple_desc = _row_desc.tuple_descriptors()[0];
    FetchOption fetch_option;
    fetch_option.desc = tuple_desc;
    fetch_option.t_fetch_opt = _fetch_option;
    fetch_option.runtime_state = state;
    RowIDFetcher id_fetcher(fetch_option);
    RETURN_IF_ERROR(id_fetcher.init());
    RETURN_IF_ERROR(id_fetcher.fetch(row_id_col.column, final_block));
    return Status::OK();
}

Status ResultSinkLocalState::close(RuntimeState* state, Status exec_status) {
    if (_closed) {
        return Status::OK();
    }
    SCOPED_TIMER(_close_timer);
    SCOPED_TIMER(exec_time_counter());
    COUNTER_SET(_wait_for_dependency_timer, _dependency->watcher_elapse_time());
    Status final_status = exec_status;
    if (_writer) {
        // close the writer
        Status st = _writer->close();
        if (!st.ok() && final_status.ok()) {
            // close file writer failed, should return this error to client
            final_status = st;
        }

        VLOG_NOTICE << fmt::format(
                "Query {} result sink closed with status {} and has written {} rows",
                print_id(state->query_id()), final_status.to_string_no_stack(),
                _writer->get_written_rows());
    }

    // close sender, this is normal path end
    if (_sender) {
        int64_t written_rows = 0;
        if (_writer) {
            written_rows = _writer->get_written_rows();
            state->get_query_ctx()->resource_ctx()->io_context()->update_returned_rows(
                    written_rows);
        }
        RETURN_IF_ERROR(_sender->close(state->fragment_instance_id(), final_status, written_rows));
    }
    state->exec_env()->result_mgr()->cancel_at_time(
            time(nullptr) + config::result_buffer_cancelled_interval_time,
            state->fragment_instance_id());
    RETURN_IF_ERROR(Base::close(state, exec_status));
    return final_status;
}

#include "common/compile_check_end.h"
} // namespace doris::pipeline
