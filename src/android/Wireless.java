package com.p4th.wireless;

import com.p4th.wireless.WiFis;
import com.p4th.wireless.Bluetooths;
import com.p4th.wireless.Result;
import com.p4th.wireless.ResultCB;
import com.p4th.wireless.Permissions;

import java.util.TimeZone;

import org.apache.cordova.PluginResult;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.provider.Settings;

import android.net.wifi.WifiManager;
import android.content.Context;
import android.net.wifi.ScanResult;
import java.util.Timer;
import java.util.TimerTask;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import android.util.Log;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Build.VERSION;
import android.Manifest;
import android.content.pm.PackageManager;

public class Wireless extends CordovaPlugin implements ResultCB {


    protected class ScanTask extends TimerTask {
	public final static String TAG = Wireless.TAG + " ScanTask";

	public void run() {
	    Log.i(Wireless.this.TAG, "ScanTask.run");
	    
	    try {
		JSONObject wrapper = new JSONObject();
		wrapper.put("type", Wireless.SCAN_STARTED_TYPE);
		Wireless.this.callCbCtx(wrapper, Wireless.this.startCbCtx, true);
		Wireless.this.ensurePermissionsAndScan();
		//		Wireless.this.getWiFis().startScan();
		//		Wireless.this.getBluetooths().startScan();
	    } catch (Exception e) {
		Log.e(this.TAG, e.getMessage());
		Log.getStackTraceString(e);
	    }
	}
    }
    
    public static final String TAG = "Wireless";
    public static final int SCAN_REQUEST_CODE = 1;
    public static final String SCAN_STARTED_TYPE = "scanStarted";
    public static final String SCAN_RESULT_TYPE = "result";
    public static final String MONITOR_STOP_TYPE = "monitorStopped";
    
    public static final Map<String , String> ERRORS = new HashMap<String , String>() {{
	    put("?", "Unknown error");
	    put("unknownAction", "Unknown action");
	    put("exception", "An unexpected error occured");
	    put("started", "Monitor is already running");
	    put("notStarted", "Monitor is not running");
	    put("scanStarted", "A scan is already started");
	    put("noInterval", "Interval is missing");
	    put("invalidInterval", "Interval is invalid");
	}};

    protected String []neededPermissions = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN};
    protected Permissions permissions = null;
    protected Result result = null; 
    protected WiFis wifis = null;
    protected Bluetooths bluetooths = null;
    protected CallbackContext startCbCtx = null;
    protected CallbackContext scanCbCtx = null;
    protected Timer scanTimer = null;
    protected String cordovaCBId = null;
    protected long scanDelay = 0;

    /**
     * Constructor.
     */
    public Wireless() {
	this.permissions = new Permissions();
    }

    /**
     * Sets the context of the Command. This can then be used to do things like
     * get file paths associated with the Activity.
     *
     * @param cordova The context of the main Activity.
     * @param webView The CordovaWebView Cordova is running in.
     */
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
    }

    public void handleResult(Result res) {
	Log.i(this.TAG, "handleResult");
	Log.i(this.TAG, res.type);
	//	Log.i(this.TAG, res.jsonData.toString());

	try {
	    String type = res.type;
	    JSONArray jsonData = res.jsonData;
	    
	    if (this.result == null || this.result.type.equals(res.type)) {
		Log.i(this.TAG, res.type + " aready reported");
		this.result = res;
		return;
	    }
	    
	    Log.i(this.TAG, res.type + " not reported");
	    jsonData = this.concatJSONArray(this.result.jsonData, res.jsonData);
	    //	Log.i(this.TAG, "reporting: " + jsonData.toString());
	    
	    JSONObject wrapper = this.wrapSuccessData(Wireless.SCAN_RESULT_TYPE, jsonData);
	    this.result = null;
	    if (this.scanCbCtx == null) {
		this.callCbCtx(wrapper, this.startCbCtx, true);
		this.scheduleNextScan();
	    } else {
		this.callCbCtx(wrapper, this.scanCbCtx, false);
		this.scanCbCtx = null;
	    }
	} catch (JSONException e) {
	    Log.e(Wireless.this.TAG, e.getMessage());
	}
    }

    protected JSONObject wrapSuccessData (String type, JSONArray data) throws JSONException {
	JSONObject wrapper = new JSONObject();
	wrapper.put("data", data);
	wrapper.put("type", type);
	return wrapper;
    }

    protected JSONObject wrapSuccessData (String type) throws JSONException {
	JSONObject wrapper = new JSONObject();
	wrapper.put("type", type);
	return wrapper;
    }

    protected JSONObject wrapErrorData (String error) {
	return this.wrapErrorData(error, "");
    }
    
    protected JSONObject wrapErrorData (String error, String info) {
	JSONObject wrapper = new JSONObject();

	if (!this.ERRORS.containsKey(error)) {
	    error = "?";
	}
	try {
	    wrapper.put("type", error);
	    wrapper.put("description", this.ERRORS.get(error));
	    if (!info.equals("")) {
		wrapper.put("info", info);
	    }
	} catch (JSONException e) {
	    Log.e(this.TAG, e.getMessage());
	}
	return wrapper;
    }

    protected JSONArray concatJSONArray(JSONArray arr1, JSONArray arr2) throws JSONException {
	JSONArray result = new JSONArray();
	for (int i = 0; i < arr1.length(); i++) {
	    result.put(arr1.get(i));
	}
	for (int i = 0; i < arr2.length(); i++) {
	    result.put(arr2.get(i));
	}
	return result;
    }

    protected boolean callCbCtx(JSONObject jsonData, CallbackContext cbCtx, boolean keepCb) {
	if (cbCtx == null) {
	    return false;
	}
	PluginResult result = new PluginResult(PluginResult.Status.OK, jsonData); 
	result.setKeepCallback(keepCb); 
	cbCtx.sendPluginResult(result);
	return true;
    }

    protected boolean isTimerStarted() {
	return this.scanTimer != null;
    }

    protected void initTimer () {
	this.stopTimer();
	if (this.scanDelay >= 0) {
	    this.scanTimer = new Timer();
	    ScanTask task = new ScanTask();
	    task.run();

	}
    }
    
    protected void stopTimer () {
	if (this.isTimerStarted()) {
	    this.scanTimer.cancel();
	    this.scanTimer =  null;
	}
    }

    protected ScanTask scheduleNextScan () {
	if (!this.isTimerStarted()) {
	    return null;
	}
	ScanTask task = new ScanTask();
	this.scanTimer.schedule(task, this.scanDelay);
	return task;
    }

    protected WiFis getWiFis () throws Exception {
	if (this.wifis == null) {
	    this.wifis = new WiFis(Wireless.this.cordova.getActivity(), Wireless.this.cordova.getActivity().getApplicationContext(), Wireless.this);
	}
	return this.wifis;
    }
    
    protected Bluetooths getBluetooths () throws Exception {
	if (this.bluetooths == null) {
	    this.bluetooths = new Bluetooths(Wireless.this.cordova.getActivity(), Wireless.this.cordova.getActivity().getApplicationContext(), Wireless.this);
	}
	return this.bluetooths;
    }
    
    protected void doScan()  {
	if (!this.permissions.hasPermissions(this.neededPermissions)) {
	    Log.e(Wireless.this.TAG, "Needed permissions not granted.");
	    return;
	}
	try {
	    this.getWiFis().startScan();
	    this.getBluetooths().startScan();
	}
	catch (JSONException e) {
	    Log.e(Wireless.this.TAG, e.getMessage());
	    this.scanCbCtx.error(this.wrapErrorData("exception",  e.getMessage()));
	}
	catch (Exception e) {
	    Log.e(Wireless.this.TAG, e.getMessage());
	    Log.getStackTraceString(e);
	    this.scanCbCtx.error(this.wrapErrorData("exception", e.getMessage()));
	}
    }

    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext)  {

	if ("scan".equals(action)) {
	    Log.i(this.TAG, "scan");
	    try {
		if (this.scanCbCtx != null) {
		    callbackContext.error(this.wrapErrorData("scanStarted"));
		    return false;
		}
		
		Wireless.this.callCbCtx(this.wrapSuccessData(Wireless.this.SCAN_STARTED_TYPE), callbackContext, true);
		this.scanCbCtx = callbackContext;
		this.ensurePermissionsAndScan();
	    }  catch (JSONException e) {
		Log.e(Wireless.this.TAG, e.getMessage());
		callbackContext.error(this.wrapErrorData("exception", e.getMessage()));
		return false;
	    }
	    return true;
	}
	
	if ("start".equals(action)) {
	    Log.i(this.TAG, "start");
	    try {
		
		if (this.isTimerStarted()) {
		    callbackContext.error(this.wrapErrorData("started"));
		    return false;
		}
		
		if (args.length() < 0) {
		    Log.e(this.TAG, "Interval missing.");
		    callbackContext.error(this.wrapErrorData("noInterval"));
		    return false;
		}
		
		this.startCbCtx = callbackContext;
		this.scanDelay = args.getLong(0);
		if (this.scanDelay <= 0) {
		    Log.e(this.TAG, "Interval invalid.");
		    callbackContext.error(this.wrapErrorData("invalidInterval"));
		    return false;
		}
		this.initTimer();
	    }
	    catch (JSONException e) {
		Log.e(Wireless.this.TAG, e.getMessage());
		callbackContext.error(this.wrapErrorData("exception", e.getMessage()));
		return false;
	    }
	    return true;
	}
	
	if ("stop".equals(action)) {
	    Log.i(this.TAG, "stop");
	    try {
		if (!this.isTimerStarted()) {
		    callbackContext.error(this.wrapErrorData("notStarted"));
		    return false;
		}
		this.stopTimer();
		this.startCbCtx = null;
		callbackContext.success(this.wrapSuccessData(Wireless.this.MONITOR_STOP_TYPE));
	    }  catch (JSONException e) {
		callbackContext.error(this.wrapErrorData("exception", e.getMessage()));
		return false;
	    }
	    return true;
	}
	
	
	Log.e(Wireless.this.TAG, this.ERRORS.get("unknownAction"));
	callbackContext.error(this.wrapErrorData("exception", action));
	return false;
    }


    public void ensurePermissionsAndScan () {
	Log.i(this.TAG, "ensurePermissionsAndScan");
	this.cordova.requestPermissions(this, Wireless.SCAN_REQUEST_CODE, this.neededPermissions);
	
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions,
					  int[] grantResults) throws JSONException
    {
	Log.i(this.TAG, "onRequestPermissionResult");
	Log.i(this.TAG, permissions.toString());
	Log.i(this.TAG, grantResults.toString());
	
	if (requestCode != Wireless.SCAN_REQUEST_CODE) {
	    return;
	}
	
	this.permissions.addPermissions(permissions, grantResults);
	this.doScan();
    }
}
