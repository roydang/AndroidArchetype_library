package com.nhn.android.archetype.base.worker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.nhn.android.archetype.base.AABaseApplication;
import com.nhn.android.archetype.base.object.BaseObj;

public abstract class MultipartJsonWorker extends Worker {
	public static final int RESULT_CODE_NETWORK_ERROR = 999;
	public static final int RESULT_CODE_UNEXPECTED_ERROR = -1;

	private static final int DEFAULT_RETRY_COUNT = 3;

	protected static AABaseApplication application;
	protected static String globalAppKey;
	protected static String globalAppSig;
	protected static boolean enableSessionCache = false;
	
	private int retrycount = DEFAULT_RETRY_COUNT;
	
	public static void init() {
		MultipartJsonWorker.application = AABaseApplication._internalInstance;
		MultipartJsonWorker.globalAppKey = application.getAppKey();
		MultipartJsonWorker.globalAppSig = application.getAppSig();
	}
	
	public static void setEnableSessionCache(boolean enableSessionCache) {
		JsonWorker.enableSessionCache = enableSessionCache;
	}
	
	private boolean isSingleAttach = false;
	private String nloingCookie = null;
	private boolean skipAuthrization = false;
	
	private List<File> attachment;

	public List<File> getAttachment() {
		return attachment;
	}

	public boolean isSingleAttach() {
		return isSingleAttach;
	}
	
	public void setSingleAttach(boolean singleAttach) {
		this.isSingleAttach = singleAttach;
	}
	
	public void setAttachment(File attachment) {
		List<File> attchList = new ArrayList<File>();
		attchList.add(attachment);
		
		setAttachment(attchList, true);
	}
	
	public void setAttachment(List<File> attchList) {
		setAttachment(attchList, false);
	}
	
	public void setAttachment(List<File> attachment, boolean isSingleAttach) {
		this.attachment = attachment;
		this.isSingleAttach = isSingleAttach;
	}

	public int getRetrycount() {
		return retrycount;
	}

	public void setRetrycount(int retrycount) {
		this.retrycount = retrycount;
	}
	
	public String getNloginCookie() {
		return nloingCookie;
	}
	
	public void setNloginCookie(String nloingCookie) {
		this.nloingCookie = nloingCookie;
	}
	
	public boolean isSkipAuthrization() {
		return skipAuthrization;
	}

	public void setSkipAuthrization(boolean skipAuthrization) {
		this.skipAuthrization = skipAuthrization;
	}
	
	public abstract BaseObj postSync();
	public abstract void abort();
	public abstract long getSendigFileLength();
	public abstract long getTotalFileLength();
	public abstract void setConnectionTimeout(int timeout);
	public abstract int getConnectionTimeout();
}
