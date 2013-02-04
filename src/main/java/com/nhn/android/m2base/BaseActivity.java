/*
 * @(#)BaseActivity.java $$version ${date}
 *
 * Copyright 2007 NHN Corp. All rights Reserved.
 * NHN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.nhn.android.m2base;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.KeyEvent;

import com.nhn.android.m2base.BaseApplication.BaseApplicationListener;
import com.nhn.android.m2base.image.ImageHelper;
import com.nhn.android.m2base.image.ImageLoadManager;
import com.nhn.android.m2base.util.internal.M2baseLogger;
import com.nhn.android.m2base.worker.Worker;

/**
 * Activity의 life-cycle 단계마다 처리해줘야하는 로직을 처리한다.
 * @author telltale
 *
 */
public class BaseActivity extends Activity implements BaseApplicationListener {
	private static M2baseLogger logger = M2baseLogger.getLogger(BaseActivity.class);
	
	/**
	 * Activity life-cycle Observer
	 */
	public interface ActivityListener {
		public void onCreate(Bundle savedInstanceState);
		public void onDestroy();
		public void onStart();
		public void onStop();
		public void onPause();
		public void onRestart();
		public void onResume();
		public void onConfigurationChanged(Configuration newConfig);
	}

	private ActivityListener activityListener;
	
	/**
	 * onPause 시 등록된 모든 Worker를 취소한다.
	 */
	private List<Worker> workers;

	public ActivityListener getActivityListener() {
		return activityListener;
	}

	public void setActivityListener(ActivityListener activityListener) {
		this.activityListener = activityListener;
	}

	/**
	 * Worker를 등록해 놓은면 onPause 시 모두 취소된다.
	 */
	public void registerWorker(Worker worker) {
		if (worker == null) {
			return;
		}
		
		if (workers == null) {
			workers = new ArrayList<Worker>();
		}
		if (workers.contains(worker) == false) {
			workers.add(worker);
		}
	}

	public void unRegisterWorker(Worker worker) {
		if (workers == null) {
			return;
		}
		workers.remove(worker);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		logger.d("onCreate class(%s)", this.getClass().getName());
		//this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		
    	super.onCreate(savedInstanceState);
    	BaseApplication.addActivity(this);
    	
    	if (activityListener != null) {
			activityListener.onCreate(savedInstanceState);
		}
	}

	@Override
	protected void onDestroy() {
		logger.d("onDestroy class(%s)", this.getClass().getName());
		super.onDestroy();
		
		BaseApplication.removeActivity(this);
		
		if (activityListener != null) {
			activityListener.onDestroy();
		}
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
	}

	@Override
	public void finish() {
		if (this.isFinishing()) {
			return;
		}
		
		super.finish();
	}
	
	public final void finishForce() {
		super.finish();
	}

	@Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

	@Override
	protected void onPause() {
		logger.d("onPause class(%s)", this.getClass().getName());
		cancelAllWorker();
		ImageLoadManager.cancelRequest();
		
		super.onPause();
		
		if (activityListener != null) {
			activityListener.onPause();
		}
	}

	@Override
	protected void onRestart() {
		logger.d("onRestart class(%s)", this.getClass().getName());
		super.onRestart();
		if (activityListener != null) {
			activityListener.onRestart();
		}
	}

	@Override
	protected void onResume() {
		logger.d("onResume class(%s)", this.getClass().getName());
		super.onResume();
		
		if (activityListener != null) {
			activityListener.onResume();
		}	
	}
	
	@Override
	protected void onStart() {
		logger.d("onStart class(%s)", this.getClass().getName());
		super.onStart();
		if (activityListener != null) {
			activityListener.onStart();
		}
	}
	
	@Override
	protected void onStop() {
		logger.d("onStop class(%s)", this.getClass().getName());
		cancelAllWorker();
		
		super.onStop();
		if (activityListener != null) {
			activityListener.onStop();
		}
	}

	@Override
    public void onConfigurationChanged(Configuration newConfig) {
    	logger.d("onConfigurationChanged(%s)", newConfig);
    	super.onConfigurationChanged(newConfig);

    	if (activityListener != null) {
			activityListener.onConfigurationChanged(newConfig);
		}
    }

	@Override
	protected void finalize() throws Throwable {
    	logger.d("finalize class(%s)", this.getClass().getName());
		super.finalize();
	}

	@Override
	public void startActivity(Intent intent) {
		ImageHelper.cancelRequest();
		super.startActivity(intent);
	}

	@Override
	public void startActivityForResult(Intent intent, int requestCode) {
		ImageHelper.cancelRequest();
		super.startActivityForResult(intent, requestCode);
	}

	protected void cancelAllWorker() {
		logger.d("cancelAllWorker class(%s)", this.getClass().getName());
		if (this.workers == null) {
			return;
		}

		Iterator<Worker> workers = this.workers.iterator();	
		while (workers.hasNext()) {
			Worker worker = workers.next();
			worker.cancel();
		}
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)  {
		try {
			return super.onKeyUp(keyCode, event);
		} catch (Exception e) {
			logger.e(e);
		}

		return false;
	}
}
