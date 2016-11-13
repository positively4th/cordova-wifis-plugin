package com.p4th.wireless;

import com.p4th.wireless.Permissioner;
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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

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

public class Bluetooths implements  Permissioner.PermissionHandler {

    public class BTReceiver extends BroadcastReceiver {

	public final static String TAG = Bluetooths.TAG + " BTReceiver";
	public final static int MIN_RSSI = -100;
	public final static int MAX_RSSI = 0;
	
	protected BluetoothAdapter btAdapter;
	protected ArrayList<Intent> intents = new ArrayList<Intent>(); 
        public BTReceiver(BluetoothAdapter btAdapter) {
	    super();
            this.btAdapter = btAdapter;
        }

	@Override
	public void onReceive(Context c, Intent intent) 
	{
	    Log.i(BTReceiver.TAG, "onReceive");
	    try {
		String action = intent.getAction();
		// When discovery finds a device
		if (BluetoothDevice.ACTION_FOUND.equals(action)) {
		    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		    this.intents.add(intent);
		    Log.i(BTReceiver.TAG, " " + device.getName() + "\n" + device.getAddress());
		    return;
		}
		if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
		    Log.i(BTReceiver.TAG, " found " + (new Integer(this.intents.size())).toString());
		    this.onScanFinished(intent);
		}
	    } catch (Exception e) {
		Log.e(BTReceiver.TAG, e.getMessage());
	    }
	}
	
	protected void onScanFinished(Intent intent) {
	    JSONArray res = this.normalizeAsJSON(this.intents);
	    Bluetooths.this.resultCB.handleResult(new Result(this.getType(), res));
       	}
	
	protected JSONArray normalizeAsJSON(List<Intent> intents)  {
	    JSONArray devices = new JSONArray();
	    for (int i = 0 ; i < intents.size() ; i++) {
		try {
		    devices.put(this.normalizeAsJSON(intents.get(i)));
		} catch (JSONException e) {
		    Log.e(BTReceiver.TAG, e.getMessage());
		}
	    }
	    return devices;
	}
	
	public JSONObject normalizeAsJSON(Intent intent) throws JSONException {
	    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
	    Integer rssi = new Integer(intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,Short.MIN_VALUE));
	    if (rssi.intValue() == Short.MIN_VALUE) {
		rssi = null;
	    }
	    String name = device.getName();
	    if (name == null || name.trim().equals("")) {
		name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
	    }
	    JSONObject res = new JSONObject();
	    res.put("id", device.getAddress());
	    res.put("name", device.getName());
	    res.put("type", this.getType());
	    res.put("strength", this.normalizeRSSI(rssi));
	    res.put("RSSI", rssi);
	    //	    res.put("timestamp", scanResult.timestamp);
	    res.put("summary", device.toString());
	    return res;
	}
	
	public float normalizeRSSI(int level) {
	    return (float)(level - BTReceiver.MIN_RSSI) / (float)Math.abs(BTReceiver.MAX_RSSI - BTReceiver.MIN_RSSI);
	}
	
	protected String getType () {
	    return this.getClass().getEnclosingClass().getSimpleName();
	}
	
    }

    
    public static final int REQUEST_CODE = 667;
    public static final String TAG = Wireless.TAG + " Bluetooths";

    protected static IntentFilter scanIntentFilter = null;
    protected BluetoothAdapter btAdapter = null;
    protected Activity activity = null;
    protected Context ctx = null;
    protected Permissioner permissioner = null;
    protected ResultCB resultCB = null;
    protected BroadcastReceiver scanReceiver = null;
    /**
     * Constructor.
     */
    public Bluetooths(Activity activity, Context ctx, ResultCB resultCB) throws Exception  {
	Log.i(this.TAG, "Bluetooths");
	this.activity = activity;
	this.ctx = ctx;
	this.resultCB = resultCB;
	this.permissioner = new Permissioner(this.ctx);

	if (Bluetooths.scanIntentFilter == null) {
	    Bluetooths.scanIntentFilter = new IntentFilter();
	    Bluetooths.scanIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
	    Bluetooths.scanIntentFilter.addAction(BluetoothDevice.ACTION_FOUND);
	}
	
	this.startScan();
    }

    public void onDestroy() {
	if (this.scanReceiver != null) {
	    this.ctx.unregisterReceiver(this.scanReceiver);
	}
    }

    public void startScan() throws Exception {
	Log.i(this.TAG, "startScan");
	this.permissioner.requestPermissions(new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN}, this);
    }

    public void handlePermissions(List<String> granted, List<String> denied) {
	Log.i(this.TAG, "handlePermissions");
	Log.i(this.TAG, " granted: " + (new Integer(granted.size())).toString());
	Log.i(this.TAG, " denied: " + (new Integer(denied.size())).toString());
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
	Log.i(this.TAG, "startScanHelper");
	if (this.getBTAdapter().isEnabled() == false) {
	    Log.e(this.TAG, "Enabling bluetooth!");
	    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	    this.activity.startActivityForResult(enableBtIntent, this.REQUEST_CODE);
	}   
	this.scanReceiver = new BTReceiver(this.getBTAdapter());
	this.ctx.registerReceiver(this.scanReceiver, Bluetooths.scanIntentFilter); // Don't forget to unregister during onDestroy
	if (!this.getBTAdapter().isDiscovering()) {
	    this.getBTAdapter().startDiscovery();
	}
    }
    
    public BluetoothAdapter getBTAdapter() throws Exception {
	if (this.btAdapter == null) {
	    this.btAdapter = BluetoothAdapter.getDefaultAdapter();
	}
	return this.btAdapter;
    }


   


}
