package com.nhn.android.m2base.worker.listener;

import com.nhn.android.m2base.object.ApiResponse;
import com.nhn.android.m2base.object.BaseObj;

public interface JsonListener {
	void onSuccess(BaseObj response);
	void onError(int statusCode, ApiResponse result);
}
