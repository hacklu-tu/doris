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

package org.apache.doris.system;

import org.apache.doris.common.io.Writable;

import com.google.gson.annotations.SerializedName;

/*
 * Broker heartbeat response currently contains nothing and the heartbeat status
 */
public class BrokerHbResponse extends HeartbeatResponse implements Writable {

    @SerializedName(value = "name")
    private String name;
    @SerializedName(value = "host")
    private String host;
    @SerializedName(value = "port")
    private int port;

    public BrokerHbResponse() {
        super(HeartbeatResponse.Type.BROKER);
    }

    public BrokerHbResponse(String name, String host, int port, long hbTime) {
        super(HeartbeatResponse.Type.BROKER);
        this.status = HbStatus.OK;
        this.name = name;
        this.host = host;
        this.port = port;
        this.hbTime = hbTime;
    }

    public BrokerHbResponse(String name, String host, int port, String errMsg) {
        super(HeartbeatResponse.Type.BROKER);
        this.status = HbStatus.BAD;
        this.name = name;
        this.host = host;
        this.port = port;
        this.msg = errMsg;
    }

    public String getName() {
        return name;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append(", name: ").append(name);
        sb.append(", host: ").append(host);
        sb.append(", port: ").append(port);
        return sb.toString();
    }

}
