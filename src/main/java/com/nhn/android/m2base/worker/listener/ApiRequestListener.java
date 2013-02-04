package com.nhn.android.m2base.worker.listener;

public interface ApiRequestListener<T, E> {
	void onSuccess(T result);
	void onError(E result);
}
