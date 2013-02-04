/*
 * @(#)BaseActivity.java $$version ${date}
 *
 * Copyright 2007 NHN Corp. All rights Reserved.
 * NHN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.nhn.android.archetype.base;

import roboguice.activity.RoboActivity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

import com.nhn.android.archetype.base.AABaseApplication.BaseApplicationListener;
import com.nhn.android.archetype.base.image.ImageHelper;
import com.nhn.android.archetype.base.image.ImageLoadManager;
import com.nhn.android.archetype.base.util.internal.M2baseLogger;

/**
 * Activity의 life-cycle 단계마다 처리해줘야하는 로직을 처리한다.
 * @author telltale
 *
 */
public class AABaseActivity extends RoboActivity implements BaseApplicationListener {
	private static M2baseLogger logger = M2baseLogger.getLogger(AABaseActivity.class);
	


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		logger.d("onCreate class(%s)", this.getClass().getName());
    	super.onCreate(savedInstanceState);
    	AABaseApplication.addActivity(this);

	}

	@Override
	protected void onDestroy() {
		logger.d("onDestroy class(%s)", this.getClass().getName());
		super.onDestroy();		
		AABaseApplication.removeActivity(this);
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
		ImageLoadManager.cancelRequest();
		
		super.onPause();	
	}

	@Override
	protected void onRestart() {
		logger.d("onRestart class(%s)", this.getClass().getName());
		super.onRestart();
	
	}

	@Override
	protected void onResume() {
		logger.d("onResume class(%s)", this.getClass().getName());
		super.onResume();
	
	}
	
	@Override
	protected void onStart() {
		logger.d("onStart class(%s)", this.getClass().getName());
		super.onStart();
		
	}
	
	@Override
	protected void onStop() {
		logger.d("onStop class(%s)", this.getClass().getName());
		super.onStop();
	}

	@Override
    public void onConfigurationChanged(Configuration newConfig) {
    	logger.d("onConfigurationChanged(%s)", newConfig);
    	super.onConfigurationChanged(newConfig);

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


}
