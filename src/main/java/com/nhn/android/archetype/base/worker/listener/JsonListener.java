package com.nhn.android.archetype.base.worker.listener;

import com.nhn.android.archetype.base.object.ApiResponse;
import com.nhn.android.archetype.base.object.BaseObj;

public interface JsonListener {
	void onSuccess(BaseObj response);
	void onError(int statusCode, ApiResponse result);
}
