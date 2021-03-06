package com.p4th.wireless;

import com.p4th.wireless.ResultCB;

import java.util.TimeZone;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.provider.Settings;
import android.net.wifi.WifiManager;
import android.app.Activity;
import android.content.Context;
import android.net.wifi.ScanResult;
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

public class WiFis {

    public class WiFiReceiver extends BroadcastReceiver {

	public static final String TAG = WiFis.TAG + " WiFis";

        public WiFiReceiver() {
	    super();
        }

	@Override
	public void onReceive(Context c, Intent intent) 
	{
	    try {
		List<ScanResult> scanResults = WiFis.this.getWifiManager().getScanResults();
		Log.i(this.TAG, "Received scan results");
		//		Log.i(this.wifisInstance.TAG, new Integer(scanResults.size()).toString());
		//		Log.i(this.TAG, scanResults.toString());
		JSONArray res = this.normalizeAsJSON(scanResults);
		WiFis.this.resultCB.handleResult(new Result(this.getType(), res));
	    } catch (Exception e) {
		Log.e(this.TAG, e.getMessage());
	    }
	}
	
	public JSONArray normalizeAsJSON(List<ScanResult> scanResults) throws JSONException {
	    JSONArray networks = new JSONArray();
	    for (int i = 0 ; i < scanResults.size() ; i++) {
		networks.put(this.normalizeAsJSON(scanResults.get(i)));
	    }
	    return networks;
	}
	
	public float normalizeRSSI(int level) {
	    return (float)(level - WiFis.MIN_RSSI) / (float)Math.abs(WiFis.MAX_RSSI - WiFis.MIN_RSSI);
	}
	
	public JSONObject normalizeAsJSON(ScanResult scanResult) throws JSONException {
	    JSONObject res = new JSONObject();
	    res.put("id", scanResult.BSSID);
	    res.put("name", scanResult.SSID);
	    res.put("strength", this.normalizeRSSI(scanResult.level));
	    res.put("type", this.getType());
	    res.put("RSSI", scanResult.level);
	    res.put("SSID", scanResult.SSID);
	    res.put("BSSID", scanResult.BSSID);
	    res.put("timestamp", scanResult.timestamp);
	    res.put("summary", scanResult.toString());
	    return res;
	}

	protected String getType () {
	    return this.getClass().getEnclosingClass().getSimpleName();
	}
	
    }
    
    public static final int REQUEST_CODE = 666;
    public static final String TAG = Wireless.TAG + " WiFis";
    public final static int MIN_RSSI = -100;
    public final static int MAX_RSSI = 0;

    protected static IntentFilter scanIntentFilter = null;
    

    protected WiFiReceiver wifiReceiver = null;
    protected Context ctx = null;
    protected Activity activity = null;
    protected ResultCB resultCB = null;
    protected WifiManager wifiManager = null;

    /**
     * Constructor.
     */
    public WiFis(Activity activity, Context ctx, ResultCB resultCB) throws Exception {
	Log.i(this.TAG, "WiFis");
	this.activity = activity;
	this.ctx = ctx;
	this.resultCB = resultCB;

	if (WiFis.scanIntentFilter == null) {
	    WiFis.scanIntentFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
	}
    }
    
    public void onDestroy() {
	if (this.wifiReceiver != null) {
	    this.ctx.unregisterReceiver(this.wifiReceiver);
	}
    }


    public void startScan () throws Exception {
	Log.i(this.TAG, "statScanHelper");

	WifiManager wifiManager = this.getWifiManager();

	if (this.wifiReceiver == null) {
	    this.wifiReceiver = new WiFiReceiver();
	    this.ctx
		.registerReceiver(this.wifiReceiver, this.scanIntentFilter);
	}
	if (this.getWifiManager().isWifiEnabled() == false) {
	    Log.e(this.TAG, "Enabling wifi!");
	    this.getWifiManager().setWifiEnabled(true);
	}   
	
	this.getWifiManager().startScan();
    }
    
    public WifiManager getWifiManager() throws Exception {
	if (this.wifiManager == null) {
	    this.wifiManager = (WifiManager)this.ctx.getSystemService(Context.WIFI_SERVICE);
	}
	return this.wifiManager;
    }
    
    

}
