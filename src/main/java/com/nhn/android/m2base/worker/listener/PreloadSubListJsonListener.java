package com.nhn.android.m2base.worker.listener;

public interface PreloadSubListJsonListener extends PreloadJsonListener {
	String getUrlPattern();
	String getSublistKey();
	String[] getKeyList();
}
