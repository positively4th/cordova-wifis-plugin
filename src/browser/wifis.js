/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
var browser = require('cordova/platform');


var wifis = ['-1,-1,-1', '0,0,0', '1,1,1'];

var delay = 500;

function getRandomWifi() {
    var index = Math.floor(Math.random() * wifis.length);
    return wifis[index];

}

function getNrm(p, q) {
    var res = p.reduce(function (res, item, i) {
	return res + (p[i] - q[i]) * (p[i] - q[i]); 
    }, 0);
    return Math.sqrt(res);
}

function scan(pendingTime) {
    var i;
    var q;
    var res = {
	networks: [],
	pendingTime: pendingTime
    };

    var p = [2 * Math.random() - 1, 2 * Math.random() - 1, 2 * Math.random() - 1];
    for (i = 0 ; i < wifis.length ; i++) {
	q = wifis[i].split(',');
	res.networks.push({
	    SSID: wifis[i],
	    RSSI: - 100 * getNrm(p,q) / Math.sqrt(4+4+4),
	    timestamp: Date.now(),
	});
	res.networks.summary = p.join(',');
    }
    return res;
}

module.exports = {
    scan: function (success/*, error*/) {
        setTimeout(function () {
            success(scan(delay * 1000));
        }, delay);
    }
};

require("cordova/exec/proxy").add("WiFis", module.exports);
