package com.nhn.android.archetype.base.cache;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.os.AsyncTask;

import com.nhn.android.archetype.base.BaseApplication;
import com.nhn.android.archetype.base.cache.listener.FileCacheListener;
import com.nhn.android.archetype.base.object.BaseObj;
import com.nhn.android.archetype.base.util.internal.M2baseLogger;
import com.nhn.android.archetype.base.util.internal.M2baseUtility;

public class FileCacheHelper {
	private static M2baseLogger logger = M2baseLogger.getLogger(FileCacheHelper.class);
	
	private synchronized static void putData(String fileName, Object data) {
		logger.d("putData: %s", fileName);
		
		String raw = null;
		
		if (data instanceof BaseObj) {
			raw = ((BaseObj) data).toJson();
		} else {
			raw = data.toString();
		}
		
		OutputStream os = null;
		
		try {
			Context context = BaseApplication._internalInstance;
			os = context.openFileOutput(fileName, Context.MODE_PRIVATE);
			
			os.write(raw.getBytes());
		} catch (Exception e) {
			logger.e(e);
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
					logger.e(e);
				}
			}
		}
	}
	
	public static void put(String key, BaseObj baseObj) {
		String fileName = getCacheFileName(key);
		putData(fileName, baseObj);
	}
	
	public static void put(String key, String json) {
		String fileName = getCacheFileName(key);
		putData(fileName, json);
	}
	
	public static void put(String userId, String key, BaseObj baseObj) {
		String fileName = getCacheFileName(userId, key);
		putData(fileName, baseObj);
	}
	
	public static void put(String userId, String url, String json) {
		String fileName = getCacheFileName(userId, url);
		putData(fileName, json);
	}

	public static void putAsync(final String key, final BaseObj baseObj, final FileCacheListener listener) {
		BaseApplication._internalInstance.getBackgroundHandler().post(new Runnable() {
			@Override
			public void run() {
				String fileName = getCacheFileName(key);

				CacheSaveTask task = new CacheSaveTask(fileName, baseObj, listener);
				if (M2baseUtility.isICSCompatibility()) {
					task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				} else {
					task.execute();
				}
			}
		});
	}
	
	public static void putAsync(final String userId, final String key, final BaseObj baseObj, final FileCacheListener listener) {
		BaseApplication._internalInstance.getBackgroundHandler().post(new Runnable() {
			@Override
			public void run() {
				String fileName = getCacheFileName(userId, key);

				CacheSaveTask task = new CacheSaveTask(fileName, baseObj, listener);
				if (M2baseUtility.isICSCompatibility()) {
					task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				} else {
					task.execute();
				}
			}
		});
	}
	
	public static void putAsync(final String userId, final String url, final String json, final FileCacheListener listener) {
		BaseApplication._internalInstance.getBackgroundHandler().post(new Runnable() {
			@Override
			public void run() {
				String fileName = getCacheFileName(userId, url);

				CacheSaveTask task = new CacheSaveTask(fileName, json, listener);
				if (M2baseUtility.isICSCompatibility()) {
					task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				} else {
					task.execute();
				}
			}
		});
	}
	
	public static void putAsync(final String userId, final String urlPattern, final List<BaseObj> objList, final String[] keyList, final FileCacheListener listener) {
		if (objList == null || objList.size() == 0) {
			return;
		}
		
		BaseApplication._internalInstance.getBackgroundHandler().post(new Runnable() {
			@Override
			public void run() {
				SubclassUrlCacheGeneratorTask task = new SubclassUrlCacheGeneratorTask(userId, objList, urlPattern, keyList, listener);
				if (M2baseUtility.isICSCompatibility()) {
					task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				} else {
					task.execute();
				}
			}
		});
	}
	
	private static FileCache getData(String fileName) {
		InputStream is = null;
		
		try {
			File file = getCacheFile(fileName);
			if (file == null || file.exists() == false) {
				return null;
			}

			Context context = BaseApplication._internalInstance;
			is = context.openFileInput(fileName);
			
			if (is != null) {
				String json = M2baseUtility.convertStreamToString(is).trim();
				FileCache cache = new FileCache(json, new Date(file.lastModified()));
				
				return cache;
			}
		} catch (Exception e) {
			logger.e(e);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					logger.e(e);
				}
			}
		}
		
		return null;
	}
	
	public static boolean exists(String key) {
		String fileName = getCacheFileName(key);
		File file = getCacheFile(fileName);
		if (file == null || file.exists() == false) {
			return false;
		}
		
		return true;
	}
	
	public static boolean exists(String userId, String url) {
		String fileName = getCacheFileName(userId, url);
		File file = getCacheFile(fileName);
		if (file == null || file.exists() == false) {
			return false;
		}
		
		return true;
	}
	
	public static FileCache get(String userId, String url) {
		String fileName = getCacheFileName(userId, url);
		return getData(fileName);
	}
	
	public static FileCache get(String key) {
		String fileName = getCacheFileName(key);
		return getData(fileName);
	}
	
	public static void getAsync(final String key, final FileCacheListener listener) {
		BaseApplication._internalInstance.getBackgroundHandler().post(new Runnable() {
			@Override
			public void run() {
				String fileName = getCacheFileName(key);
				logger.d("getAsync: %s -> %s", key, fileName);
				File cache = getCacheFile(fileName);
				if (!cache.exists()) {
					if (listener != null) {
						BaseApplication._internalInstance.getHandler().post(new Runnable() {
							@Override
							public void run() {
								listener.onError();
							}
						});
					}
					return;
				}

				CacheLoadTask task = new CacheLoadTask(fileName, listener);
				if (M2baseUtility.isICSCompatibility()) {
					task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				} else {
					task.execute();
				}
			}
		});
	}
	
	public static void getAsync(String userId, String url, FileCacheListener listener) {
		String fileName = getCacheFileName(userId, url);
		getAsync(fileName, listener);
	}
	
	public static void clear(String key) {
		String fileName = getCacheFileName(key);
		
		File file = getCacheFile(fileName);
		if (file == null || file.exists() == false) {
			return;
		}
		
		file.delete();
	}
	
	public static void clear(String userId, String key) {
		String fileName = getCacheFileName(userId, key);
		
		File file = getCacheFile(fileName);
		if (file == null || file.exists() == false) {
			return;
		}
		
		file.delete();
	}
	
	public final static String getCacheFileName(String key) {
		if (M2baseUtility.isNullOrEmpty(key)) {
			return key;
		}
		
		if (key.startsWith("c") && key.endsWith(".tmp")) {
			return key;
		}
		
		return String.format("c%s.tmp", M2baseUtility.md5(key));
	}
	
	private final static String checkAndAppendParam(String url, String param, String value) {
		String pValue = param + "=";

		if (url.indexOf(pValue) < 0) {
			if (url.indexOf("?") < 0) {
				url += "?";
			}

			url += String.format("&%s%s", pValue, value);
		}

		return url;
	}
	
	public final static String getCacheFileName(String userId, String url) {
		url = checkAndAppendParam(url, "locale", M2baseUtility.getSystemLocaleString(BaseApplication._internalInstance));
		int idx = url.indexOf("asig=");
		
		// asig값은 동적으로 변화하기 때문에 캐시로 사용할 수 없음.
		if (idx > 0) {
			String prefix = url.substring(0, idx);
			String last = url.substring(idx);
			
			int end = last.indexOf("&");
			if (end > 0) {
				url = prefix + last.substring(end);
			} else {
				url = prefix;
			}
		}
		
		String key = (userId+":"+url.trim());
		logger.d("key: %s", key);
		
		return getCacheFileName(key);
	}
	
	private static File getCacheFile(String fileName) {
		Context context = BaseApplication._internalInstance;
		if (context != null) {
			return context.getFileStreamPath(fileName);
		}
		
		return null;
	}
	
	private static class CacheSaveTask extends AsyncTask<Void, Void, Void> {
		private String key;
		private Object data;
		private FileCacheListener listener;
		
		public CacheSaveTask(String key, Object data, FileCacheListener listener) {
			this.key = key;
			this.data = data;
			this.listener = listener;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			FileCacheHelper.putData(key, data);
			
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if (listener != null) {
				listener.onSuccess(null);
			}
		}
	}
	
	private static class CacheLoadTask extends AsyncTask<Void, Void, FileCache> {
		private String key;
		private FileCacheListener listener;
		
		public CacheLoadTask(String key, FileCacheListener listener) {
			this.key = key;
			this.listener = listener;
		}
		
		@Override
		protected FileCache doInBackground(Void... params) {
			return FileCacheHelper.getData(key);
		}

		@Override
		protected void onPostExecute(FileCache result) {
			if (listener != null) {
				listener.onSuccess(result);
			}
		}
	}
	
	private static class SubclassUrlCacheGeneratorTask extends AsyncTask<Void, Void, Void> {
		private String userId;
		private List<BaseObj> objList;
		private String urlPattern;
		private String[] keyList;
		private FileCacheListener listener;
		
		public SubclassUrlCacheGeneratorTask(String userId, List<BaseObj> objList, String urlPattern, String[] keyList, FileCacheListener listener) {
			this.userId = userId; 
			this.objList = objList;
			this.urlPattern = urlPattern;
			this.keyList = keyList;
			this.listener = listener;
		}
		
		private String checkAndAppendParam(String url, String param, String value) {
			String pValue = param + "=";

			if (url.indexOf(pValue) < 0) {
				if (url.indexOf("?") < 0) {
					url += "?";
				}

				url += String.format("&%s%s", pValue, value);
			}

			return url;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			urlPattern = urlPattern.replace(URLEncoder.encode("%s"), "%s");
			urlPattern = checkAndAppendParam(urlPattern, "locale", M2baseUtility.getSystemLocaleString(BaseApplication._internalInstance));
			urlPattern = checkAndAppendParam(urlPattern, "akey", BaseApplication._internalInstance.getAppKey());
			urlPattern = checkAndAppendParam(urlPattern, "asig", BaseApplication._internalInstance.getAppSig());
			
			logger.d("urlPattern: %s", urlPattern);
			
			for (BaseObj obj : objList) {
				Object[] values = new Object[keyList.length];
				for (int i=0;i<keyList.length;i++) {
					values[i] = obj.getString(keyList[i], "").trim();
				}
				
				String url = String.format(urlPattern, values);
				logger.d("url: %s", url);
				
				BaseObj tempObj = new BaseObj();
				tempObj.setDataMap(new HashMap(obj.getDataMap()));
				FileCacheHelper.put(userId, url, tempObj);
			}
			
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if (listener != null) {
				listener.onSuccess(null);
			}
		}
	}
}
