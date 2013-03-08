/*
 * @(#)BaseApplication.java $$version ${date}
 *
 * Copyright 2007 NHN Corp. All rights Reserved.
 * NHN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.nhn.android.archetype.base;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Application;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import com.nhn.android.archetype.base.image.ImageCacheManager;
import com.nhn.android.archetype.base.theme.ThemeHelper;
import com.nhn.android.archetype.base.util.internal.M2baseLogger;
import com.nhn.android.archetype.base.util.internal.M2baseUtility;
import com.nhn.android.archetype.base.worker.ApacheJsonWorker;
import com.nhn.android.archetype.base.worker.JsonWorker;
import com.nhn.android.archetype.base.worker.Worker;

public abstract class BaseApplication extends Application {
	private static M2baseLogger logger = M2baseLogger.getLogger(BaseApplication.class);
	public static BaseApplication _internalInstance; // M2base 내부에서만 사용할 인스턴스. 외부에선 접근하지 말것!


	@Override
	public void onCreate() {
		logger.d("onCreate");
		_internalInstance = this;
		
		// http://stackoverflow.com/questions/4443278/toast-sending-message-to-a-handler-on-a-dead-thread
		try {
			Class.forName("android.os.AsyncTask");
		} catch (ClassNotFoundException e) {
		}

		if (M2baseUtility.isUnderFroyo()) {
			System.setProperty("http.keepAlive", "false");
		}
		
		super.onCreate();
		
		init();
	}

	@Override
	public void onLowMemory() {
		logger.d("onLowMemory");
		ImageCacheManager.clearMemoryCache();
		super.onLowMemory();
	}

	protected void init() {		

		
		JsonWorker.init();
		
		logger.d("Application init completed.....");
	}

	private void closeHttpClient(HttpClient client) {
		ClientConnectionManager mgr = client.getConnectionManager();
		mgr.closeExpiredConnections();
		mgr.closeIdleConnections(0, TimeUnit.MILLISECONDS);
	}
	
	protected void close() {
		
		closeHttpClient(ApacheJsonWorker.getHttpClient());
	}



	@Override
	public void onTerminate() {
		logger.d("onTerminate");
		
		close();
		super.onTerminate();
	}
	
	public void selectThemePackageName(String name) {
		ThemeHelper.updateTheme();
	}
	
	public String getSelectedThemePackageName() {
		return null;
	}

	public static File getExternalStorageDirectory() {
		if (Build.VERSION.SDK_INT == 17) {
			// android 4.2 sdcard 버그
			return new File("/sdcard");
		}
		
		return Environment.getExternalStorageDirectory();
	}
	
	public abstract File getExternalCacheFolder();
	public abstract File getExternalImagesFolder();
	
	public abstract String getAppKey();
	public abstract String getAppSig();
	public abstract String getUserAgent();
	public abstract String getUserId();
	public abstract String getFullAuthToken();
}
