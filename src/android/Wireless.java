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
		if (Wireless.this.wifis != null) {
		    Wireless.this.wifis.startScan();
		}
		if (Wireless.this.bluetooths != null) {
		    Wireless.this.bluetooths.startScan();
		}
	    } catch (Exception e) {
		Log.e(this.TAG, e.getMessage());
		Log.getStackTraceString(e);
	    }
	}
    }
    
    public static final String TAG = "Wireless";
    public static final int SCAN_REQUEST_CODE = 1;
    
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
	
	this.result = null;
	this.callCbCtx(jsonData, this.startCbCtx, true);
	//	if (this.startCbCtx != this.scanCbCtx) {
	this.callCbCtx(jsonData, this.scanCbCtx, false);
	//	    this.scanCbCtx = null;
	//	}
	this.scheduleNextScan();
	    
	    
    }

    protected JSONArray concatJSONArray(JSONArray arr1, JSONArray arr2) {
	try {
	    JSONArray result = new JSONArray();
	    for (int i = 0; i < arr1.length(); i++) {
		result.put(arr1.get(i));
	    }
	    for (int i = 0; i < arr2.length(); i++) {
		result.put(arr2.get(i));
	    }
	    return result;
	} catch (JSONException e) {
	    Log.e(Wireless.this.TAG, e.getMessage());
	    return new JSONArray();
	}
    }

    protected boolean callCbCtx(JSONArray jsonData, CallbackContext cbCtx, boolean keepCb) {
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
	    this.scheduleNextScan();
	}
    }
    
    protected void stopTimer () {
	if (this.isTimerStarted()) {
	    this.scanTimer.cancel();
	    this.scanTimer =  null;
	}
    }

    protected void scheduleNextScan () {
	if (!this.isTimerStarted()) {
	    return;
	}
	this.scanTimer.schedule(new ScanTask(), this.scanDelay);
    }
    
    protected void doScan()  {
	if (!this.permissions.hasPermissions(this.neededPermissions)) {
	    this.scanCbCtx.error("Missing required permissions.");
	    return;
	}
	try {
	    Wireless.this.wifis = new WiFis(Wireless.this.cordova.getActivity(), Wireless.this.cordova.getActivity().getApplicationContext(), Wireless.this);
	    Wireless.this.bluetooths = new Bluetooths(Wireless.this.cordova.getActivity(), Wireless.this.cordova.getActivity().getApplicationContext(), Wireless.this);
	}
	catch (JSONException e) {
	    Log.e(Wireless.this.TAG, e.getMessage());
	    this.scanCbCtx.error(e.getMessage());
	}
	catch (Exception e) {
	    Log.e(Wireless.this.TAG, e.getMessage());
	    Log.getStackTraceString(e);
	    this.scanCbCtx.error(e.getMessage());
	}
    }

    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext)  {
	if ("scan".equals(action)) {
	    Log.i(this.TAG, "scan");
	    if (this.scanCbCtx != null) {
		callbackContext.error(this.TAG + ": " + "Scan already started.");
		return false;
	    }
	    this.scanCbCtx = callbackContext;
	    this.ensurePermissions(Wireless.SCAN_REQUEST_CODE);
	    return true;
	}
	if ("start".equals(action)) {
	    Log.i(this.TAG, "start");
	    if (this.isTimerStarted()) {
		callbackContext.error(this.TAG + ": " + "Already started.");
		return false;
	    }
	    this.startCbCtx = callbackContext;
	    try {
		
		if (args.length() < 0) {
		    Log.e(this.TAG, "Interval missing.");
		    callbackContext.error(this.TAG + ": " + "Interval missing.");
		    return false;
		}
		
		this.scanDelay = args.getLong(0);
		if (this.scanDelay <= 0) {
		    Log.e(this.TAG, "Interval invalid.");
		    callbackContext.error(this.TAG + ": " + "Interval invalid.");
		    return false;
		}
		this.initTimer();
	    }
	    catch (JSONException e) {
		Log.e(Wireless.this.TAG, e.getMessage());
		callbackContext.error(e.getMessage());
		return false;
	    }
	    this.ensurePermissions(Wireless.SCAN_REQUEST_CODE);
	    return true;
	}
	if ("stop".equals(action)) {
	    Log.i(this.TAG, "stop");
	    if (!this.isTimerStarted()) {
		callbackContext.error(this.TAG + ": " + "Not started.");
		return false;
	    }
	    this.stopTimer();
	    this.startCbCtx = null;
	    callbackContext.success();
	    return true;
	}
	String error = "Unknown action: " + action; 
	Log.e(Wireless.this.TAG, error);
	callbackContext.error(this.TAG + ": " + error);
	return false;
    }


    public void ensurePermissions (int requestCode) {
	Log.i(this.TAG, "requestCode");
	this.cordova.requestPermissions(this, requestCode, this.neededPermissions);
	
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
