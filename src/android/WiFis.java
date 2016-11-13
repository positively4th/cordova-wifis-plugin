package com.p4th.wireless;

import com.p4th.wireless.Permissioner;
import com.p4th.wireless.CallbackJSON;

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

public class WiFis implements Permissioner.PermissionHandler {

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
		Log.i(this.TAG, scanResults.toString());
		JSONObject res;
		res = this.normalizeAsJSON(scanResults);
		WiFis.this.cbJSON.cbJSON(res);
	    } catch (Exception e) {
		Log.e(this.TAG, e.getMessage());
	    }
	}
	
	public JSONObject normalizeAsJSON(List<ScanResult> scanResults) throws JSONException {
	    JSONObject res = new JSONObject();
	    JSONArray networks = new JSONArray();
	    for (int i = 0 ; i < scanResults.size() ; i++) {
		networks.put(this.normalizeAsJSON(scanResults.get(i)));
	    }
	    res.put("networks", networks);
	    return res;
	}
	
	public float normalizeRSSI(int level) {
	    return (float)(level - WiFis.MIN_RSSI) / (float)Math.abs(WiFis.MAX_RSSI - WiFis.MIN_RSSI);
	}
	
	public JSONObject normalizeAsJSON(ScanResult scanResult) throws JSONException {
	    JSONObject res = new JSONObject();
	    res.put("id", scanResult.BSSID);
	    res.put("name", scanResult.SSID);
	    res.put("strength", this.normalizeRSSI(scanResult.level));
	    res.put("RSSI", scanResult.level);
	    res.put("SSID", scanResult.SSID);
	    res.put("BSSID", scanResult.BSSID);
	    res.put("timestamp", scanResult.timestamp);
	    res.put("summary", scanResult.toString());
	    return res;
	}
	
    }
    
    public static final int REQUEST_CODE = 666;
    public static final String TAG = Wireless.TAG + " WiFis";
    public final static int MIN_RSSI = -100;
    public final static int MAX_RSSI = 0;

    protected static IntentFilter scanIntentFilter = null;
    

    protected WiFiReceiver wifiReceiver = null;
    protected Context ctx = null;
    protected Permissioner permissioner = null;
    protected CallbackJSON cbJSON = null;
    protected WifiManager wifiManager = null;

    /**
     * Constructor.
     */
    public WiFis(Context ctx, CallbackJSON cbJSON) throws Exception {
	this.ctx = ctx;
	this.cbJSON = cbJSON;
	this.permissioner = new Permissioner(this.ctx);

	if (WiFis.scanIntentFilter == null) {
	    WiFis.scanIntentFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
	}
	this.startScan();
    }
    
    public void onDestroy() {
	if (this.wifiReceiver != null) {
	    this.ctx.unregisterReceiver(this.wifiReceiver);
	}
    }


    public void startScan() throws Exception {
	Log.i(this.TAG, "startScan");
	this.permissioner.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, this);
    }

    public void handlePermissions(List<String> granted, List<String> denied) {
	Log.i(this.TAG, "handelPermissions");
	if (denied.size() > 0) {
	    return;
	}
	try {
	    this.startScanHelper();
	} catch (Exception e) {
	    Log.i(this.TAG, e.getMessage());
	}
    }
    
    protected void startScanHelper () throws Exception {
	Log.i(this.TAG, "statScanHelper");

	WifiManager wifiManager = this.getWifiManager();

	this.wifiReceiver = new WiFiReceiver();
	this.ctx
	    .registerReceiver(this.wifiReceiver, this.scanIntentFilter);
	
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
