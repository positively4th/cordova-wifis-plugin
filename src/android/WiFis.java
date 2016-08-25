package com.p4th.wifis;

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

public class WiFis extends CordovaPlugin {

    public class PendingScan {
	final public CallbackContext callbackContext;
	final public long pendingTime;

	public PendingScan(CallbackContext callbackContext) {
	    this.callbackContext = callbackContext;
	    this.pendingTime = System.nanoTime();
	}
    }
    
    public static class WiFiReceiver extends BroadcastReceiver {

	protected WiFis wifisInstance;
    
        public WiFiReceiver(WiFis wifisInstance) {
	    super();
            this.wifisInstance = wifisInstance;
        }

	@Override
	public void onReceive(Context c, Intent intent) 
	{
	    try {
		List<ScanResult> scanResults = this.wifisInstance.getWifiManager().getScanResults();
		Log.i(this.wifisInstance.TAG, "Received scan results");
		//		Log.i(this.wifisInstance.TAG, new Integer(scanResults.size()).toString());
		Log.i(this.wifisInstance.TAG, scanResults.toString());
		JSONObject res;
		PendingScan pendingScan;
		Log.i(this.wifisInstance.TAG, "pending scans size:");
		Log.i(this.wifisInstance.TAG, new Integer(this.wifisInstance.getPendingScans().size()).toString());
		while (this.wifisInstance.getPendingScans().size() > 0) {
		    pendingScan = this.wifisInstance.getPendingScans().remove(0);
		    res = this.wifisInstance.normalizeAsJSON(scanResults, pendingScan.pendingTime);
		    pendingScan.callbackContext.success(res);
		}
	    } catch (Exception e) {
		Log.e(this.wifisInstance.TAG, e.getMessage());
	    }
	    }
	}
    
    public static final int REQUEST_CODE = 666;
    public static final String TAG = "WiFis";
    public final static int MIN_RSSI = -100;
    public final static int MAX_RSSI = 0;

    
    protected WifiManager wifiManager = null;
    protected ArrayList<PendingScan> pendingScans = new ArrayList<PendingScan>();

    /**
     * Constructor.
     */
    public WiFis() {
    }

    public ArrayList<PendingScan> getPendingScans() {
	return this.pendingScans;
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

    public WifiManager getWifiManager() throws Exception {
	if (this.wifiManager == null) {
	    this.wifiManager = (WifiManager)this.cordova.getActivity().getSystemService(Context.WIFI_SERVICE);
	    
  
	    
	    this.cordova.getActivity().registerReceiver(new WiFiReceiver(this), new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
	}
	return this.wifiManager;
    }
    
    protected void startScan() throws Exception {
	Log.i(this.TAG, "startScan");
	if (this.getPendingScans().size() > 1) {
	    Log.i(WiFis.this.TAG, "startScan aborted");
	    return;
	}
	if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
	   this.cordova.getActivity().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
	    Log.i(this.TAG, "Requesting ACCESS_COARSE_LOCATION permision!");
   
	    
	    this.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, this.REQUEST_CODE);
	} else {
	    Log.i(this.TAG, "Permision ACCESS_COARSE_LOCATION already granted!");
	    if (this.getWifiManager().isWifiEnabled() == false) {
		Log.e(this.TAG, "Enabling wifi!");
		this.getWifiManager().setWifiEnabled(true);
	    }   
	    this.getWifiManager().startScan();
	}
    }
    
    protected void requestPermissions(String [] permissions, int requestCode) throws Exception {
	Log.i(this.TAG, "requestPermissions");
        try {
            java.lang.reflect.Method method = cordova.getClass().getMethod("requestPermissions",
									   org.apache.cordova.CordovaPlugin.class,
									   int.class, java.lang.String[].class);
            method.invoke(cordova, this, requestCode, permissions);
        } catch (NoSuchMethodException e) {
            throw new Exception("requestPermissions() method not found in CordovaInterface implementation of Cordova v" + CordovaWebView.CORDOVA_VERSION);
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions,
					   int[] grantResults) {
	Log.i(this.TAG, "onRequestPermissionsResult");
	
	for (int i = 0; i < grantResults.length ; i++) {
	    Log.i(this.TAG, permissions[i]);
	    if (requestCode == this.REQUEST_CODE && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
		try {
		    if (this.getWifiManager().isWifiEnabled() == false) {
			Log.e(this.TAG, "Enabling wifi!");
			this.getWifiManager().setWifiEnabled(true);
		    }   
		    this.getWifiManager().startScan();
		} catch (Exception e) {
		    Log.e(this.TAG, e.getMessage());
		}
	    } else {
		Log.e(this.TAG, permissions[i] + " not granted.");
	    }
	   
	}
    }    
    

    public JSONObject normalizeAsJSON(List<ScanResult> scanResults, long pendingTime) throws JSONException {
	JSONObject res = new JSONObject();
	JSONArray networks = new JSONArray();
	for (int i = 0 ; i < scanResults.size() ; i++) {
	    networks.put(this.normalizeAsJSON(scanResults.get(i)));
	}
	res.put("networks", networks);
	res.put("pendingTime", (System.nanoTime() - pendingTime) / 1000);
	return res;
    }

    public static float normalizeRSSI(int level) {
	return (float)(level - WiFis.MIN_RSSI) / (float)Math.abs(WiFis.MAX_RSSI - WiFis.MIN_RSSI);
    }

    public JSONObject normalizeAsJSON(ScanResult scanResult) throws JSONException {
	JSONObject res = new JSONObject();
	res.put("SSID", scanResult.SSID);
	res.put("RSSI", scanResult.level);
	res.put("timestamp", scanResult.timestamp);
	res.put("summary", scanResult.toString());
	res.put("strength", WiFis.normalizeRSSI(scanResult.level));
	return res;
    }

    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if ("scan".equals(action)) {

	    Log.i(this.TAG, "execute");
	    this.pendingScans.add(new PendingScan(callbackContext));
	    cordova.getThreadPool().execute(new Runnable() {
		    @Override
		    public void run()  {
			Log.i(WiFis.this.TAG, "run");
			try {
			    WiFis.this.startScan();
			}
			catch (Exception e) {
			    Log.e(WiFis.this.TAG, e.getMessage());
			    callbackContext.success("");
		    
			}
		    }
		});
        }
        else {
            return false;
        }
        return true;
    }


}
