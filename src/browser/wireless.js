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
var types = {
    "-1,-1,-1": 'WiFis',
    "0,0,0": "WiFis",
    "1,1,1": "Bluetooths"
};

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

function normalizeRSSI(level) {
    var MIN_RSSI = -100;
    var MAX_RSSI = 0;
    return (level - MIN_RSSI) / Math.abs(MAX_RSSI - MIN_RSSI);
}

function scan(pendingTime) {
    var i;
    var q;
    var networks = [];

    var p = [2 * Math.random() - 1, 2 * Math.random() - 1, 2 * Math.random() - 1];
    for (i = 0 ; i < wifis.length ; i++) {
	if (Math.random() > 0.25) {
	    q = wifis[i].split(',');
	    var rssi = - 100 * getNrm(p,q) / Math.sqrt(4+4+4);
	    networks.push({
		id: wifis[i],
		name: 'Name of ' + wifis[i], 
		type: types[wifis[i]],
		SSID: wifis[i],
		RSSI: rssi,
		timestamp: Date.now(),
		strength: normalizeRSSI(rssi)
	    });
	    networks.summary = p.join(',');
	}
    }
    return networks;
}

var interval = null;
var timeout = null;
var scanTime = 0.5;
module.exports = {

    scan: function (success, error) {
	var delay = 500;
	if (timeout) {
	    error('Scan already started');
	    return false;
	}
        setTimeout(function () {
            success(scan(delay));
	    timeout = null;
        }, delay);
	return true;
    },

    start: function (success, error, delay) {
	if (interval) {
	    error('Already started');
	    return false;
	}
        success(scan(delay), { keepCallback: true });
        interval = setInterval(function () {
	    console.log('start scan started');


	    success(scan(delay), { keepCallback: true });
        }, delay);
	return true;
    },
    
    stop: function (success, error) {
	if (!interval) {
	    error('Not started');
	    return false;
	}
	clearInterval(interval);
	interval =  null;
        success();
	return true;
    }
};

require("cordova/exec/proxy").add("Wireless", module.exports);
