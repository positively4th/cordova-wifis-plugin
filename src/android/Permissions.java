package com.p4th.wireless;

import android.provider.Settings;

import android.content.Context;
import java.util.List;
import java.util.ArrayList;
import android.util.Log;
import android.content.pm.PackageManager;

public class Permissions  {

    public final static String TAG = Wireless.TAG + " Permissions";
    
    protected List<String> granted = new ArrayList<String>();
    
    public Permissions() {}
    
    public void addPermissions(String[] permissions, int[] grantResults) {
	for (int i = 0; i < grantResults.length ; i++) {
	    Log.i(this.TAG, permissions[i]);
	    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
		this.addPermission(permissions[i]);
	    }
	}
    }
    
    public void addPermission(String permission) {
	if (!this.hasPermission(permission)) {
	    this.granted.add(permission);
	}
    }
    
    
    public boolean hasPermissions(String []ps) {
	for (int i = 0 ; i < ps.length ; i++) {
	    if (!this.hasPermission(ps[i])) {
		return false;
	    }
	}
	return true;
    }

    public boolean hasPermission(String p) {
	for (int i = 0 ; i < this.granted.size() ; i++) {
	    if (p.equals(this.granted.get(i))) {
		return true;
	    }
	}
	return false;
    }


}


