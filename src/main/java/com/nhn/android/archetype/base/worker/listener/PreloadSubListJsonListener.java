package com.nhn.android.archetype.base.worker.listener;

public abstract class PreloadSubListJsonListener extends PreloadJsonListener {
	public abstract String getUrlPattern();
	public abstract String getSublistKey();
	public abstract String[] getKeyList();
}
