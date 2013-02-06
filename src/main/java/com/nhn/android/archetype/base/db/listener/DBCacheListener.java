package com.nhn.android.archetype.base.db.listener;

import java.util.List;

import com.nhn.android.archetype.base.object.BaseObj;

public interface DBCacheListener {
	void onSuccess(List<BaseObj> objList);
	void onError();
}
