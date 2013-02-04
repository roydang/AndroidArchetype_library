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

public abstract class AABaseApplication extends Application {
	private static M2baseLogger logger = M2baseLogger.getLogger(AABaseApplication.class);
	public static AABaseApplication _internalInstance; // M2base 내부에서만 사용할 인스턴스. 외부에선 접근하지 말것!

	public static interface BaseApplicationListener {
		void finishForce();
	}
	
	private static List<WeakReference<BaseApplicationListener>> activityList = new ArrayList<WeakReference<BaseApplicationListener>>();
	public static void addActivity(BaseApplicationListener activity) {
		activityList.add(new WeakReference<BaseApplicationListener>(activity));
	}
	
	public static void removeActivity(BaseApplicationListener activity) {
		for (int i = activityList.size() - 1; i >= 0; i--) {
			WeakReference<BaseApplicationListener> activityRef = activityList.get(i);
			BaseApplicationListener data = activityRef.get();
			
			if (data == null || data == activity) {
				activityList.remove(i);
			}
		}
	}
	
	public static void finishActivities() {
		if (activityList == null) {
			return;
		}
		
		for (WeakReference<BaseApplicationListener> activityRef : activityList) {
			if (activityRef != null) {
				if (activityRef.get() != null) {
					activityRef.get().finishForce();
				}
			}
		}
		
		if (activityList != null) {
			activityList.clear();
		}
	}

	/**
	 * 모든 프로세스 강제종료
	 */
	public static void applicationKill() {
		ActivityManager am = (ActivityManager) _internalInstance.getSystemService(Activity.ACTIVITY_SERVICE);
		am.killBackgroundProcesses(_internalInstance.getPackageName());
	}
	
	public static void applicationAllKill() {
		final ActivityManager am = (ActivityManager) _internalInstance.getSystemService(Activity.ACTIVITY_SERVICE);
		// stop running service inside current process.
		List<RunningServiceInfo> serviceList = am.getRunningServices(100);
		for (RunningServiceInfo service : serviceList) {
			if (service.pid == android.os.Process.myPid()) {
				Intent stop = new Intent();
				stop.setComponent(service.service);
				_internalInstance.stopService(stop);
			}
		}

		// move current task to background.
		Intent launchHome = new Intent(Intent.ACTION_MAIN);
		launchHome.addCategory(Intent.CATEGORY_DEFAULT);
		launchHome.addCategory(Intent.CATEGORY_HOME);
		_internalInstance.startActivity(launchHome);

		// post delay runnable(waiting for home application launching)
		new Handler().postDelayed(new Runnable() {
			public void run() {
				am.killBackgroundProcesses(_internalInstance.getPackageName());
			}
		}, 2000);
	}
	
	/**
	 * Worker ThreadPool
	 */
	private ExecutorService workExecutor;
	private ExecutorService statsWorkExecutor; // 통계용 worker. single thread
	
	private Handler handler;
	private Handler backgroundHandler;
	private HandlerThread backgroundHandlerThread;

	@Override
	public void onCreate() {
		logger.d("onCreate");
		_internalInstance = this;
		
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
		workExecutor = Executors.newCachedThreadPool();
		statsWorkExecutor = Executors.newFixedThreadPool(1);
		
		handler = new Handler(Looper.getMainLooper());
		
		backgroundHandlerThread = new HandlerThread("BandBackgroundHandlerThread");
		backgroundHandlerThread.start();
		
		backgroundHandler = new Handler(backgroundHandlerThread.getLooper());
		
		JsonWorker.init();
		
		logger.d("Application init completed.....");
	}

	private void closeHttpClient(HttpClient client) {
		ClientConnectionManager mgr = client.getConnectionManager();
		mgr.closeExpiredConnections();
		mgr.closeIdleConnections(0, TimeUnit.MILLISECONDS);
	}
	
	protected void close() {
		if (backgroundHandlerThread != null) {
			try {
				backgroundHandlerThread.quit();
			} catch (Exception e) {
			}
			
			backgroundHandlerThread = null;
			backgroundHandler = null;
		}
		
		if (workExecutor != null) {
			workExecutor.shutdown();	
		}
		
		if (statsWorkExecutor != null) {
			statsWorkExecutor.shutdown();	
		}
		
		closeHttpClient(ApacheJsonWorker.getHttpClient());
	}

	public Handler getHandler() {
		return handler;
	}
	
	public Handler getBackgroundHandler() {
		return backgroundHandler;
	}

	public void addWorker(Worker worker) {
		if (worker == null) {
			return;
		}
		workExecutor.execute((Runnable)worker);
	}

	public void addWorker(Runnable runnable) {
		if (runnable == null) {
			return;
		}
		workExecutor.execute(runnable);
	}

	public void addStatsWorker(Worker worker) {
		if (worker == null) {
			return;
		}
		
		statsWorkExecutor.execute((Runnable)worker);
	}

	public void addStatsWorker(Runnable runnable) {
		if (runnable == null) {
			return;
		}
		
		statsWorkExecutor.execute(runnable);
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
