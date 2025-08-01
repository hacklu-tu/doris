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

suite("test_infer_predicate") {
    sql 'set enable_nereids_planner=true'
    sql 'set enable_fallback_to_original_planner=false'
    sql "set disable_nereids_rules=PRUNE_EMPTY_PARTITION"


    sql 'drop table if exists infer_tb1;'
    sql 'drop table if exists infer_tb2;'
    sql 'drop table if exists infer_tb3;'

    sql '''create table infer_tb1 (k1 int, k2 int) distributed by hash(k1) buckets 3 properties('replication_num' = '1');'''

    sql '''create table infer_tb2 (k1 tinyint, k2 smallint, k3 int, k4 bigint, k5 largeint, k6 date, k7 datetime, k8 float, k9 double) distributed by hash(k1) buckets 3 properties('replication_num' = '1');'''

    sql '''create table infer_tb3 (k1 varchar(100), k2 int) distributed by hash(k1) buckets 3 properties('replication_num' = '1');'''

    explain {
        sql "select * from infer_tb1 inner join infer_tb2 where infer_tb2.k1 = infer_tb1.k2  and infer_tb2.k1 = 1;"
        contains "PREDICATES: (k2"
    }

    explain {
        sql "select * from infer_tb1 inner join infer_tb2 where infer_tb1.k2 = infer_tb2.k1  and infer_tb2.k1 = 1;"
        contains "PREDICATES: (k2"
    }

    // InferPredicate not support infer predicate downcast
    // but ConstantPropagation rewrite cast(k4 as int) = k2 to cast(1 as int) = k2,
    // then expression opt rwrite it to k2 = 1
    explain {
        sql "select * from infer_tb1 inner join infer_tb2 where cast(infer_tb2.k4 as int) = infer_tb1.k2  and infer_tb2.k4 = 1;"
        contains "PREDICATES: (k2"
    }

    // InferPredicate not infer k2 = 123, because k1 is string type, k2 is int type
    // but ConstantPropagation rewrite cast(k1 as double) = cast(k2 as double) to cast('123' as double) = cast(k2 as double),
    // then expression opt rewrite it to k2 = 123
    explain {
        sql "select * from infer_tb1 inner join infer_tb3 where infer_tb3.k1 = infer_tb1.k2  and infer_tb3.k1 = '123';"
        contains "PREDICATES: (k2"
    }

    explain {
        sql "select * from infer_tb1 left join infer_tb2 on infer_tb1.k1 = infer_tb2.k3 left join infer_tb3 on " +
                "infer_tb2.k3 = infer_tb3.k2 where infer_tb1.k1 = 1;"
        contains "PREDICATES: (k3"
        // After modifying the logic of pull up predicates from join, the left join left table predicate will not be pulled up.
        // left join left table predicates should not be pulled up. because there may be null value.
        // However, in this case, pulling up seems to be OK, so note for now
        // contains "PREDICATES: (k2"
    }
}
