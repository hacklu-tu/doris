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

suite("test_conditional_function") {
    sql "set batch_size = 4096;"

    def tbName = "test_conditional_function"
    sql "DROP TABLE IF EXISTS ${tbName};"
    sql """
            CREATE TABLE IF NOT EXISTS ${tbName} (
                user_id INT
            )
            DISTRIBUTED BY HASH(user_id) BUCKETS 5 properties("replication_num" = "1");
        """
    sql """
        INSERT INTO ${tbName} VALUES 
            (1),
            (2),
            (3),
            (4);
        """
    sql """
        INSERT INTO ${tbName} VALUES 
            (null),
            (null),
            (null),
            (null);
        """

    qt_sql "select user_id, case user_id when 1 then 'user_id = 1' when 2 then 'user_id = 2' else 'user_id not exist' end test_case from ${tbName} order by user_id;"
    qt_sql "select user_id, case when user_id = 1 then 'user_id = 1' when user_id = 2 then 'user_id = 2' else 'user_id not exist' end test_case from ${tbName} order by user_id;"

    qt_sql "select user_id, if(user_id = 1, \"true\", \"false\") test_if from ${tbName} order by user_id;"

    qt_sql "select coalesce(NULL, '1111', '0000');"

    qt_sql "select ifnull(1,0);"
    qt_sql "select ifnull(null,10);"
    qt_sql "select ifnull(1,user_id) from ${tbName} order by user_id;"
    qt_sql "select ifnull(user_id,1) from ${tbName} order by user_id;"
    qt_sql "select ifnull(null,user_id) from ${tbName} order by user_id;"
    qt_sql "select ifnull(user_id,null) from ${tbName} order by user_id;"

    qt_sql "select nullif(1,1);"
    qt_sql "select nullif(1,0);"
    qt_sql "select nullif(1,user_id) from ${tbName} order by user_id;"
    qt_sql "select nullif(user_id,1) from ${tbName} order by user_id;"
    qt_sql "select nullif(null,user_id) from ${tbName} order by user_id;"
    qt_sql "select nullif(user_id,null) from ${tbName} order by user_id;"


    qt_sql "select nullif(1,1);"
    qt_sql "select nullif(1,0);"


    qt_sql "select is_null_pred(user_id) from ${tbName} order by user_id"
    qt_sql "select is_not_null_pred(user_id) from ${tbName} order by user_id"

    qt_sql """select if(date_format(CONCAT_WS('', '9999-07', '-26'), '%Y-%m')= DATE_FORMAT( curdate(), '%Y-%m'),
	        curdate(),
	        DATE_FORMAT(DATE_SUB(month_ceil ( CONCAT_WS('', '9999-07', '-26')), 1), '%Y-%m-%d'));"""

    qt_sql "select ifnull(date_format(CONCAT_WS('', '9999-07', '-00'), '%Y-%m'),3);"

    qt_sql "select ifnull(date_format(CONCAT_WS('', '9999-07', '-01'), '%Y-%m'),3);"

    qt_sql "select ifnull(date_format(CONCAT_WS('', '9999-07', '-00'), '%Y-%m'),date_format(CONCAT_WS('', '9999-07', '-00'), '%Y-%m'));"

    qt_sql "select ifnull(date_format(CONCAT_WS('', '9999-07', '-00'), '%Y-%m'),date_format(CONCAT_WS('', '9999-07', '-26'), '%Y-%m'));"

    qt_sql "select ifnull( user_id, to_date('9999-01-01')) r from ${tbName} order by r"

    qt_sql "select ifnull( user_id, 999) r from ${tbName} order by r"

    qt_if_true_then_nullable """select IF(true, DAYOFWEEK("2022-12-06 17:48:46"), 1) + 1;"""
    qt_if_true_else_nullable """select IF(true, 1, DAYOFWEEK("2022-12-06 17:48:46")) + 1;"""

    qt_if_false_then_nullable """select IF(false, DAYOFWEEK("2022-12-06 17:48:46"), 1) + 1;"""
    qt_if_false_else_nullable """select IF(false, 1, DAYOFWEEK("2022-12-06 17:48:46")) + 1;"""

    sql 'set enable_fallback_to_original_planner=false'
    sql 'set enable_nereids_planner=true'

    qt_sql "select user_id, case user_id when 1 then 'user_id = 1' when 2 then 'user_id = 2' else 'user_id not exist' end test_case from ${tbName} order by user_id;"
    qt_sql "select user_id, case when user_id = 1 then 'user_id = 1' when user_id = 2 then 'user_id = 2' else 'user_id not exist' end test_case from ${tbName} order by user_id;"

    qt_sql "select user_id, if(user_id = 1, \"true\", \"false\") test_if from ${tbName} order by user_id;"

    qt_sql "select coalesce(NULL, '1111', '0000');"

    qt_sql "select ifnull(1,0);"
    qt_sql "select ifnull(null,10);"
    qt_sql "select ifnull(1,user_id) from ${tbName} order by user_id;"
    qt_sql "select ifnull(user_id,1) from ${tbName} order by user_id;"
    qt_sql "select ifnull(null,user_id) from ${tbName} order by user_id;"
    qt_sql "select ifnull(user_id,null) from ${tbName} order by user_id;"

    qt_sql "select nullif(1,1);"
    qt_sql "select nullif(1,0);"
    qt_sql "select nullif(1,user_id) from ${tbName} order by user_id;"
    qt_sql "select nullif(user_id,1) from ${tbName} order by user_id;"
    qt_sql "select nullif(null,user_id) from ${tbName} order by user_id;"
    qt_sql "select nullif(user_id,null) from ${tbName} order by user_id;"


    qt_sql "select nullif(1,1);"
    qt_sql "select nullif(1,0);"


    qt_sql "select is_null_pred(user_id) from ${tbName} order by user_id"
    qt_sql "select is_not_null_pred(user_id) from ${tbName} order by user_id"

    qt_sql """select if(date_format(CONCAT_WS('', '9999-07', '-26'), '%Y-%m')= DATE_FORMAT( curdate(), '%Y-%m'),
	        curdate(),
	        DATE_FORMAT(DATE_SUB(month_ceil ( CONCAT_WS('', '9999-07', '-26')), 1), '%Y-%m-%d'));"""

    qt_sql "select ifnull(date_format(CONCAT_WS('', '9999-07', '-00'), '%Y-%m'),3);"

    qt_sql "select ifnull(date_format(CONCAT_WS('', '9999-07', '-01'), '%Y-%m'),3);"

    qt_sql "select ifnull(date_format(CONCAT_WS('', '9999-07', '-00'), '%Y-%m'),date_format(CONCAT_WS('', '9999-07', '-00'), '%Y-%m'));"

    qt_sql "select ifnull(date_format(CONCAT_WS('', '9999-07', '-00'), '%Y-%m'),date_format(CONCAT_WS('', '9999-07', '-26'), '%Y-%m'));"

    qt_sql "select ifnull( user_id, to_date('9999-01-01')) r from ${tbName} order by r"

    qt_sql "select ifnull( user_id, 999) r from ${tbName} order by r"

    qt_if_true_then_nullable """select IF(true, DAYOFWEEK("2022-12-06 17:48:46"), 1) + 1;"""
    qt_if_true_else_nullable """select IF(true, 1, DAYOFWEEK("2022-12-06 17:48:46")) + 1;"""

    qt_if_false_then_nullable """select IF(false, DAYOFWEEK("2022-12-06 17:48:46"), 1) + 1;"""
    qt_if_false_else_nullable """select IF(false, 1, DAYOFWEEK("2022-12-06 17:48:46")) + 1;"""

    qt_sql "select date_add('9999-08-01 00:00:00',1);"

    sql "DROP TABLE ${tbName};"

    sql "set enable_decimal256=true"
    sql """ drop table if exists t1; """
    sql """ create table t1(
        k1 int,
        k2 date,
        k22 date,
        k3 datetime,
        k33 datetime,
        k4 decimalv3(76, 6),
        k44 decimalv3(76, 6)
    ) distributed by hash (k1) buckets 1
    properties ("replication_num"="1");
    """
    sql """
    insert into t1 values
    (1, null, '2023-01-02', '2023-01-01 00:00:00', '2023-01-02 00:00:00',2222222222222222222222222222222222222222222222222222222222222222222222,3333333333333333333333333333333333333333333333333333333333333333333333),(2, '2023-02-01', null, '2023-02-01 00:00:00', '2023-02-02 00:00:00', null,5555555555555555555555555555555555555555555555555555555555555555555555),(3, '2023-03-01', '2023-03-02', null, '2023-03-02 00:00:00', null,666666666666666666666666666666666666666666666666)
    """
    qt_test "select k1, coalesce(k2, k22),coalesce(k3, k33) from t1 order by k1"
    // fix after disable decimal256 disable implicit conversion to int128
    // qt_test "select k1, coalesce(k2, k22),coalesce(k4, k44) from t1 order by k1"

}
