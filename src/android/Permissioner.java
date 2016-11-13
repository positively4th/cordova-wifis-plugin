package com.p4th.wireless;

import android.provider.Settings;

import android.content.Context;
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

public class Permissioner  {

    public interface PermissionHandler {
	public void handlePermissions(List<String> granted, List<String> denied);
    }
    
    protected class Request {
	protected static final String TAG = "Wifis: Permissioner.Request";
	protected List<String> granted = new ArrayList<String>();
	protected List<String> denied = new ArrayList<String>();
	protected PermissionHandler handler = null;
	protected final int requestCode = Permissioner.nextRequestCode++;
	
	public Request(List<String> granted, List<String> requested, PermissionHandler handler) throws Exception {
	    this.granted.addAll(granted);
	    this.handler = handler;
	    if (requested.size() < 1) {
		this.reportResult();
	    } else {
		this.requestPermissions((String [])requested.toArray());
	    }
	    
	}

	public void add(String permission, boolean granted) {
	    if (granted) {
		this.granted.add(permission);
	    } else {
		this.denied.add(permission);
	    }
	}

	protected void requestPermissions(String [] permissions) throws Exception {
	    Log.i(this.TAG, "requestPermissions");
	    try {
		java.lang.reflect.Method method = Permissioner.this.ctx
		    .getClass()
		    .getMethod("requestPermissions",
			       org.apache.cordova.CordovaPlugin.class,
			       int.class, java.lang.String[].class);
		method.invoke(Permissioner.this.ctx, this, this.requestCode, permissions);
	    } catch (NoSuchMethodException e) {
		throw new Exception("requestPermissions() method not found in context.");
	    }
	}

	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
	    if (this.requestCode != requestCode) {
		return;
	    }
	    for (int i = 0; i < grantResults.length ; i++) {
		Log.i(this.TAG, permissions[i]);
		this.add(permissions[i], grantResults[i] == PackageManager.PERMISSION_GRANTED);
	    }
	    this.reportResult();
	}
	
	public void reportResult() {
	    this.handler.handlePermissions(this.granted, this.denied);
	}
	
    }
    
    public static int nextRequestCode = 1;
    protected Context ctx = null;
    protected List<String> grantedPermissions = null;
    protected List<String> deniedPermissions = null;

    public Permissioner(Context ctx) {
	this.ctx = ctx;
    }

    protected boolean isPermissionGranted(String p) {
	return
	    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
	    &&
	    this.ctx.checkSelfPermission(p)
	    !=
	    PackageManager.PERMISSION_GRANTED; 
    }

    
    public void requestPermissions(String []permissions, PermissionHandler handler) throws Exception {
	List<String> granted = new ArrayList<String>();
	List<String> needed = new ArrayList<String>();
	for (int i = 0 ; i < permissions.length ; i++) {
	    if (this.isPermissionGranted(permissions[i])) {
		needed.add(permissions[i]);
	    } else {
		granted.add(permissions[i]);
	    }
	}
	
	Request req = new Request(granted, needed, handler); 
    }
    

    


}
