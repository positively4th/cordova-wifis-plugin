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

/*
var argscheck = require('cordova/argscheck'),
    channel = require('cordova/channel'),
    utils = require('cordova/utils'),
    exec = require('cordova/exec'),
    cordova = require('cordova');
*/

var  exec = require('cordova/exec');


//channel.createSticky('onCordovaInfoReady');
// Tell cordova channel to wait on the CordovaInfoReady event
//channel.waitForInitialization('onCordovaInfoReady');

function WiFis() {
    this.scanResult = [];
}

WiFis.prototype.wifis = function() {
    return this.scanResult;
};

WiFis.prototype.scan = function() {


    return new Promise(function(resolve, reject) {
	var onSuccess = function (scanResult) {
	    this.scanResult = scanResult;
 	    resolve(this.scanResult);
	}.bind(this);
	
	var onError = function (err) {
	    console.log('Error: Wifis: scan: ' + err);
	    this.scanResult = [];
 	    reject(err);
	}.bind(this);
	
	//    argscheck.checkArgs('fF', 'Device.getInfo', arguments);
	exec(onSuccess, onError, "WiFis", "scan", []);
    });
};

module.exports = new WiFis();
