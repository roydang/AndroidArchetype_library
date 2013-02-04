package com.nhn.android.m2base.db.listener;

import java.util.List;

import com.nhn.android.m2base.object.BaseObj;

public interface DBCacheListener {
	void onSuccess(List<BaseObj> objList);
	void onError();
}
