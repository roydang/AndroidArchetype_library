package com.nhn.android.archetype.base.worker.listener;

public interface ApiRequestListener<T, E> {
	void onSuccess(T result);
	void onError(E result);
}
