package com.p4th.wireless;

import com.p4th.wireless.WiFis;
import com.p4th.wireless.Bluetooths;
import com.p4th.wireless.CallbackJSON;

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

public class Wireless extends CordovaPlugin implements CallbackJSON {

    protected class ScanTask extends TimerTask {
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
		
	    }
	}
    }
    
    public static final String TAG = "Wireless";
    protected WiFis wifis = null;
    protected Bluetooths bluetooths = null;
    protected CallbackContext callbackContext;
    protected Timer scanTimer = null;
    protected String cordovaCBId = null;
    /**
     * Constructor.
     */
    public Wireless() {
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

    public void cbJSON(JSONObject jsonData) {
	Log.i(this.TAG, "cbJSON");
	Log.i(this.TAG, jsonData.toString());
	//	this.callbackContext.success(jsonData);

	PluginResult result = new PluginResult(PluginResult.Status.OK, jsonData); 
	result.setKeepCallback(true); 
	this.callbackContext.sendPluginResult(result);
    }

    protected void startTimer (long period) {
	if (this.scanTimer != null) {
	    this.scanTimer.cancel();
	    this.scanTimer =  null;
	}
	if (period > 0) {
	    this.scanTimer = new Timer();
	    this.scanTimer.schedule(new ScanTask(), period, period);
	}
    }

    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
	if ("start".equals(action)) {
	    

	    this.callbackContext = callbackContext;

	    long period = 0;
	    if (args.length() > 0) {
		period = args.getLong(0);
		Log.i(this.TAG, "periodSupplied");
		Log.i(this.TAG, (new Integer((int)period).toString()));
	    }
	    this.startTimer(period);
	    
	    
	    Log.i(this.TAG, "execute");
	    try {
		Wireless.this.wifis = new WiFis(Wireless.this.cordova.getActivity().getApplicationContext(), Wireless.this);
		Wireless.this.bluetooths = new Bluetooths(Wireless.this.cordova.getActivity(), Wireless.this.cordova.getActivity().getApplicationContext(), Wireless.this);
	    }
	    catch (Exception e) {
		Log.e(Wireless.this.TAG, e.getMessage());
		callbackContext.success("");
	    }
	} else {
	    return false;
	}
	this.cordovaCBId = callbackContext.getCallbackId();
	PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT); 
	pluginResult.setKeepCallback(true); 
	callbackContext.sendPluginResult(pluginResult);
	return true;
    }
    
}
    
