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

package org.apache.doris.qe;

import org.apache.doris.common.Config;
import org.apache.doris.common.Version;
import org.apache.doris.common.util.TimeUtils;
import org.apache.doris.mysql.MysqlHandshakePacket;

import com.google.common.collect.Lists;

import java.lang.reflect.Field;
import java.util.List;

// You can place your global variable in this class with public and VariableMgr.VarAttr annotation.
// You can get this variable from MySQL client with statement `SELECT @@variable_name`,
// and change its value through `SET variable_name = xxx`
// NOTE: If you want access your variable safe, please hold VariableMgr's lock before access.
public final class GlobalVariable {

    public static final int VARIABLE_VERSION_0 = 0;
    public static final int VARIABLE_VERSION_100 = 100;
    public static final int VARIABLE_VERSION_101 = 101;
    public static final int VARIABLE_VERSION_200 = 200;
    public static final int VARIABLE_VERSION_300 = 300;
    public static final int VARIABLE_VERSION_400 = 400;
    public static final int CURRENT_VARIABLE_VERSION = VARIABLE_VERSION_400;
    public static final String VARIABLE_VERSION = "variable_version";

    public static final String VERSION_COMMENT = "version_comment";
    public static final String VERSION = "version";
    public static final String LOWER_CASE_TABLE_NAMES = "lower_case_table_names";
    public static final String LICENSE = "license";
    public static final String LANGUAGE = "language";
    public static final String INIT_CONNECT = "init_connect";
    public static final String SYSTEM_TIME_ZONE = "system_time_zone";
    public static final String QUERY_CACHE_SIZE = "query_cache_size";
    public static final String DEFAULT_ROWSET_TYPE = "default_rowset_type";
    public static final String PERFORMANCE_SCHEMA = "performance_schema";
    public static final String DEFAULT_PASSWORD_LIFETIME = "default_password_lifetime";
    public static final String PASSWORD_HISTORY = "password_history";
    public static final String VALIDATE_PASSWORD_POLICY = "validate_password_policy";
    public static final String SHOW_FULL_DBNAME_IN_INFO_SCHEMA_DB = "show_full_dbname_in_info_schema_db";

    public static final long VALIDATE_PASSWORD_POLICY_DISABLED = 0;
    public static final long VALIDATE_PASSWORD_POLICY_STRONG = 2;

    public static final String SQL_CONVERTER_SERVICE_URL = "sql_converter_service_url";
    public static final String ENABLE_AUDIT_PLUGIN = "enable_audit_plugin";
    public static final String AUDIT_PLUGIN_MAX_BATCH_BYTES = "audit_plugin_max_batch_bytes";
    public static final String AUDIT_PLUGIN_MAX_BATCH_INTERVAL_SEC = "audit_plugin_max_batch_interval_sec";
    public static final String AUDIT_PLUGIN_MAX_SQL_LENGTH = "audit_plugin_max_sql_length";
    public static final String AUDIT_PLUGIN_MAX_INSERT_STMT_LENGTH = "audit_plugin_max_insert_stmt_length";
    public static final String AUDIT_PLUGIN_LOAD_TIMEOUT = "audit_plugin_load_timeout";

    public static final String ENABLE_GET_ROW_COUNT_FROM_FILE_LIST = "enable_get_row_count_from_file_list";
    public static final String READ_ONLY = "read_only";
    public static final String SUPER_READ_ONLY = "super_read_only";
    public static final String DEFAULT_USING_META_CACHE_FOR_EXTERNAL_CATALOG
            = "default_using_meta_cache_for_external_catalog";

    public static final String PARTITION_ANALYZE_BATCH_SIZE = "partition_analyze_batch_size";
    public static final String HUGE_PARTITION_LOWER_BOUND_ROWS = "huge_partition_lower_bound_rows";

    public static final String ENABLE_FETCH_ICEBERG_STATS = "enable_fetch_iceberg_stats";

    public static final String ENABLE_ANSI_QUERY_ORGANIZATION_BEHAVIOR
            = "enable_ansi_query_organization_behavior";

    @VariableMgr.VarAttr(name = VARIABLE_VERSION, flag = VariableMgr.INVISIBLE
            | VariableMgr.READ_ONLY | VariableMgr.GLOBAL)
    public static int variableVersion = CURRENT_VARIABLE_VERSION;

    @VariableMgr.VarAttr(name = VERSION_COMMENT, flag = VariableMgr.READ_ONLY)
    public static String versionComment = "Doris version "
            + Version.DORIS_BUILD_VERSION + "-" + Version.DORIS_BUILD_SHORT_HASH
            + (Config.isCloudMode() ? " (Cloud Mode)" : "");

    @VariableMgr.VarAttr(name = VERSION, flag = VariableMgr.READ_ONLY)
    public static String version = MysqlHandshakePacket.SERVER_VERSION;

    // 0: table names are stored as specified and comparisons are case sensitive.
    // 1: table names are stored in lowercase on disk and comparisons are not case sensitive.
    // 2: table names are stored as given but compared in lowercase.
    @VariableMgr.VarAttr(name = LOWER_CASE_TABLE_NAMES, flag = VariableMgr.READ_ONLY | VariableMgr.GLOBAL)
    public static int lowerCaseTableNames = 0;

    @VariableMgr.VarAttr(name = LICENSE, flag = VariableMgr.READ_ONLY)
    public static String license = "Apache License, Version 2.0";

    @VariableMgr.VarAttr(name = LANGUAGE, flag = VariableMgr.READ_ONLY)
    public static String language = "/palo/share/english/";

    // A string to be executed by the server for each client that connects
    @VariableMgr.VarAttr(name = INIT_CONNECT, flag = VariableMgr.GLOBAL)
    public static volatile String initConnect = "";

    // A string to be executed by the server for each client that connects
    @VariableMgr.VarAttr(name = SYSTEM_TIME_ZONE, flag = VariableMgr.READ_ONLY)
    public static String systemTimeZone = TimeUtils.getSystemTimeZone().getID();

    // The amount of memory allocated for caching query results
    @VariableMgr.VarAttr(name = QUERY_CACHE_SIZE, flag = VariableMgr.GLOBAL)
    public static volatile long queryCacheSize = 1048576;

    @VariableMgr.VarAttr(name = DEFAULT_ROWSET_TYPE, flag = VariableMgr.GLOBAL)
    public static volatile String defaultRowsetType = "beta";

    // add performance schema to support MYSQL JDBC 8.0.16 or later versions.
    @VariableMgr.VarAttr(name = PERFORMANCE_SCHEMA, flag = VariableMgr.READ_ONLY)
    public static String performanceSchema = "OFF";

    @VariableMgr.VarAttr(name = DEFAULT_PASSWORD_LIFETIME, flag = VariableMgr.GLOBAL)
    public static int defaultPasswordLifetime = 0;

    @VariableMgr.VarAttr(name = PASSWORD_HISTORY, flag = VariableMgr.GLOBAL)
    public static int passwordHistory = 0;
    // 0: DISABLED
    // 2: STRONG
    @VariableMgr.VarAttr(name = VALIDATE_PASSWORD_POLICY, flag = VariableMgr.GLOBAL)
    public static long validatePasswordPolicy = 0;

    // If set to true, the db name of TABLE_SCHEMA column in tables in information_schema
    // database will be shown as `ctl.db`. Otherwise, show only `db`.
    // This is used to compatible with some MySQL tools.
    @VariableMgr.VarAttr(name = SHOW_FULL_DBNAME_IN_INFO_SCHEMA_DB, flag = VariableMgr.GLOBAL)
    public static boolean showFullDbNameInInfoSchemaDb = false;

    @VariableMgr.VarAttr(name = SQL_CONVERTER_SERVICE_URL, flag = VariableMgr.GLOBAL)
    public static String sqlConverterServiceUrl = "";

    @VariableMgr.VarAttr(name = ENABLE_AUDIT_PLUGIN, flag = VariableMgr.GLOBAL)
    public static boolean enableAuditLoader = false;

    @VariableMgr.VarAttr(name = AUDIT_PLUGIN_MAX_BATCH_BYTES, flag = VariableMgr.GLOBAL)
    public static long auditPluginMaxBatchBytes = 50 * 1024 * 1024;

    @VariableMgr.VarAttr(name = AUDIT_PLUGIN_MAX_BATCH_INTERVAL_SEC, flag = VariableMgr.GLOBAL)
    public static long auditPluginMaxBatchInternalSec = 60;

    @VariableMgr.VarAttr(name = AUDIT_PLUGIN_MAX_SQL_LENGTH, flag = VariableMgr.GLOBAL)
    public static int auditPluginMaxSqlLength = 4096;

    @VariableMgr.VarAttr(name = AUDIT_PLUGIN_MAX_INSERT_STMT_LENGTH, flag = VariableMgr.GLOBAL,
            description = {"专门用于限制 INSERT 语句的长度。如果该值大于 AUDIT_PLUGIN_MAX_SQL_LENGTH，"
                    + "则使用 AUDIT_PLUGIN_MAX_SQL_LENGTH 的值。"
                    + "如果 INSERT 语句超过该长度，将会被截断。",
                    "This is specifically used to limit the length of INSERT statements. "
                            + "If this value is greater than AUDIT_PLUGIN_MAX_SQL_LENGTH, "
                            + "it will use the value of AUDIT_PLUGIN_MAX_SQL_LENGTH. "
                            + "If an INSERT statement exceeds this length, it will be truncated."})
    public static int auditPluginMaxInsertStmtLength = Integer.MAX_VALUE;

    @VariableMgr.VarAttr(name = AUDIT_PLUGIN_LOAD_TIMEOUT, flag = VariableMgr.GLOBAL)
    public static int auditPluginLoadTimeoutS = 600;

    @VariableMgr.VarAttr(name = ENABLE_GET_ROW_COUNT_FROM_FILE_LIST, flag = VariableMgr.GLOBAL,
            description = {
                    "针对外表，是否允许根据文件列表估算表行数。获取文件列表可能是一个耗时的操作，"
                            + "如果不需要估算表行数或者对性能有影响，可以关闭该功能。",
                    "For external tables, whether to enable getting row count from file list. "
                            + "Getting file list may be a time-consuming operation. "
                            + "If you don't need to estimate the number of rows in the table "
                            + "or it affects performance, you can disable this feature."})
    public static boolean enable_get_row_count_from_file_list = true;

    @VariableMgr.VarAttr(name = READ_ONLY, flag = VariableMgr.GLOBAL,
            description = {"仅用于兼容MySQL生态，暂无实际意义",
                    "Only for compatibility with MySQL ecosystem, no practical meaning"})
    public static boolean read_only = true;

    @VariableMgr.VarAttr(name = SUPER_READ_ONLY, flag = VariableMgr.GLOBAL,
            description = {"仅用于兼容MySQL生态，暂无实际意义",
                    "Only for compatibility with MySQL ecosystem, no practical meaning"})
    public static boolean super_read_only = true;

    @VariableMgr.VarAttr(name = PARTITION_ANALYZE_BATCH_SIZE, flag = VariableMgr.GLOBAL,
            description = {
                "批量收集分区信息的分区数",
                "Number of partitions to collect in one batch."})
    public static int partitionAnalyzeBatchSize = 10;

    @VariableMgr.VarAttr(name = HUGE_PARTITION_LOWER_BOUND_ROWS, flag = VariableMgr.GLOBAL,
            description = {
                "行数超过该值的分区将跳过自动分区收集",
                "This defines the lower size bound for large partitions, which will skip auto partition analyze."})
    public static long hugePartitionLowerBoundRows = 100000000L;

    @VariableMgr.VarAttr(name = ENABLE_FETCH_ICEBERG_STATS, flag = VariableMgr.GLOBAL,
            description = {
                "当HMS catalog中的Iceberg表没有统计信息时，是否通过Iceberg Api获取统计信息",
                "Enable fetch stats for HMS Iceberg table when it's not analyzed."})
    public static boolean enableFetchIcebergStats = false;


    @VariableMgr.VarAttr(name = ENABLE_ANSI_QUERY_ORGANIZATION_BEHAVIOR, flag = VariableMgr.GLOBAL,
            description = {
                    "控制 query organization 的行为。当设置为 true 时使用 ANSI 的 query organization 行为，即作用于整个语句。"
                            + "当设置为 false 时，使用 Doris 历史版本的行为，"
                            + "即 order by 默认只作用于 set operation 的最后一个 operand。",
                    "Controls the behavior of query organization. When set to true, uses the ANSI query"
                            + " organization behavior, which applies to the entire statement. When set to false,"
                            + " uses the behavior of Doris's historical versions, where order by by default only"
                            + " applies to the last operand of the set operation."})
    public static boolean enable_ansi_query_organization_behavior = true;

    // Don't allow creating instance.
    private GlobalVariable() {
    }

    public static List<String> getPersistentGlobalVarNames() {
        List<String> varNames = Lists.newArrayList();
        for (Field field : GlobalVariable.class.getDeclaredFields()) {
            VariableMgr.VarAttr attr = field.getAnnotation(VariableMgr.VarAttr.class);
            if (attr != null && (attr.flag() & VariableMgr.GLOBAL) != 0) {
                varNames.add(attr.name());
            }
        }
        return varNames;
    }
}
