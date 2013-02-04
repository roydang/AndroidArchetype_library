package com.nhn.android.archetype.base.worker;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;

import com.nhn.android.archetype.base.BaseApplication;
import com.nhn.android.archetype.base.cache.FileCache;
import com.nhn.android.archetype.base.cache.FileCacheHelper;
import com.nhn.android.archetype.base.object.ApiResponse;
import com.nhn.android.archetype.base.object.BaseObj;
import com.nhn.android.archetype.base.util.Base64Utility;
import com.nhn.android.archetype.base.util.internal.M2baseLogger;
import com.nhn.android.archetype.base.util.internal.M2baseUtility;
import com.nhn.android.archetype.base.worker.listener.JsonListener;
import com.nhn.android.archetype.base.worker.listener.PreloadJsonListener;
import com.nhn.android.archetype.base.worker.listener.PreloadSubListJsonListener;

public class AndroidHttpJsonWorker extends MultipartJsonWorker {
	public static final String METHOD_POST = "POST";
	public static final String METHOD_GET = "GET";
	public static final String METHOD_OPTIONS = "OPTIONS";
	public static final String METHOD_DELETE = "DELETE";
	public static final String METHOD_DEFAULT = METHOD_POST;
	
	private static final String RET_FAIL = "-1";
	
	private static final int DEFAULT_SO_TIMEOUT = 15 * 1000;
	
	private static M2baseLogger logger = M2baseLogger.getLogger(AndroidHttpJsonWorker.class);

	private static final String CRLF = "\r\n";
	private static final String TWO_HYPHENS = "--";
	private static final String BOUNDARY = "*****b*o*u*n*d*a*r*y*****";

	private int timeout = DEFAULT_SO_TIMEOUT;
	private boolean networkError = false;

	private boolean useAsigParam = false;
	private boolean useLocaleParam = true;

	private String responseJson;

	private String url;

	private boolean syncMode = false;
	private BaseObj resultObj = null;

	private JsonListener jsonListener = null;
	private HttpURLConnection conn;
	private DataOutputStream currentOutputStream;

	private long currentSendedBytes;
	private long currentSendingFileLength;
	private long currentTotalFileLength;

	private FileCache currentCache = null;
	//private AtomicBoolean preloadExecuted;
	private AtomicBoolean successExecuted;
	
	private String method = METHOD_DEFAULT;
	
	public AndroidHttpJsonWorker(String url) {
		setUrl(url);
	}

	public AndroidHttpJsonWorker(String url, JsonListener jsonListener) {
		setUrl(url);
		setJsonListener(jsonListener);
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public boolean isNetworkError() {
		return networkError;
	}

	public void setNetworkError(boolean networkError) {
		this.networkError = networkError;
	}

	public boolean isUseAsigParam() {
		return useAsigParam;
	}

	public void setUseAsigParam(boolean useAsigParam) {
		this.useAsigParam = useAsigParam;
	}

	public boolean isUseLocaleParam() {
		return useLocaleParam;
	}

	public void setUseLocaleParam(boolean useLocaleParam) {
		this.useLocaleParam = useLocaleParam;
	}

	public String getResponseJson() {
		return responseJson;
	}

	public void setResponseJson(String responseJson) {
		this.responseJson = responseJson;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}
	
	private boolean isMethodPost() {
		return METHOD_POST.equalsIgnoreCase(getMethod());
	}

	public JsonListener getJsonListener() {
		return jsonListener;
	}

	public void setJsonListener(JsonListener jsonListener) {
		this.jsonListener = jsonListener;
	}

	public boolean isPreload() {
		return jsonListener instanceof PreloadJsonListener;
	}

	private String checkAndAppendParam(String url, String param, String value) {
		String pValue = param + "=";

		if (url.indexOf(pValue) < 0) {
			if (url.indexOf("?") < 0) {
				url += "?";
			}

			url += M2baseUtility.format("&%s%s", pValue, value);
		}

		return url;
	}

	public void post() {
		syncMode = false;

		if (JsonWorker.application == null) {
			Executors.newCachedThreadPool().execute((Runnable) this);
		} else {
			JsonWorker.application.addWorker(this);
		}
	}

	@Override
	public BaseObj postSync() {
		syncMode = true;

		if (jsonListener == null) {
			setJsonListener(new JsonListener() {
				public void onSuccess(BaseObj result) {
				}

				public void onError(int statusCode, ApiResponse result) {
				}
			});
		}

		doWork();

		return resultObj;
	}

	private final TrustManager[] trustAllCerts = new TrustManager[]{
	    new X509TrustManager() {
	        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
	            return null;
	        }
	        public void checkClientTrusted(
	            java.security.cert.X509Certificate[] certs, String authType) {
	        }
	        public void checkServerTrusted(
	            java.security.cert.X509Certificate[] certs, String authType) {
	        }
	    }
	};
	
	protected void setupHttps() {
		if (M2baseUtility.isUnderFroyo()) {
			System.setProperty("http.keepAlive", "false");
		}
		
		try {
		    SSLContext sc = SSLContext.getInstance("SSL");
		    sc.init(null, trustAllCerts, null);
		    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {
		}
	}
	
	protected HttpURLConnection createConnection(String url, int streamingSize) throws IOException {
		setupHttps();
		
		logger.d("createConnection: %s %s", url, streamingSize);
		
		URL connectURL = new URL(url);
		HttpURLConnection conn = (HttpURLConnection) connectURL.openConnection();
		
		if (conn instanceof HttpsURLConnection) {
			((HttpsURLConnection) conn).setHostnameVerifier(new HostnameVerifier() {
				@Override
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			});
		}


		if (isMethodPost() == false || getAttachment() == null || getAttachment().size() == 0) {
			conn.setConnectTimeout(getTimeout());
		}
		
		conn.setRequestProperty("User-agent", JsonWorker.application.getUserAgent());
		
		//TODO 네이버 로그인 모듈에서 받아온 쿠키 설정
		String cookie = getNloginCookie();
		if (M2baseUtility.isNotNullOrEmpty(cookie)) {  
			conn.setRequestProperty("Cookie", cookie);
			logger.d("createConnection(), Cookie(%s)", cookie);
		}
		
		conn.setDoInput(true);
		conn.setUseCaches(false);
		
		if (isMethodPost()) {
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			
			if (getAttachment() != null && getAttachment().size() > 0) {
				for (File file : getAttachment()) {
					streamingSize += file.length();
				}
				conn.setRequestProperty("Content-Type", M2baseUtility.format("multipart/form-data;boundary=%s", BOUNDARY));
				conn.setFixedLengthStreamingMode(streamingSize);
			} else {
				conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
			}
		}
		
		String loginId = JsonWorker.application.getUserId();
		String fullAuthToken = JsonWorker.application.getFullAuthToken();
		if (!isSkipAuthrization() && (M2baseUtility.isNotNullOrEmpty(loginId) && M2baseUtility.isNotNullOrEmpty(fullAuthToken))) {
			StringBuffer buffer = new StringBuffer();
			buffer.append(loginId).append(":").append("full_auth_token ").append(fullAuthToken);
			Base64Utility base64 = new Base64Utility(false);
			String encodeValue = base64.encode(buffer.toString());
			buffer.setLength(0);
			buffer.append("Basic ").append(encodeValue);
			String authorization = buffer.toString();
			conn.setRequestProperty("Authorization", authorization);  
		}
		
		conn.connect();
		return conn;
	}

	private void writeBytes(DataOutputStream dataStream, String text) throws UnsupportedEncodingException, IOException {
		dataStream.write(text.getBytes("UTF-8"));
	}
	
	protected void writeFormField(DataOutputStream dataStream, String key, String value) {
		logger.d("writeFormField: %s => %s", key, value);
		
		try {
			writeBytes(dataStream, TWO_HYPHENS + BOUNDARY + CRLF);
			writeBytes(dataStream, M2baseUtility.format("Content-Disposition: form-data; name=\"%s\"%s", key, CRLF));
			writeBytes(dataStream, CRLF);
			writeBytes(dataStream, value);
			writeBytes(dataStream, CRLF);
		} catch (Exception e) {
			logger.e(e);
		}
	}

	protected void writeFileField(DataOutputStream dataStream, String key, File file, String contentType, boolean test) {
		FileInputStream fis = null;
		
		try {
			fis = new FileInputStream(file);
			String value = file.getName();

			if (test == false) {
				int bytesAvailable = fis.available();
				
				currentSendingFileLength = 0;
				currentTotalFileLength = bytesAvailable;
			}
			
			logger.d("file: %s - %sbytes", file.getAbsolutePath(), currentTotalFileLength);
			
			// opening boundary line
			writeBytes(dataStream, TWO_HYPHENS + BOUNDARY + CRLF);
			writeBytes(dataStream, M2baseUtility.format("Content-Disposition: form-data; name=\"%s\";filename=\"%s\"%s",key, value, CRLF));
			
			if (M2baseUtility.isNotNullOrEmpty(contentType)) {
				contentType = "binary";
			}
			
			writeBytes(dataStream, "Content-Type: " + contentType + CRLF);
			writeBytes(dataStream, CRLF);

			if (test == false) {
				currentSendedBytes = dataStream.size();
				
				// create a buffer of maximum size
				int maxBufferSize = 8 * 1024;

				int bufferSize = maxBufferSize;
				byte[] buffer = new byte[bufferSize];

				int bytesRead = fis.read(buffer, 0, bufferSize);
				while (bytesRead > 0) {
					dataStream.write(buffer, 0, bytesRead);
					currentSendingFileLength += bytesRead;
					
					bytesRead = fis.read(buffer, 0, bufferSize);
				}
			}
			
			writeBytes(dataStream, CRLF);
		} catch (Exception e) {
			logger.e(e);
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
				}
			}
		}
	}

	protected void writeFields(DataOutputStream outputStream, String extraParams, boolean test) {
		if (getAttachment() != null && getAttachment().size() > 0) {
			if (extraParams != null && extraParams.length() > 0) {
				String[] parts = extraParams.split("&");
				for (String part : parts) {
					String[] data = part.split("=");

					if (data.length == 2) {
						writeFormField(outputStream, data[0], data[1]);
					} else if (data.length == 1) {
						writeFormField(outputStream, data[0], "");
					}
				}
			}
			
			setRetrycount(1);
			
			if (isSingleAttach()) {
				writeFileField(outputStream, "attachment", getAttachment().get(0), null, test);
			} else {
				for (int i=0;i<getAttachment().size();i++) {
					writeFileField(outputStream, "attachment"+(i+1), getAttachment().get(i), null, test);
				}
			}

			try {
				writeBytes(outputStream, TWO_HYPHENS + BOUNDARY + TWO_HYPHENS + CRLF);
				outputStream.flush();
			} catch (IOException e) {
				logger.e(e);
			}
		} else {
			try {
				writeBytes(outputStream, extraParams);
				outputStream.flush();
			} catch (Exception e) {
				logger.e(e);
			}
		}
	}
	
	@Override
	protected void doWork() {
		logger.d("doWork: %s", url);

		// 정확한 캐시의 동작을 위해 파라미터는 파라미터는 여기에서 추가해야 함.
		Context context = JsonWorker.application;
		if (useLocaleParam) {
			if (context != null) {
				url = checkAndAppendParam(url, "locale", M2baseUtility.getSystemLocaleString(context));
			}
		}

		if (useAsigParam) {
			url = checkAndAppendParam(url, "akey", JsonWorker.globalAppKey);
			url = checkAndAppendParam(url, "asig", JsonWorker.globalAppSig);
		}

		int tempId = new Random().nextInt(20);
		long startTime = System.currentTimeMillis();
		
		if (isPreload()) {
			doPreload();
		}
		
		logger.d("%s] preload : %sms", tempId, (System.currentTimeMillis() - startTime));

		int iRetryCount = 0;
		networkError = false;

		while (iRetryCount < getRetrycount()) {
			DataOutputStream outputStream = null;
			InputStream in = null;

			try {
				iRetryCount++;

				logger.d("%s] doWork url(%s)", tempId, url);
				String extraParams = "";
				String currentUrl = url;
				
				if (isMethodPost()) {
					int idx = url.indexOf("?");
					if (idx > 0) {
						extraParams = url.substring(idx + 1);
						currentUrl = url.substring(0, idx);
					}
				}

				logger.d("%s] before execute : %sms", tempId, (System.currentTimeMillis() - startTime));
				if (getAttachment() != null && getAttachment().size() > 0) {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					DataOutputStream testOutputStream = new DataOutputStream(baos);
					writeFields(testOutputStream, extraParams, true);
					
					conn = createConnection(currentUrl, baos.size());
				} else {
					conn = createConnection(currentUrl, 0);
				}

				if (isMethodPost()) {
					outputStream = new DataOutputStream(conn.getOutputStream());
					writeFields(outputStream, extraParams, false);
					
					if (outputStream != null) {
						try {
							outputStream.close();
							outputStream = null;
						} catch (Exception e) {
						}
					}
				}

				logger.d("%s] execute : %sms", tempId, (System.currentTimeMillis() - startTime));
				
				int responseCode = conn.getResponseCode();
				logger.d("%s] ResponseCode(%s), StatusLine(%s)", tempId, responseCode, conn.getResponseMessage());

				if (responseCode != HttpURLConnection.HTTP_OK) {
					in = conn.getErrorStream();
					onError(in, responseCode);
					return;
				}

				in = conn.getInputStream();
				logger.d("%s] end : %sms", tempId, (System.currentTimeMillis() - startTime));

				String json = M2baseUtility.convertStreamToString(in).trim();
				if (M2baseUtility.isNullOrEmpty(json)) {
					if (iRetryCount >= getRetrycount()) {
						onNetworkError();
						return;
					}
				} else {
					onSuccess(json);	
					return;
				}
			} catch (SocketTimeoutException timeoutEx) {
				logger.e(timeoutEx);

				if (iRetryCount >= getRetrycount()) {
					onNetworkError();
					return;
				}
			} catch (SocketException e) {
				logger.e(e);

				if (iRetryCount >= getRetrycount()) {
					onNetworkError();
					return;
				}
			} catch (Exception e) {
				logger.e(e);

				if (iRetryCount >= getRetrycount()) {
					onNetworkError();
					return;
				}
			} finally {
				logger.d("release connections");
				
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
					}
					
					in = null;
				}
				
				if (outputStream != null) {
					try {
						outputStream.close();
					} catch (IOException e) {
					}
					
					outputStream = null;
				}
				
				if (conn != null) {
					conn.disconnect();
					conn = null;
				}
			}
		}
	}
	

	@SuppressLint("NewApi")
	protected FileCache doPreload() {
		if (syncMode) {
			try {
				final FileCache cache = FileCacheHelper.get(AndroidHttpJsonWorker.application.getUserId(), getUrl());

				if (cache != null) {
					currentCache = cache;
					logger.d("Cache exists: %s", cache.getCachedDate());

					resultObj = cache.getModel();
					((PreloadJsonListener) jsonListener).onPreload(cache.getModel(), cache.getCachedDate());
					return cache;
				}
			} catch (Exception e) {
				logger.e(e);
			}
		} else {
			PreloadAsyncTask task = new PreloadAsyncTask();
			if (M2baseUtility.isICSCompatibility()) {
				task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			} else {
				task.execute();
			}
		}
		
		return null;
	}

	@SuppressWarnings("unchecked")
	private void onSuccess(final String json) {
		logger.d("var ret = %s", json);
		
		successExecuted = new AtomicBoolean(true);
		
		if (jsonListener != null) {
			if (currentCache != null) {
				logger.d("hasCurrentCache");

				if (currentCache.getJson().equals(json)) {
					logger.d("skip onSuccess");
					if (jsonListener instanceof PreloadJsonListener) {
						logger.d("skip onSuccess");
							
						getHandler().post(new Runnable() {
							public void run() {
								((PreloadJsonListener) jsonListener).onSkipSuccess(currentCache.getModel());
							}
						});
					}
					return;
				}
			}
			
			final BaseObj obj = BaseObj.parse(json, BaseObj.class);

			if (syncMode) {
				resultObj = obj;
				jsonListener.onSuccess(obj);
				return;
			}

			if (!this.isCanceled()) {
				getHandler().post(new Runnable() {
					public void run() {
						if (currentCache != null) {
							logger.d("hasCurrentCache");
							boolean compare = currentCache.getJson().equals(json);
							if (compare) {
								logger.d("skip onSuccess");
								return;
							}
						}
						
						jsonListener.onSuccess(obj);
					}
				});
			}
			
			if (isPreload()) {
				FileCacheHelper.put(AndroidHttpJsonWorker.application.getUserId(), getUrl(), json);
				try {
					if (jsonListener instanceof PreloadSubListJsonListener) {
						PreloadSubListJsonListener preloadSublistJsonListener = (PreloadSubListJsonListener) jsonListener;
						BaseObj tempObj = BaseObj.parse(json, BaseObj.class);
						
						FileCacheHelper.putAsync(BaseApplication._internalInstance.getUserId(), preloadSublistJsonListener.getUrlPattern(), 
								(List<BaseObj>) tempObj.getList(preloadSublistJsonListener.getSublistKey(), BaseObj.class), 
								preloadSublistJsonListener.getKeyList(), null);
					}
				} catch (Exception e) {
					logger.e(e);
				}
			}
		}
	}

	private void onNetworkError() {
		try {
			if (jsonListener != null) {
				getHandler().post(new Runnable() {
					public void run() {
						ApiResponse response = new ApiResponse();
						response.setCode(RET_FAIL);

						response.setMessage("A network error has occurred");
						response.setDescription("A network error has occurred");
						
						resultObj = response;
						jsonListener.onError(RESULT_CODE_NETWORK_ERROR, response);
					}
				});
			}
		} catch (Exception e) {
			logger.e(e);
		}
	}

	private void onError(InputStream in, final int statusCode) {
		if (jsonListener != null) {
			String json = M2baseUtility.convertStreamToString(in);
			logger.d("onError: %s", json);
			
			final BaseObj obj = BaseObj.parse(json, BaseObj.class);
			logger.d("var ret = %s", obj);
			
			if (syncMode) {
				resultObj = obj;
				jsonListener.onError(statusCode, obj.asApiResponse());
				return;
			} 
			
			getHandler().post(new Runnable() {
				public void run() {
					jsonListener.onError(statusCode, obj.asApiResponse());
				}
			});
		}
	}

	@Override
	public void abort() {
		if (conn != null) {
			conn.disconnect();
		}
	}

	@Override
	public long getSendigFileLength() {
		if (currentOutputStream != null) {
			return currentOutputStream.size() - currentSendedBytes;
		}
		
		return currentSendingFileLength;
	}

	@Override
	public long getTotalFileLength() {
		return currentTotalFileLength;
	}
	
	private class PreloadAsyncTask extends AsyncTask<Void, Void, FileCache> {
		@Override
		protected FileCache doInBackground(Void... params) {
			return FileCacheHelper.get(AndroidHttpJsonWorker.application.getUserId(), getUrl());
		}

		@Override
		protected void onPostExecute(final FileCache cache) {
			if (successExecuted != null && successExecuted.get()) {
				return;
			}
			
			currentCache = cache;
			
			if (cache != null) {
				logger.d("Cache exists: %s", cache.getCachedDate());
				((PreloadJsonListener) jsonListener).onPreload(cache.getModel(), cache.getCachedDate());
			} else {
				((PreloadJsonListener) jsonListener).onPreload(null, null);
			}
		}
	}

	@Override
	public void setConnectionTimeout(int timeout) {
		setTimeout(timeout);
	}

	@Override
	public int getConnectionTimeout() {
		return getTimeout();
	}
}
