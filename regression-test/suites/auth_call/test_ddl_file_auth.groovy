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

import org.junit.Assert;

suite("test_ddl_file_auth","p0,auth_call") {
    String user = 'test_ddl_file_auth_user'
    String pwd = 'C123_567p'
    String dbName = 'test_ddl_file_auth_db'
    String fileName = 'test_ddl_file_auth_file'

    try_sql("DROP USER ${user}")
    try_sql """drop database if exists ${dbName}"""
    sql """CREATE USER '${user}' IDENTIFIED BY '${pwd}'"""
    sql """grant select_priv on regression_test to ${user}"""
    sql """create database ${dbName}"""

    //cloud-mode
    if (isCloudMode()) {
        def clusters = sql " SHOW CLUSTERS; "
        assertTrue(!clusters.isEmpty())
        def validCluster = clusters[0][0]
        sql """GRANT USAGE_PRIV ON CLUSTER `${validCluster}` TO ${user}""";
    }


    String s3_endpoint = getS3Endpoint()
    String bucket = context.config.otherConfigs.get("s3BucketName");
    def dataFilePath = "https://"+"${bucket}"+"."+"${s3_endpoint}"+"/regression/auth_test.key"

    // ddl create,show,drop
    connect(user, "${pwd}", context.config.jdbcUrl) {
        test {
            sql """CREATE FILE "${fileName}" IN ${dbName}
                PROPERTIES
                (
                    "url" = "${dataFilePath}",
                    "catalog" = "internal"
                );"""
            exception "denied"
        }
        test {
            sql """SHOW FILE FROM ${dbName};"""
            exception "denied"
        }
        test {
            sql """DROP FILE "${fileName}" from ${dbName} properties("catalog" = "internal");"""
            exception "denied"
        }
    }
    sql """grant select_priv on ${dbName} to ${user}"""
    connect(user, "${pwd}", context.config.jdbcUrl) {
        sql """SHOW FILE FROM ${dbName};"""
    }
    sql """revoke select_priv on ${dbName} from ${user}"""

    sql """grant admin_priv on *.*.* to ${user}"""
    connect(user, "${pwd}", context.config.jdbcUrl) {
        checkNereidsExecute("""CREATE FILE "${fileName}" IN ${dbName}
                PROPERTIES
                (
                    "url" = "${dataFilePath}",
                    "catalog" = "internal"
                );""")
        sql """use ${dbName}"""
        checkNereidsExecute("SHOW FILE;")
        checkNereidsExecute("SHOW FILE FROM ${dbName};")
        def res = sql """SHOW FILE FROM ${dbName};"""
        assertTrue(res.size() == 1)

        checkNereidsExecute("""DROP FILE "${fileName}" from ${dbName} properties("catalog" = "internal");""")
        res = sql """SHOW FILE FROM ${dbName};"""
        assertTrue(res.size() == 0)
    }

    sql """drop database if exists ${dbName}"""
    try_sql("DROP USER ${user}")
}
