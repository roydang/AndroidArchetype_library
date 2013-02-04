package com.nhn.android.m2base.cache;

import java.util.Date;

import com.nhn.android.m2base.object.BaseObj;

public class FileCache {
	private BaseObj model;
	private String json;
	private Date cachedDate;

	public FileCache(String json, Date cachedDate) {
		this.json = json;
		this.cachedDate = cachedDate;
	}
	
	public BaseObj getModel() {
		if (model == null) {
			model = BaseObj.parse(getJson());
		}
		
		return model;
	}

	public String getJson() {
		return json.trim();
	}

	public Date getCachedDate() {
		return cachedDate;
	}
}
