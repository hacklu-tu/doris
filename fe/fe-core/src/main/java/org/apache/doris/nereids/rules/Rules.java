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

package org.apache.doris.nereids.rules;

import org.apache.doris.nereids.CascadesContext;
import org.apache.doris.nereids.trees.TreeNode;

import java.util.List;

/** Rules */
public abstract class Rules {
    protected List<Rule> rules;

    public Rules(List<Rule> rules) {
        this.rules = rules;
    }

    public abstract List<Rule> getCurrentAndChildrenRules(TreeNode<?> treeNode);

    public List<Rule> getCurrentAndChildrenRules() {
        return rules;
    }

    public abstract List<Rule> getCurrentRules(TreeNode<?> treeNode);

    public abstract List<Rule> getAllRules();

    public abstract List<Rule> filterValidRules(CascadesContext cascadesContext);
}
