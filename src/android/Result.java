package com.p4th.wireless;

import org.json.JSONArray;

public class Result {
    public String type = null;
    public JSONArray jsonData = null;
    
    public Result (String type, JSONArray jsonData) {
	this.type = type;
	this.jsonData = jsonData; 
    }
}
