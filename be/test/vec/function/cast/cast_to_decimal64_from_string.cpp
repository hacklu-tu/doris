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

#include "cast_to_decimal.h"

namespace doris::vectorized {

void FunctionCastToDecimalTest::from_string_to_decimal64_test_func() {
    int table_index = 0;
    int test_data_index = 0;

    from_string_test_func<Decimal64>(18, 0, table_index, test_data_index);
    ++table_index;

    from_string_test_func<Decimal64>(18, 1, table_index, test_data_index);
    ++table_index;
    from_string_test_func<Decimal64>(18, 1, table_index, test_data_index, 0, true);
    ++table_index;
    from_string_test_func<Decimal64>(18, 1, table_index, test_data_index, 1);
    ++table_index;
    from_string_test_func<Decimal64>(18, 1, table_index, test_data_index, 100);
    ++table_index;
    from_string_test_func<Decimal64>(18, 1, table_index, test_data_index, -1);
    ++table_index;
    from_string_test_func<Decimal64>(18, 1, table_index, test_data_index, -17);
    ++table_index;
    from_string_test_func<Decimal64>(18, 1, table_index, test_data_index, -100);
    ++table_index;

    from_string_test_func<Decimal64>(18, 9, table_index, test_data_index);
    ++table_index;
    from_string_test_func<Decimal64>(18, 9, table_index, test_data_index, 0, true);
    ++table_index;
    from_string_test_func<Decimal64>(18, 9, table_index, test_data_index, 1);
    ++table_index;
    from_string_test_func<Decimal64>(18, 9, table_index, test_data_index, 9);
    ++table_index;
    from_string_test_func<Decimal64>(18, 9, table_index, test_data_index, 100);
    ++table_index;
    // from_string_test_func<Decimal64, 18, 9>(table_index, test_data_index, -11);
    // ++table_index;
    // from_string_test_func<Decimal64, 18, 9>(table_index, test_data_index, -9);
    // ++table_index;
    // from_string_test_func<Decimal64, 18, 9>(table_index, test_data_index, -100);
    // ++table_index;

    from_string_test_func<Decimal64>(18, 17, table_index, test_data_index);
    ++table_index;
    from_string_test_func<Decimal64>(18, 17, table_index, test_data_index, 0, true);
    ++table_index;
    from_string_test_func<Decimal64>(18, 17, table_index, test_data_index, 1);
    ++table_index;
    from_string_test_func<Decimal64>(18, 17, table_index, test_data_index, 17);
    ++table_index;
    from_string_test_func<Decimal64>(18, 17, table_index, test_data_index, 100);
    ++table_index;
    // from_string_test_func<Decimal64, 18, 17>(table_index, test_data_index, -1);
    // ++table_index;
    // from_string_test_func<Decimal64, 18, 17>(table_index, test_data_index, -100);
    // ++table_index;

    from_string_test_func<Decimal64>(18, 18, table_index, test_data_index);
    ++table_index;
}
TEST_F(FunctionCastToDecimalTest, test_to_decimal64_from_string) {
    from_string_to_decimal64_test_func();
}
} // namespace doris::vectorized