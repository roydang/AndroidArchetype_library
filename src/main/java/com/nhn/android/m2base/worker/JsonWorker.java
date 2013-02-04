package com.nhn.android.m2base.worker;

import java.io.File;
import java.util.List;

import com.nhn.android.m2base.object.BaseObj;
import com.nhn.android.m2base.util.internal.M2baseUtility;
import com.nhn.android.m2base.worker.listener.JsonListener;

public class JsonWorker extends MultipartJsonWorker {
	private MultipartJsonWorker worker;
	
	public JsonWorker(String url) {
		initWorker(url, null);
	}

	public JsonWorker(String url, JsonListener jsonListener) {
		initWorker(url, jsonListener);
	}
	
	private void initWorker(String url, JsonListener jsonListener) {
		if (M2baseUtility.isICSCompatibility()) {
			worker = new AndroidHttpJsonWorker(url, jsonListener);
		} else {
			worker = new ApacheJsonWorker(url, jsonListener);
		}
	}
	
	@Override
	public void setNloginCookie(String nloingCookie) {
		super.setNloginCookie(nloingCookie);
		worker.setNloginCookie(nloingCookie);
	}

	@Override
	public void setSingleAttach(boolean singleAttach) {
		super.setSingleAttach(singleAttach);
		worker.setSingleAttach(singleAttach);
	}

	@Override
	public void setAttachment(File attachment) {
		super.setAttachment(attachment);
		worker.setAttachment(attachment);
	}

	@Override
	public void setAttachment(List<File> attchList) {
		super.setAttachment(attchList);
		worker.setAttachment(attchList);
	}

	@Override
	public void setAttachment(List<File> attachment, boolean isSingleAttach) {
		super.setAttachment(attachment, isSingleAttach);
		worker.setAttachment(attachment, isSingleAttach);
	}

	@Override
	public void setRetrycount(int retrycount) {
		super.setRetrycount(retrycount);
		worker.setRetrycount(retrycount);
	}

	@Override
	public void setSkipAuthrization(boolean skipAuthrization) {
		super.setSkipAuthrization(skipAuthrization);
		worker.setSkipAuthrization(skipAuthrization);
	}
	
	@Override
	public BaseObj postSync() {
		return worker.postSync();
	}

	@Override
	public void abort() {
		worker.abort();
	}

	@Override
	public long getSendigFileLength() {
		return worker.getSendigFileLength();
	}

	@Override
	public long getTotalFileLength() {
		return worker.getTotalFileLength();
	}

	@Override
	protected void doWork() {
		worker.doWork();
	}

	@Override
	public void setConnectionTimeout(int timeout) {
		worker.setConnectionTimeout(timeout);
	}

	@Override
	public int getConnectionTimeout() {
		return worker.getConnectionTimeout();
	}
}
