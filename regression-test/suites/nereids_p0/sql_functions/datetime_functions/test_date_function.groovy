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

import java.text.SimpleDateFormat

suite("test_date_function") {
    sql """ SET enable_profile = true """
    sql "SET enable_nereids_planner=true"
    sql "SET enable_fallback_to_original_planner=false"
    def tableName = "test_date_function"

    sql """ DROP TABLE IF EXISTS ${tableName} """
    sql """
            CREATE TABLE IF NOT EXISTS ${tableName} (
                test_datetime datetime NULL COMMENT ""
            ) ENGINE=OLAP
            DUPLICATE KEY(test_datetime)
            COMMENT "OLAP"
            DISTRIBUTED BY HASH(test_datetime) BUCKETS 1
            PROPERTIES (
                "replication_allocation" = "tag.location.default: 1",
                "in_memory" = "false",
                "storage_format" = "V2"
            )
        """
    sql """ insert into ${tableName} values ("2019-08-01 13:21:03") """
    // convert_tz
    qt_sql """ SELECT convert_tz(test_datetime, 'Asia/Shanghai', 'America/Los_Angeles') result from ${tableName}; """
    qt_sql """ SELECT convert_tz(test_datetime, '+08:00', 'America/Los_Angeles') result from ${tableName}; """

    qt_sql """ SELECT convert_tz(test_datetime, 'Asia/Shanghai', 'Europe/London') result from ${tableName}; """
    qt_sql """ SELECT convert_tz(test_datetime, '+08:00', 'Europe/London') result from ${tableName}; """

    qt_sql """ SELECT convert_tz(test_datetime, '+08:00', 'America/London') result from ${tableName}; """

    // some invalid date
    qt_sql """ SELECT convert_tz('2022-2-29 13:21:03', '+08:00', 'America/London') result; """
    qt_sql """ SELECT convert_tz('2022-02-29 13:21:03', '+08:00', 'America/London') result; """
    qt_sql """ SELECT convert_tz('1900-00-00 13:21:03', '+08:00', 'America/London') result; """

    sql """ truncate table ${tableName} """

    // test convert_tz for datetimev2
    def tableScale6 = "dtv2s6"
    sql """ DROP TABLE IF EXISTS ${tableScale6} """
    sql """
            CREATE TABLE IF NOT EXISTS ${tableScale6} (
                k1 datetimev2(6) NULL COMMENT ""
            ) ENGINE=OLAP
            DUPLICATE KEY(k1)
            COMMENT "OLAP"
            DISTRIBUTED BY HASH(k1) BUCKETS 1
            PROPERTIES (
                "replication_allocation" = "tag.location.default: 1",
                "in_memory" = "false",
                "storage_format" = "V2"
            )
        """
    sql """ insert into ${tableScale6} values ("2019-08-01 13:21:03.000123"),("2019-08-01 13:21:03.123") """
    qt_sql """ SELECT * from ${tableScale6} order by k1 """
    // convert_tz
    qt_sql """ SELECT k1, convert_tz(k1, 'Asia/Shanghai', 'America/Los_Angeles') result from ${tableScale6} order by k1 """
    qt_sql """ SELECT k1, convert_tz(k1, '+08:00', 'America/Los_Angeles') result from ${tableScale6} order by k1 """
    qt_sql """ SELECT k1, convert_tz(k1, 'Asia/Shanghai', 'Europe/London') result from ${tableScale6} order by k1 """
    qt_sql """ SELECT k1, convert_tz(k1, '+08:00', 'Europe/London') result from ${tableScale6} order by k1 """
    qt_sql """ SELECT k1, convert_tz(k1, '+08:00', 'America/London') result from ${tableScale6} order by k1 """
    qt_sql """ SELECT convert_tz('2019-08-01 01:01:02.123' , '+00:00', '+07:00') """

    def timezoneCachedTableName = "test_convert_tz_with_timezone_cache"
    sql """ DROP TABLE IF EXISTS ${timezoneCachedTableName} """
    sql """
        CREATE TABLE ${timezoneCachedTableName} (
            id int,
            test_datetime datetime NULL COMMENT "",
            origin_tz VARCHAR(255),
            target_tz VARCHAR(255)
        ) ENGINE=OLAP
        DUPLICATE KEY(id)
        COMMENT "OLAP"
        DISTRIBUTED BY HASH(id) BUCKETS 1
        PROPERTIES (
            "replication_allocation" = "tag.location.default: 1",
            "in_memory" = "false",
            "storage_format" = "V2"
        )
    """

    sql """
        INSERT INTO ${timezoneCachedTableName} VALUES
            (1, "2019-08-01 13:21:03", "Asia/Shanghai", "Asia/Shanghai"),
            (2, "2019-08-01 13:21:03", "Asia/Singapore", "Asia/Shanghai"),
            (3, "2019-08-01 13:21:03", "Asia/Taipei", "Asia/Shanghai"),
            (4, "2019-08-02 13:21:03", "Australia/Queensland", "Asia/Shanghai"),
            (5, "2019-08-02 13:21:03", "Australia/Lindeman", "Asia/Shanghai"),
            (6, "2019-08-03 13:21:03", "America/Aruba", "Asia/Shanghai"),
            (7, "2019-08-03 13:21:03", "America/Blanc-Sablon", "Asia/Shanghai"),
            (8, "2019-08-04 13:21:03", "America/Dawson", "Africa/Lusaka"),
            (9, "2019-08-04 13:21:03", "America/Creston", "Africa/Lusaka"),
            (10, "2019-08-05 13:21:03", "Asia/Shanghai", "Asia/Shanghai"),
            (11, "2019-08-05 13:21:03", "Asia/Shanghai", "Asia/Singapore"),
            (12, "2019-08-05 13:21:03", "Asia/Shanghai", "Asia/Taipei"),
            (13, "2019-08-06 13:21:03", "Asia/Shanghai", "Australia/Queensland"),
            (14, "2019-08-06 13:21:03", "Asia/Shanghai", "Australia/Lindeman"),
            (15, "2019-08-07 13:21:03", "Asia/Shanghai", "America/Aruba"),
            (16, "2019-08-07 13:21:03", "Asia/Shanghai", "America/Blanc-Sablon"),
            (17, "2019-08-08 13:21:03", "Africa/Lusaka", "America/Dawson"),
            (18, "2019-08-08 13:21:03", "Africa/Lusaka", "America/Creston")
    """

    sql "set parallel_pipeline_task_num = 8"

    qt_sql1 """
        SELECT
            `id`, `test_datetime`, `origin_tz`, `target_tz`, convert_tz(`test_datetime`, `origin_tz`, `target_tz`)
        FROM
            ${timezoneCachedTableName}
        ORDER BY `id`
    """
    qt_sql2 """
        SELECT
            convert_tz(`test_datetime`, `origin_tz`, `target_tz`),
            convert_tz(`test_datetime`, "Asia/Singapore", `target_tz`),
            convert_tz(`test_datetime`, `origin_tz`, "Asia/Shanghai")
        FROM
            ${timezoneCachedTableName}
        WHERE
            id = 2;
    """
    qt_sql3 """
        SELECT
            convert_tz(`test_datetime`, `origin_tz`, `target_tz`),
            convert_tz(`test_datetime`, "Australia/Queensland", `target_tz`),
            convert_tz(`test_datetime`, `origin_tz`, "Asia/Shanghai")
        FROM
            ${timezoneCachedTableName}
        WHERE
            id = 4;
    """
    qt_sql4 """
        SELECT
            convert_tz(`test_datetime`, `origin_tz`, `target_tz`),
            convert_tz(`test_datetime`, "America/Dawson", `target_tz`),
            convert_tz(`test_datetime`, `origin_tz`, "Africa/Lusaka")
        FROM
            ${timezoneCachedTableName}
        WHERE
            id = 8;
    """

    qt_sql_vec1 """
        SELECT
            `id`, `test_datetime`, `origin_tz`, `target_tz`, convert_tz(`test_datetime`, `origin_tz`, `target_tz`)
        FROM
            ${timezoneCachedTableName}
        ORDER BY `id`
    """
    qt_sql_vec2 """
        SELECT
            convert_tz(`test_datetime`, `origin_tz`, `target_tz`),
            convert_tz(`test_datetime`, "Asia/Singapore", `target_tz`),
            convert_tz(`test_datetime`, `origin_tz`, "Asia/Shanghai")
        FROM
            ${timezoneCachedTableName}
        WHERE
            id = 2;
    """
    qt_sql_vec3 """
        SELECT
            convert_tz(`test_datetime`, `origin_tz`, `target_tz`),
            convert_tz(`test_datetime`, "Australia/Queensland", `target_tz`),
            convert_tz(`test_datetime`, `origin_tz`, "Asia/Shanghai")
        FROM
            ${timezoneCachedTableName}
        WHERE
            id = 4;
    """
    qt_sql_vec4 """
        SELECT
            convert_tz(`test_datetime`, `origin_tz`, `target_tz`),
            convert_tz(`test_datetime`, "America/Dawson", `target_tz`),
            convert_tz(`test_datetime`, `origin_tz`, "Africa/Lusaka")
        FROM
            ${timezoneCachedTableName}
        WHERE
            id = 8;
    """

    // curdate,current_date
    String curdate_str = new SimpleDateFormat("yyyy-MM-dd").format(new Date())
    def curdate_result = sql """ SELECT CURDATE() """
    def curdate_date_result = sql """ SELECT CURRENT_DATE() """
    assertTrue(curdate_str == curdate_result[0][0].toString())
    assertTrue(curdate_str == curdate_date_result[0][0].toString())

    // DATETIME CURRENT_TIMESTAMP()
    def current_timestamp_result = """ SELECT current_timestamp() """
    assertTrue(current_timestamp_result[0].size() == 1)

    // TIME CURTIME()
    def curtime_result = sql """ SELECT CURTIME() """
    assertTrue(curtime_result[0].size() == 1)

    sql """ insert into ${tableName} values ("2010-11-30 23:59:59") """
    // DATE_ADD
    qt_sql """ select date_add(test_datetime, INTERVAL 2 YEAR) result from ${tableName}; """
    qt_sql """ select date_add(test_datetime, INTERVAL 2 MONTH) result from ${tableName}; """
    qt_sql """ select date_add(test_datetime, INTERVAL 2 DAY) result from ${tableName}; """
    qt_sql """ select date_add(test_datetime, INTERVAL 2 HOUR) result from ${tableName}; """
    qt_sql """ select date_add(test_datetime, INTERVAL 2 MINUTE) result from ${tableName}; """
    qt_sql """ select date_add(test_datetime, INTERVAL 2 SECOND) result from ${tableName}; """

    // DATE_FORMAT
    sql """ truncate table ${tableName} """
    sql """ insert into ${tableName} values ("2009-10-04 22:23:00") """
    def resArray = ["Sunday October 2009", "星期日 十月 2009"]
    def res = sql  """ select date_format(test_datetime, '%W %M %Y') from ${tableName}; """
    assertTrue(resArray.contains(res[0][0]))
    sql """ truncate table ${tableName} """
    sql """ insert into ${tableName} values ("2007-10-04 22:23:00") """
    qt_sql """ select date_format(test_datetime, '%H:%i:%s') from ${tableName};"""
    sql """ truncate table ${tableName} """
    sql """ insert into ${tableName} values ("1900-10-04 22:23:00") """
    qt_sql """ select date_format(test_datetime, '%D %y %a %d %m %b %j') from ${tableName}; """
    sql """ truncate table ${tableName} """
    sql """ insert into ${tableName} values ("1997-10-04 22:23:00") """
    qt_sql """ select date_format(test_datetime, '%H %k %I %r %T %S %w') from ${tableName}; """
    sql """ truncate table ${tableName} """
    sql """ insert into ${tableName} values ("1999-01-01 00:00:00") """
    qt_sql """ select date_format(test_datetime, '%X %V') from ${tableName}; """
    sql """ truncate table ${tableName} """
    sql """ insert into ${tableName} values ("2006-06-01") """
    qt_sql """ select date_format(test_datetime, '%d') from ${tableName}; """
    qt_sql """ select date_format(test_datetime, '%%%d') from ${tableName}; """
    sql """ truncate table ${tableName} """
    sql """ insert into ${tableName} values ("2009-10-04 22:23:00") """
    qt_sql """ select date_format(test_datetime, 'yyyy-MM-dd') from ${tableName}; """
    qt_sql_date_format_long """ select date_format(test_datetime, '%f %V %f %l %V %I %S %p %w %r %j %f %l %I %D %w %j %D %e %s %V %f %D %M %s %X %U %v %c %u %x %r %j %a %h %s %m %a %v %u %b') from ${tableName};"""
    qt_sql_date_format_long """ select date_format(non_nullable(test_datetime), '%f %V %f %l %V %I %S %p %w %r %j %f %l %I %D %w %j %D %e %s %V %f %D %M %s %X %U %v %c %u %x %r %j %a %h %s %m %a %v %u %b') from ${tableName};"""
  
    sql """ truncate table ${tableName} """

    sql """ insert into ${tableName} values ("2010-11-30 23:59:59") """
    // DATE_SUB
    qt_sql """ select date_sub(test_datetime, INTERVAL 2 YEAR) from ${tableName};"""
    qt_sql """ select date_sub(test_datetime, INTERVAL 2 MONTH) from ${tableName};"""
    qt_sql """ select date_sub(test_datetime, INTERVAL 2 DAY) from ${tableName};"""
    qt_sql """ select date_sub(test_datetime, INTERVAL 2 HOUR) from ${tableName};"""
    qt_sql """ select date_sub(test_datetime, INTERVAL 2 MINUTE) from ${tableName};"""
    qt_sql """ select date_sub(test_datetime, INTERVAL 2 SECOND) from ${tableName};"""


    // DATEDIFF
    qt_sql """ select datediff(CAST('2007-12-31 23:59:59' AS DATETIME), CAST('2007-12-30' AS DATETIME)) """
    qt_sql """ select datediff(CAST('2010-11-30 23:59:59' AS DATETIME), CAST('2010-12-31' AS DATETIME)) """
    qt_sql """ select datediff('2010-10-31', '2010-10-15') """

    // DAY
    qt_sql """ select day('1987-01-31') """
    qt_sql """ select day('2004-02-29') """

    // DAYNAME
    qt_sql """ select dayname('2007-02-03 00:00:00') """

    // DAYOFMONTH
    qt_sql """ select dayofmonth('1987-01-31') """

    // DAYOFWEEK
    qt_sql """ select dayofweek('2019-06-25') """
    qt_sql """ select dayofweek(cast(20190625 as date)) """

    // DAYOFYEAR
    qt_sql """ select dayofyear('2007-02-03 10:00:00') """
    qt_sql """ select dayofyear('2007-02-03') """

    // FROM_DAYS
    // 通过距离0000-01-01日的天数计算出哪一天
    qt_sql """ select from_days(730669) """
    qt_sql """ select from_days(1) """

    // FROM_UNIXTIME
    qt_sql """ select /*+SET_VAR(time_zone="Asia/Hong_Kong")*/ from_unixtime(1196440219) """
    qt_sql """ select /*+SET_VAR(time_zone="Asia/Hong_Kong")*/ from_unixtime(1196440219, 'yyyy-MM-dd HH:mm:ss') """
    qt_sql """ select /*+SET_VAR(time_zone="Asia/Hong_Kong")*/ from_unixtime(1196440219, '%Y-%m-%d') """
    qt_sql """ select /*+SET_VAR(time_zone="Asia/Hong_Kong")*/ from_unixtime(1196440219, '%Y-%m-%d %H:%i:%s') """
    qt_sql """ select /*+SET_VAR(time_zone="Asia/Hong_Kong")*/ from_unixtime(253402272000, '%Y-%m-%d %H:%i:%s') """

    // HOUR
    qt_sql """ select hour('2018-12-31 23:59:59') """
    qt_sql """ select hour('2018-12-31') """

    // MAKEDATE
    qt_sql """ select makedate(2021,1), makedate(2021,100), makedate(2021,400) """

    // MINUTE
    qt_sql """ select minute('2018-12-31 23:59:59') """
    qt_sql """ select minute('2018-12-31') """

    // MONTH
    qt_sql """ select month('1987-01-01 23:59:59') """
    qt_sql """ select month('1987-01-01') """

    // MONTHNAME
    qt_sql """ select monthname('2008-02-03 00:00:00') """
    qt_sql """ select monthname('2008-02-03') """

    // NOW
    def now_result = sql """ select now() """
    assertTrue(now_result[0].size() == 1)

    // SECOND
    qt_sql """ select second('2018-12-31 23:59:59') """
    qt_sql """ select second('2018-12-31 00:00:00') """

    // MICROSECOND
    qt_sql """ select microsecond(cast('1999-01-02 10:11:12.067890' as datetimev2(6))); """

    // STR_TO_DATE
    sql """ truncate table ${tableName} """
    sql """ insert into ${tableName} values ("2014-12-21 12:34:56")  """
    qt_sql """ select str_to_date(test_datetime, '%Y-%m-%d %H:%i:%s') from ${tableName}; """
    qt_sql """ select str_to_date("2014-12-21 12:34%3A56", '%Y-%m-%d %H:%i%%3A%s'); """
    qt_sql """ select str_to_date("2014-12-21 12:34:56.789 PM", '%Y-%m-%d %h:%i:%s.%f %p'); """
    qt_sql """ select str_to_date('2023-07-05T02:09:55.880Z','%Y-%m-%dT%H:%i:%s.%fZ') """
    qt_sql """ select str_to_date('200442 Monday', '%X%V %W') """
    sql """ truncate table ${tableName} """
    sql """ insert into ${tableName} values ("2020-09-01")  """
    qt_sql """ select str_to_date(test_datetime, "%Y-%m-%d %H:%i:%s") from ${tableName};"""

    // TIME_ROUND
    qt_sql """ SELECT YEAR_FLOOR('20200202000000') """
    qt_sql """ SELECT MONTH_CEIL(CAST('2020-02-02 13:09:20' AS DATETIME), 3) """
    qt_sql """ SELECT WEEK_CEIL('2020-02-02 13:09:20', '2020-01-06') """
    qt_sql """ SELECT MONTH_CEIL(CAST('2020-02-02 13:09:20' AS DATETIME), 3, CAST('1970-01-09 00:00:00' AS DATETIME)) """

    // TIMEDIFF
    qt_sql """ SELECT TIMEDIFF(now(),utc_timestamp()) """
    qt_sql """ SELECT TIMEDIFF('2019-07-11 16:59:30','2019-07-11 16:59:21') """
    qt_sql """ SELECT TIMEDIFF('2019-01-01 00:00:00', NULL) """

    // TIMESTAMPADD
    sql """ truncate table ${tableName} """
    sql """ insert into ${tableName} values ("2019-01-02") ; """
    qt_sql """ SELECT TIMESTAMPADD(YEAR,1,test_datetime) from ${tableName}; """
    qt_sql """ SELECT TIMESTAMPADD(MONTH,1,test_datetime) from ${tableName}; """
    qt_sql """ SELECT TIMESTAMPADD(WEEK,1,test_datetime) from ${tableName}; """
    qt_sql """ SELECT TIMESTAMPADD(DAY,1,test_datetime) from ${tableName}; """
    qt_sql """ SELECT TIMESTAMPADD(HOUR,1,test_datetime) from ${tableName}; """
    qt_sql """ SELECT TIMESTAMPADD(MINUTE,1,test_datetime) from ${tableName}; """
    qt_sql """ SELECT TIMESTAMPADD(SECOND,1,test_datetime) from ${tableName}; """

    // TIMESTAMPDIFF
    qt_sql """ SELECT TIMESTAMPDIFF(MONTH,'2003-02-01','2003-05-01') """
    qt_sql """ SELECT TIMESTAMPDIFF(YEAR,'2002-05-01','2001-01-01') """
    qt_sql """ SELECT TIMESTAMPDIFF(MINUTE,'2003-02-01','2003-05-01 12:05:55') """
    qt_sql """ SELECT TIMESTAMPDIFF(SECOND,'2003-02-01','2003-05-01') """
    qt_sql """ SELECT TIMESTAMPDIFF(HOUR,'2003-02-01','2003-05-01') """
    qt_sql """ SELECT TIMESTAMPDIFF(DAY,'2003-02-01','2003-05-01') """
    qt_sql """ SELECT TIMESTAMPDIFF(WEEK,'2003-02-01','2003-05-01') """

    // TO_DAYS
    qt_sql """ select to_days('2007-10-07') """
    qt_sql """ select to_days('2050-10-07') """

    // UNIX_TIMESTAMP
    def unin_timestamp_str = """ select unix_timestamp() """
    assertTrue(unin_timestamp_str[0].size() == 1)
    qt_sql_ustamp1 """ select unix_timestamp('2007-11-30 10:30:19') """
    qt_sql_ustamp2 """ select unix_timestamp('2007-11-30 10:30-19', '%Y-%m-%d %H:%i-%s') """
    qt_sql_ustamp3 """ select unix_timestamp('2007-11-30 10:30%3A19', '%Y-%m-%d %H:%i%%3A%s') """
    qt_sql_ustamp4 """ select unix_timestamp('1969-01-01 00:00:00') """
    qt_sql_ustamp5 """ select unix_timestamp('2007-11-30 10:30:19.123456') """
    qt_sql_ustamp6 """ select unix_timestamp(cast('2007-11-30 10:30:19.123456' as datetimev2(3))) """
    qt_sql_ustamp7 """ select unix_timestamp(cast('2007-11-30 10:30:19.123456' as datetimev2(4))) """
    qt_sql_ustamp8 """ SELECT UNIX_TIMESTAMP('9999-12-30 23:59:59.999'); """
    qt_sql_ustamp9 """ SELECT UNIX_TIMESTAMP('9999-12-30 23:59:59'); """
    testFoldConst("SELECT UNIX_TIMESTAMP('9999-12-30 23:59:59.999');")
    testFoldConst("SELECT UNIX_TIMESTAMP('9999-12-30 23:59:59');")
    // these two functions may return different value if we call it in different time. so dont use testFoldConst here
    sql "set debug_skip_fold_constant=true;"
    sql "SELECT UNIX_TIMESTAMP(current_timestamp()) AS unix_timestamp;"
    sql "SELECT UNIX_TIMESTAMP(localtimestamp()) AS unix_timestamp;"
    sql "set debug_skip_fold_constant=false;"
    sql "SELECT UNIX_TIMESTAMP(current_timestamp()) AS unix_timestamp;"
    sql "SELECT UNIX_TIMESTAMP(localtimestamp()) AS unix_timestamp;"

    // UTC_TIMESTAMP
    def utc_timestamp_str = sql """ select utc_timestamp(),utc_timestamp() + 1 """
    assertTrue(utc_timestamp_str[0].size() == 2)
    // WEEK
    qt_sql """ select week('2020-1-1') """
    qt_sql """ select week('2020-7-1',1) """

    // WEEKDAY
    qt_sql """ select weekday('2019-06-25'); """
    qt_sql """ select weekday(cast(20190625 as date)); """

    // WEEKOFYEAR
    qt_sql """ select weekofyear('2008-02-20 00:00:00') """

    sql """ truncate table ${tableName} """
    sql """ insert into ${tableName} values ("2019-08-01 13:21:03"), ("9999-08-01 13:21:03"),("0-08-01 13:21:03")"""

    // YEAR
    qt_sql """ select year('1987-01-01') """
    qt_sql """ select year('2050-01-01') """
    qt_sql """ select test_datetime, year(test_datetime) from ${tableName} order by test_datetime """

    // YEARWEEK
    qt_sql """ select yearweek('2021-1-1') """
    qt_sql """ select yearweek('2020-7-1') """
    qt_sql """ select yearweek('1989-03-21', 0) """
    qt_sql """ select yearweek('1989-03-21', 1) """
    qt_sql """ select yearweek('1989-03-21', 2) """
    qt_sql """ select yearweek('1989-03-21', 3) """
    qt_sql """ select yearweek('1989-03-21', 4) """
    qt_sql """ select yearweek('1989-03-21', 5) """
    qt_sql """ select yearweek('1989-03-21', 6) """
    qt_sql """ select yearweek('1989-03-21', 7) """


    // microsecond
    sql """ drop table ${tableName} """
    tableName = "test_microsecond"
    sql """ DROP TABLE IF EXISTS ${tableName} """
    sql """
           CREATE TABLE IF NOT EXISTS ${tableName} (k1 datetimev2(6)) duplicate key(k1) distributed by hash(k1) buckets 1 properties('replication_num' = '1');
        """
    sql """ insert into ${tableName} values('1999-01-02 10:11:12.767891') """

    qt_sql """ select microsecond(k1) from ${tableName}; """
    qt_sql_ms_use_date "select microsecond(cast(k1 as date)) from ${tableName};"

    // qt_sql """ select count(*) from (select * from numbers("number" = "200")) tmp1 WHERE 0 <= UNIX_TIMESTAMP(); """

    sql """ drop table ${tableName} """

    tableName = "test_from_unixtime"

    sql """ DROP TABLE IF EXISTS ${tableName} """
    sql """
            CREATE TABLE IF NOT EXISTS ${tableName} (
                `id` INT NOT NULL COMMENT "用户id",
                `update_time` INT NOT NULL COMMENT "数据灌入日期时间"
            ) ENGINE=OLAP
            UNIQUE KEY(`id`)
            DISTRIBUTED BY HASH(`id`)
            PROPERTIES("replication_num" = "1");
        """
    sql """ insert into ${tableName} values (1, 1659344431) """
    sql """ insert into ${tableName} values (2, 1659283200) """
    sql """ insert into ${tableName} values (3, 1659283199) """
    sql """ insert into ${tableName} values (4, 1659283201) """

    qt_sql """ SELECT id,FROM_UNIXTIME(update_time,"%Y-%m-%d") FROM ${tableName} WHERE FROM_UNIXTIME(update_time,"%Y-%m-%d") = '2022-08-01' ORDER BY id; """
    qt_sql """ SELECT id,FROM_UNIXTIME(update_time,"%Y-%m-%d") FROM ${tableName} WHERE FROM_UNIXTIME(update_time,"%Y-%m-%d") > '2022-08-01' ORDER BY id; """
    qt_sql """ SELECT id,FROM_UNIXTIME(update_time,"%Y-%m-%d") FROM ${tableName} WHERE FROM_UNIXTIME(update_time,"%Y-%m-%d") < '2022-08-01' ORDER BY id; """
    qt_sql """ SELECT id,FROM_UNIXTIME(update_time,"%Y-%m-%d") FROM ${tableName} WHERE FROM_UNIXTIME(update_time,"%Y-%m-%d") >= '2022-08-01' ORDER BY id; """
    qt_sql """ SELECT id,FROM_UNIXTIME(update_time,"%Y-%m-%d") FROM ${tableName} WHERE FROM_UNIXTIME(update_time,"%Y-%m-%d") <= '2022-08-01' ORDER BY id; """
    qt_sql """ SELECT id,FROM_UNIXTIME(update_time,"%Y-%m-%d") FROM ${tableName} WHERE FROM_UNIXTIME(update_time,"%Y-%m-%d") LIKE '2022-08-01' ORDER BY id; """

    qt_sql """ SELECT id,FROM_UNIXTIME(update_time,"%Y-%m-%d %H:%i:%s") FROM ${tableName} WHERE FROM_UNIXTIME(update_time,"%Y-%m-%d %H:%i:%s") = '2022-08-01 00:00:00' ORDER BY id; """
    qt_sql """ SELECT id,FROM_UNIXTIME(update_time,"%Y-%m-%d %H:%i:%s") FROM ${tableName} WHERE FROM_UNIXTIME(update_time,"%Y-%m-%d %H:%i:%s") > '2022-08-01 00:00:00' ORDER BY id; """
    qt_sql """ SELECT id,FROM_UNIXTIME(update_time,"%Y-%m-%d %H:%i:%s") FROM ${tableName} WHERE FROM_UNIXTIME(update_time,"%Y-%m-%d %H:%i:%s") < '2022-08-01 00:00:00' ORDER BY id; """
    qt_sql """ SELECT id,FROM_UNIXTIME(update_time,"%Y-%m-%d %H:%i:%s") FROM ${tableName} WHERE FROM_UNIXTIME(update_time,"%Y-%m-%d %H:%i:%s") >= '2022-08-01 00:00:00' ORDER BY id; """
    qt_sql """ SELECT id,FROM_UNIXTIME(update_time,"%Y-%m-%d %H:%i:%s") FROM ${tableName} WHERE FROM_UNIXTIME(update_time,"%Y-%m-%d %H:%i:%s") <= '2022-08-01 00:00:00' ORDER BY id; """
    qt_sql """ SELECT id,FROM_UNIXTIME(update_time,"%Y-%m-%d %H:%i:%s") FROM ${tableName} WHERE FROM_UNIXTIME(update_time,"%Y-%m-%d %H:%i:%s") LIKE '2022-08-01 00:00:00' ORDER BY id; """
    qt_sql """ SELECT id,FROM_UNIXTIME(update_time,"%Y-%m-%d %H:%i:%s") FROM ${tableName} WHERE FROM_UNIXTIME(update_time,"%Y-%m-%d %H:%i:%s") = '2022-08-01 17:00:31' ORDER BY id; """
    qt_sql """ SELECT id,FROM_UNIXTIME(update_time,null) FROM ${tableName} WHERE FROM_UNIXTIME(update_time,"%Y-%m-%d %H:%i:%s") = '2022-08-01 17:00:31' ORDER BY id; """
    qt_sql """ SELECT id,FROM_UNIXTIME(update_time,'%f %V %f %l %V %I %S %p %w %r %j %f %l %I %D %w %j %D %e %s %V %f %D %M %s %X %U %v %c %u %x %r %j %a %h %s %m %a %v %u %b') FROM ${tableName} WHERE FROM_UNIXTIME(update_time,"%Y-%m-%d %H:%i:%s") = '2022-08-01 17:00:31' ORDER BY id; """
    
    qt_sql """SELECT CURDATE() = CURRENT_DATE();"""
    qt_sql """SELECT unix_timestamp(CURDATE()) = unix_timestamp(CURRENT_DATE());"""

    sql """ drop table ${tableName} """

    qt_sql """ select date_format('1999-01-01', '%X %V'); """
    qt_sql """ select date_format('2025-01-01', '%X %V'); """
    qt_sql """ select date_format('2022-08-04', '%X %V %w'); """
    qt_sql_date_format_long """ select date_format(cast('2011-06-24' as DATETIMEV2(0)), '%f %V %f %l %V %I %S %p %w %r %j %f %l %I %D %w %j %D %e %s %V %f %D %M %s %X %U %v %c %u %x %r %j %a %h %s %m %a %v %u %b') """
    qt_sql_date_format_long """ select date_format(null, '%f %V %f %l %V %I %S %p %w %r %j %f %l %I %D %w %j %D %e %s %V %f %D %M %s %X %U %v %c %u %x %r %j %a %h %s %m %a %v %u %b') """
    
    qt_sql """ select STR_TO_DATE('Tue Jul 12 20:00:45 CST 2022', '%a %b %e %H:%i:%s %Y'); """
    qt_sql """ select STR_TO_DATE('Tue Jul 12 20:00:45 CST 2022', '%a %b %e %T CST %Y'); """
    qt_sql """ select STR_TO_DATE('2018-4-2 15:3:28','%Y-%m-%d %H:%i:%s'); """

    qt_sql """ select length(cast(now() as string)), length(cast(now(0) as string)), length(cast(now(1) as string)),
                      length(cast(now(2) as string)), length(cast(now(3) as string)), length(cast(now(4) as string)),
                      length(cast(now(5) as string)), length(cast(now(6) as string)); """
    qt_sql """ select length(cast(current_timestamp() as string)), length(cast(current_timestamp(0) as string)),
                      length(cast(current_timestamp(1) as string)), length(cast(current_timestamp(2) as string)),
                      length(cast(current_timestamp(3) as string)), length(cast(current_timestamp(4) as string)),
                      length(cast(current_timestamp(5) as string)), length(cast(current_timestamp(6) as string)); """


   tableName = "test_time_add_sub_function"

    sql """ DROP TABLE IF EXISTS ${tableName} """
    sql """
            CREATE TABLE IF NOT EXISTS ${tableName} (
                test_time datetime NULL COMMENT "",
                test_time1 datetimev2(3) NULL COMMENT "",
                test_time2 datetimev2(6) NULL COMMENT ""
            ) ENGINE=OLAP
            DUPLICATE KEY(test_time)
            COMMENT "OLAP"
            DISTRIBUTED BY HASH(test_time) BUCKETS 1
            PROPERTIES (
                "replication_allocation" = "tag.location.default: 1",
                "in_memory" = "false",
                "storage_format" = "V2"
            )
        """
    sql """ insert into ${tableName} values ("2019-08-01 13:21:03", "2019-08-01 13:21:03.111", "2019-08-01 13:21:03.111111") """
    //years_add
    qt_sql """ select years_add(test_time,1) result from ${tableName}; """
    //months_add
    qt_sql """ select months_add(test_time,1) result from ${tableName}; """
    //weeks_add
    qt_sql """ select weeks_add(test_time,1) result from ${tableName}; """
    //days_add
    qt_sql """ select days_add(test_time,1) result from ${tableName}; """
    //hours_add
    qt_sql """ select hours_add(test_time,1) result from ${tableName}; """
    //minutes_add
    qt_sql """ select minutes_add(test_time,1) result from ${tableName}; """
    //seconds_add
    qt_sql """ select seconds_add(test_time,1) result from ${tableName}; """

    //years_sub
    qt_sql """ select years_sub(test_time,1) result from ${tableName}; """
    //months_sub
    qt_sql """ select months_sub(test_time,1) result from ${tableName}; """
    //weeks_sub
    qt_sql """ select weeks_sub(test_time,1) result from ${tableName}; """
    //days_sub
    qt_sql """ select days_sub(test_time,1) result from ${tableName}; """
    //hours_sub
    qt_sql """ select hours_sub(test_time,1) result from ${tableName}; """
    //minutes_sub
    qt_sql """ select minutes_sub(test_time,1) result from ${tableName}; """
    //seconds_sub
    qt_sql """ select seconds_sub(test_time,1) result from ${tableName}; """

    qt_sql """ select date_add(NULL, INTERVAL 1 month); """
    qt_sql """ select date_add(NULL, INTERVAL 1 day); """

    //years_add
    qt_sql """ select years_add(test_time1,1) result from ${tableName}; """
    //months_add
    qt_sql """ select months_add(test_time1,1) result from ${tableName}; """
    //weeks_add
    qt_sql """ select weeks_add(test_time1,1) result from ${tableName}; """
    //days_add
    qt_sql """ select days_add(test_time1,1) result from ${tableName}; """
    //hours_add
    qt_sql """ select hours_add(test_time1,1) result from ${tableName}; """
    //minutes_add
    qt_sql """ select minutes_add(test_time1,1) result from ${tableName}; """
    //seconds_add
    qt_sql """ select seconds_add(test_time1,1) result from ${tableName}; """

    //years_sub
    qt_sql """ select years_sub(test_time1,1) result from ${tableName}; """
    //months_sub
    qt_sql """ select months_sub(test_time1,1) result from ${tableName}; """
    //weeks_sub
    qt_sql """ select weeks_sub(test_time1,1) result from ${tableName}; """
    //days_sub
    qt_sql """ select days_sub(test_time1,1) result from ${tableName}; """
    //hours_sub
    qt_sql """ select hours_sub(test_time1,1) result from ${tableName}; """
    //minutes_sub
    qt_sql """ select minutes_sub(test_time1,1) result from ${tableName}; """
    //seconds_sub
    qt_sql """ select seconds_sub(test_time1,1) result from ${tableName}; """

    //years_add
    qt_sql """ select years_add(test_time2,1) result from ${tableName}; """
    //months_add
    qt_sql """ select months_add(test_time2,1) result from ${tableName}; """
    //weeks_add
    qt_sql """ select weeks_add(test_time2,1) result from ${tableName}; """
    //days_add
    qt_sql """ select days_add(test_time2,1) result from ${tableName}; """
    //hours_add
    qt_sql """ select hours_add(test_time2,1) result from ${tableName}; """
    //minutes_add
    qt_sql """ select minutes_add(test_time2,1) result from ${tableName}; """
    //seconds_add
    qt_sql """ select seconds_add(test_time2,1) result from ${tableName}; """

    //years_sub
    qt_sql """ select years_sub(test_time2,1) result from ${tableName}; """
    //months_sub
    qt_sql """ select months_sub(test_time2,1) result from ${tableName}; """
    //weeks_sub
    qt_sql """ select weeks_sub(test_time2,1) result from ${tableName}; """
    //days_sub
    qt_sql """ select days_sub(test_time2,1) result from ${tableName}; """
    //hours_sub
    qt_sql """ select hours_sub(test_time2,1) result from ${tableName}; """
    //minutes_sub
    qt_sql """ select minutes_sub(test_time2,1) result from ${tableName}; """
    //seconds_sub
    qt_sql """ select seconds_sub(test_time2,1) result from ${tableName}; """

    // test last_day for vec
    sql """ DROP TABLE IF EXISTS test_time_add_sub_function; """
    sql """
            CREATE TABLE IF NOT EXISTS test_time_add_sub_function (
                birth date,
                birth1 datev2,
                birth2 datetime,
                birth3 datetimev2)
            UNIQUE KEY(birth, birth1, birth2, birth3)
            DISTRIBUTED BY HASH (birth) BUCKETS 1
            PROPERTIES( "replication_allocation" = "tag.location.default: 1");
        """
    sql """
        insert into test_time_add_sub_function values
        ('2022-01-01', '2022-01-01', '2022-01-01 00:00:00', '2022-01-01 00:00:00'),
        ('2000-02-01', '2000-02-01', '2000-02-01 00:00:00', '2000-02-01 00:00:00.123'),
        ('2022-02-27', '2022-02-27', '2022-02-27 00:00:00', '2022-02-27 00:00:00'),
        ('2022-02-28', '2022-02-28', '2022-02-28T23:59:59', '2022-02-28T23:59:59');"""
    qt_sql """
        select last_day(birth), last_day(birth1),
                last_day(birth2), last_day(birth3)
                from test_time_add_sub_function;
    """

    sql """ DROP TABLE IF EXISTS test_time_add_sub_function; """
    sql """
            CREATE TABLE IF NOT EXISTS ${tableName} (
                birth date,
                birth1 datetime)
            UNIQUE KEY(birth, birth1)
            DISTRIBUTED BY HASH (birth) BUCKETS 1
            PROPERTIES( "replication_allocation" = "tag.location.default: 1");
        """
    sql """
        insert into ${tableName} values
        ('2022-01-01', '2022-01-01 00:00:00'),
        ('2000-02-01', '2000-02-01 00:00:00'),
        ('2022-02-27', '2022-02-27 00:00:00'),
        ('2022-02-28', '2022-02-28 23:59:59');"""
    qt_sql """
        select last_day(birth), last_day(birth1) from ${tableName};
    """
    sql """ DROP TABLE IF EXISTS ${tableName}; """

    // test to_monday
    sql """ DROP TABLE IF EXISTS ${tableName}; """
    sql """
            CREATE TABLE IF NOT EXISTS ${tableName} (
                birth date,
                birth1 datev2,
                birth2 datetime,
                birth3 datetimev2)
            UNIQUE KEY(birth, birth1, birth2, birth3)
            DISTRIBUTED BY HASH (birth) BUCKETS 1
            PROPERTIES( "replication_allocation" = "tag.location.default: 1");
        """
    sql """
        insert into ${tableName} values
        ('2022-01-01', '2022-01-01', '2022-01-01 00:00:00', '2022-01-01 00:00:00'),
        ('2000-02-01', '2000-02-01', '2000-02-01 00:00:00', '2000-02-01 00:00:00.123'),
        ('2022-02-27', '2022-02-27', '2022-02-27 00:00:00', '2022-02-27 00:00:00'),
        ('2022-02-28', '2022-02-28', '2022-02-28 23:59:59', '2022-02-28 23:59:59'),
        ('1970-01-02', '1970-01-02', '1970-01-02 01:02:03', '1970-01-02 02:03:04');"""
    qt_sql """
        select to_monday(birth), to_monday(birth1),
                to_monday(birth2), to_monday(birth3)
                from ${tableName};
    """

    // Test dow and doy
    qt_sql """
        select extract(dow from birth), extract(dow from birth1), extract(dow from birth2), extract(dow from birth3),
               extract(dayofweek from birth), extract(dayofweek from birth1), extract(dayofweek from birth2), extract(dayofweek from birth3),
               extract(doy from birth), extract(doy from birth1), extract(doy from birth2), extract(doy from birth3),
               extract(dayofyear from birth), extract(dayofyear from birth1), extract(dayofyear from birth2), extract(dayofyear from birth3)
               from ${tableName};
    """

    sql """ DROP TABLE IF EXISTS ${tableName}; """

    test {
        sql "select cast('20230631' as date), cast('20230632' as date)"
        result([[null, null]])
    }

    qt_sql """ select date_add("2023-08-17T01:41:18Z", interval 8 hour) """

    sql """ DROP TABLE IF EXISTS dt_null; """
    sql """ CREATE TABLE IF NOT EXISTS dt_null(
            `k1` INT NOT NULL,
            `dtv24` datetimev2(4) NOT NULL,
            `dtv20n` datetimev2(0) NULL,
            `dv2` datev2 NOT NULL,
            `dv2n` datev2 NULL,
            `str` VARCHAR NULL
            )
            DISTRIBUTED BY HASH(`k1`) BUCKETS 5
            properties("replication_num" = "1"); """
    sql """ insert into dt_null values ('1', '2020-12-12', '2020-12-12', '2020-12-12', '2020-12-12', '2020-12-12'),
            ('2', '2020-12-12 12:12:12', '2020-12-12 12:12:12', '2020-12-12 12:12:12', '2020-12-12 12:12:12', '2020-12-12 12:12:12'),
            ('3', '2020-12-12 12:12:12.0', '2020-12-12 12:12:12.0', '2020-12-12 12:12:12.0', '2020-12-12 12:12:12.0', '2020-12-12 12:12:12.0'),
            ('4', '2020-12-12 12:12:12.123', '2020-12-12 12:12:12.123', '2020-12-12 12:12:12.123', '2020-12-12 12:12:12.123', '2020-12-12 12:12:12.123'),
            ('5', '2020-12-12 12:12:12.666666', '2020-12-12 12:12:12.666666', '2020-12-12 12:12:12.666666', '2020-12-12 12:12:12.666666', '2020-12-12 12:12:12.666666'); """

    qt_sql_dt_null_1 """ select unix_timestamp(dtv24), unix_timestamp(dtv20n), unix_timestamp(dv2), unix_timestamp(dv2n), unix_timestamp(str) from dt_null order by k1; """

    sql """ DROP TABLE IF EXISTS dt_timenull; """

    sql """
     CREATE TABLE IF NOT EXISTS dt_timenull(
            `k1` INT NOT NULL,
            `k2` BIGINT NOT NULL
            )
            DISTRIBUTED BY HASH(`k1`) BUCKETS 5
            properties("replication_num" = "1");
    """
    
    sql """ insert into dt_timenull values (1, 0),(2, 100),(3, 123),(4, 219837),(5, -8923),(6, -29313),(7, 2131321231),(8, -21312313),(9,1112345);"""

    qt_sql_time_value """ select k1 , cast(k2 as time) , hour(cast(k2 as time)) , minute(cast(k2 as time)), second(cast(k2 as time)) from dt_timenull order by k1;"""


    qt_sql_time_value """ select  cast(4562632 as time),  hour(cast(4562632 as time)) ,  minute(cast(4562632 as time)) , second(cast(4562632 as time)); """

    def test_simplify = {
        test {
            sql "select months_add(dt, 1) = date '2024-02-29' from (select date '2024-01-31' as dt)a"
            result([[true]])
        }
        test {
            sql "select years_add(dt, 1) = date '2025-02-28' from (select date '2024-02-29' as dt)a"
            result([[true]])
        }
    }()
}
