package com.nhn.android.m2base.worker;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.AbstractHttpMessage;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.SSLCertificateSocketFactory;
import android.net.SSLSessionCache;

import com.nhn.android.m2base.BaseApplication;
import com.nhn.android.m2base.cache.FileCache;
import com.nhn.android.m2base.cache.FileCacheHelper;
import com.nhn.android.m2base.object.ApiResponse;
import com.nhn.android.m2base.object.BaseObj;
import com.nhn.android.m2base.util.Base64Utility;
import com.nhn.android.m2base.util.internal.M2baseLogger;
import com.nhn.android.m2base.util.internal.M2baseUtility;
import com.nhn.android.m2base.worker.listener.JsonListener;
import com.nhn.android.m2base.worker.listener.PreloadJsonListener;
import com.nhn.android.m2base.worker.listener.PreloadSubListJsonListener;
import com.nhn.android.m2base.worker.multipart.FilePart;
import com.nhn.android.m2base.worker.multipart.MultipartEntity;
import com.nhn.android.m2base.worker.multipart.Part;
import com.nhn.android.m2base.worker.multipart.StringPart;

public class ApacheJsonWorker extends MultipartJsonWorker {
	private static M2baseLogger logger = M2baseLogger.getLogger(ApacheJsonWorker.class);
	
	private static final String ENC_TYPE = "gzip";
	private static final String RET_FAIL = "-1";
	//private static final String RET_SUCCESS = "0";
	//private static final String RET_UNAUTHORIZED = "401";
	
	private static final int DEFAULT_CONNECTION_TIMEOUT = 15 * 1000;
	private static final int DEFAULT_SO_TIMEOUT = 15 * 1000;
	private static final int DEFAULT_MULTIPART_CONNECTION_TIMEOUT = 15 * 1000;
	private static final int DEFAULT_MULTIPART_SO_TIMEOUT = 60 * 1000;

	private static HttpClient client;
	
	public static HttpClient createHttpClient() {
		Context context = ApacheJsonWorker.application;
		
		HttpParams params = new BasicHttpParams();
		params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
		
		ConnManagerParams.setMaxTotalConnections(params, 200);
		
		HttpConnectionParams.setConnectionTimeout(params, DEFAULT_CONNECTION_TIMEOUT);
		HttpConnectionParams.setSoTimeout(params, DEFAULT_SO_TIMEOUT);
		HttpConnectionParams.setStaleCheckingEnabled(params, false);
		HttpConnectionParams.setTcpNoDelay(params, true);
		
		HostnameVerifier hostnameVerifier = org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER; 
		SchemeRegistry registry = new SchemeRegistry(); 
		
		SSLSocketFactory socketFactory = null;
		if (context != null && ApacheJsonWorker.enableSessionCache) {
			SSLSessionCache sessionCache = new SSLSessionCache(context);
			socketFactory = SSLCertificateSocketFactory.getHttpSocketFactory(DEFAULT_CONNECTION_TIMEOUT, sessionCache);
		} else {
			socketFactory = SSLSocketFactory.getSocketFactory();
		}
		
		socketFactory.setHostnameVerifier((X509HostnameVerifier) hostnameVerifier); 
		registry.register(new Scheme("https", socketFactory, 443)); 
		registry.register (new Scheme ("http", PlainSocketFactory.getSocketFactory (), 80)); 
		ThreadSafeClientConnManager mgr = new ThreadSafeClientConnManager(params, registry); 
		HttpClient client = new DefaultHttpClient(mgr, params); 
		
		// Set verifier      
		HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier); 
		
		return client;
	}
	
	public static synchronized HttpClient getHttpClient() {
		if (client == null) {
			client = createHttpClient();
		}
		
		return client;
	}
	
	private int timeout = DEFAULT_SO_TIMEOUT;
	private boolean networkError = false;

	private boolean useAsigParam = false;
	private boolean useLocaleParam = true;

	private String responseJson;
	

	private String url;
	
	private boolean syncMode = false;
	private BaseObj resultObj = null;
	
	private JsonListener jsonListener = null;
	
	private HttpRequestBase requestBase;
	private FilePart filePart;
	
	public ApacheJsonWorker(String url) {
		setUrl(url);
	}
	
	public ApacheJsonWorker(String url, JsonListener jsonListener) {
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

			url += String.format("&%s%s", pValue, value);
		}

		return url;
	}
	
	public void post() {
		syncMode = false;
		
		if (ApacheJsonWorker.application == null) {
			Executors.newCachedThreadPool().execute((Runnable) this);
		} else {
			ApacheJsonWorker.application.addWorker(this);		
		}
	}
	
	protected HttpRequestBase createHttpMehtod(String url) {
		return new HttpPost(url);
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
	
	@Override
	protected void doWork() {
		logger.d("doWork: %s", url);
		
		// 정확한 캐시의 동작을 위해 파라미터는 파라미터는 여기에서 추가해야 함.
		Context context = ApacheJsonWorker.application;
		if (useLocaleParam) {
			if (context != null) {
				url = checkAndAppendParam(url, "locale", M2baseUtility.getSystemLocaleString(context));
			}
		}

		if (useAsigParam) {
			url = checkAndAppendParam(url, "akey", ApacheJsonWorker.globalAppKey);
			url = checkAndAppendParam(url, "asig", ApacheJsonWorker.globalAppSig);
		}
		
		FileCache cache = null;
		
		if (isPreload()) {
			cache = doPreload();
		}
		
		int iRetryCount = 0;
		networkError = false;

		while (iRetryCount < getRetrycount()) {
			InputStream in = null;
			InputStream gzipInputStream = null;
			InputStream originalInputStream = null;
			HttpEntity entitiy = null;
			
			try {
				iRetryCount++;
				
				int tempId = new Random().nextInt(20);
				long startTime = System.currentTimeMillis();
				
				logger.d("%s] doWork url(%s)", tempId, url);
				String extraParams = "";
				String currentUrl = url;
				int idx = url.indexOf("?");
				if (idx > 0) {
					extraParams = url.substring(idx+1);
					currentUrl = url.substring(0, idx);
				}
				
				requestBase = createHttpMehtod(currentUrl);
				
				HttpClient httpClient = getHttpClient();
				settingHttpClient(requestBase, httpClient, extraParams);
				
				logger.d("%s] before execute : %sms", tempId, (System.currentTimeMillis() - startTime));
				
				HttpResponse response = httpClient.execute(requestBase);
				entitiy = response.getEntity();
				
				logger.d("%s] execute : %sms", tempId, (System.currentTimeMillis() - startTime));
				in = entitiy.getContent();
				originalInputStream = in;
				
				Header contentEncoding = response.getFirstHeader("Content-Encoding");
				if (contentEncoding != null && (contentEncoding.getValue().equalsIgnoreCase("gzip") || contentEncoding.getValue().equalsIgnoreCase("x-gzip"))) {
					gzipInputStream = new GZIPInputStream(in);
					in = gzipInputStream;
				}
				
				int responseCode = response.getStatusLine().getStatusCode();
				logger.d("%s] ResponseCode(%s), StatusLine(%s)", tempId, responseCode, response.getStatusLine().getReasonPhrase());

				if (responseCode != HttpURLConnection.HTTP_OK) {
					onError(response, in);
					return;
				}

				logger.d("%s] end : %sms", tempId, (System.currentTimeMillis() - startTime));
				onSuccess(response, in, cache);
				break;
			} catch (SocketTimeoutException timeoutEx) {
				logger.e(timeoutEx);
				requestBase.abort();
				
				if (iRetryCount >= getRetrycount()) {
					onNetworkError();
					return;
				}
			} catch (SocketException e) {
				logger.e(e);
				requestBase.abort();
				
				if (iRetryCount >= getRetrycount()) {
					onNetworkError();
					return;
				}
			} catch (Exception e) {
				logger.e(e);
				requestBase.abort();
				
				if (iRetryCount >= getRetrycount()) {
					onNetworkError();
					return;
				}
			} finally {
				logger.d("release connections");
				
				try {
					if (gzipInputStream != null) {
						gzipInputStream.close();
					}
				} catch (Exception e) {
				}
				
				try {
					if (originalInputStream != null) {
						originalInputStream.close();
					}
				} catch (IOException e) {
				}
				
				if (entitiy != null) {
					try {
						entitiy.consumeContent();
					} catch (IOException e) {
					}
				}
				
				if (requestBase != null) {
					requestBase = null;
				}
			}
		}
	}
	
	@Override
	public void abort() {
		try {
			if (requestBase != null) {
				requestBase.abort();
			}
		} catch (Exception e) {
			logger.e(e);
		}
	}

	@Override
	public long getSendigFileLength() {
		// 멀티업로드일경우 정상동작 보장 못함.
		if (filePart == null) {
			return 0;
		} else {
			return filePart.getSendingDataLength();
		}
	}

	@Override
	public long getTotalFileLength() {
		// 멀티업로드일경우 정상동작 보장 못함.
		if (filePart == null) {
			return 0;
		} else {
			return filePart.getTotalLength();
		}
	}
	

	/**
	 * 미투데이 서버 api 접속 방법 참조.
	 * Http Basic Auth를 사용한다.
	 * @throws UnsupportedEncodingException 
	 */
	protected void settingHttpClient(AbstractHttpMessage httpMessage, HttpClient httpClient, String extraParams) throws UnsupportedEncodingException {
		Context context = ApacheJsonWorker.application;
		
		String loginId = ApacheJsonWorker.application.getUserId();
		String fullAuthToken = ApacheJsonWorker.application.getFullAuthToken();

		if (!isSkipAuthrization() && (M2baseUtility.isNotNullOrEmpty(loginId) && M2baseUtility.isNotNullOrEmpty(fullAuthToken))) {
			StringBuffer buffer = new StringBuffer();
			buffer.append(loginId).append(":").append("full_auth_token ").append(fullAuthToken);
			Base64Utility base64 = new Base64Utility(false);
			String encodeValue = base64.encode(buffer.toString());
			buffer.setLength(0);
			buffer.append("Basic ").append(encodeValue);
			String authorization = buffer.toString();

			logger.d("Authorization (%s, %s): %s", loginId, fullAuthToken, authorization);

			httpMessage.setHeader("Authorization", authorization);
		} else {
			logger.d("skip authrization");
		}

		httpMessage.setHeader("Accept-Encoding", ENC_TYPE);
		httpMessage.setHeader("User-Agent", ApacheJsonWorker.application.getUserAgent());		
		httpMessage.setHeader("Connection", "Keep-Alive");

		//TODO 네이버 로그인 모듈에서 받아온 쿠키 설정
		String cookie = getNloginCookie();
		if (M2baseUtility.isNotNullOrEmpty(cookie)) {
			httpMessage.setHeader("Cookie", cookie);
			logger.d("settingHttpClient(), Cookie(%s)", cookie);
		}
		
		String aKey = ApacheJsonWorker.globalAppKey;
		String aSig = ApacheJsonWorker.globalAppSig;
		httpMessage.setHeader("Me2_application_key", aKey);
		httpMessage.setHeader("Me2_asig", aSig);

		try {
			String appVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
			httpMessage.setHeader("Me2_application_version", appVersion);
			logger.d("settingHttpClient(), appVersion(%s)", appVersion);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		ArrayList<Part> partArray = new ArrayList<Part>();
		AbstractHttpEntity entity = null;
		
		if (extraParams != null && extraParams.length() > 0) {
			String[] parts = extraParams.split("&");
			for (String part : parts) {
				String[] data = part.split("=");
				
				if (data.length == 2) {
					partArray.add(new StringPart(data[0], data[1], "UTF8"));
				} else if (data.length == 1) {
					partArray.add(new StringPart(data[0], "", "UTF8"));
				}
			}
		}

		HttpParams httpParams = httpClient.getParams();
		httpParams.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT);
		httpParams.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, DEFAULT_SO_TIMEOUT);
		
		if (getAttachment() != null && getAttachment().size() > 0) {
			httpParams.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, DEFAULT_MULTIPART_CONNECTION_TIMEOUT);
			httpParams.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, DEFAULT_MULTIPART_SO_TIMEOUT);
			
			setRetrycount(1);
			
			//싱글모드 : 밴드커버/프로필사진
			//멀티모드 : 멀티사진 업로드
			if (isSingleAttach()) {
				try {
					filePart = new FilePart("attachment", getAttachment().get(0));
					partArray.add(filePart);
				} catch (FileNotFoundException e) {
					logger.e(e);
				}
			} else {
				for (int i=0;i<getAttachment().size();i++) {
					try {
						filePart = new FilePart("attachment"+(i+1), getAttachment().get(i));
						partArray.add(filePart);
					} catch (FileNotFoundException e) {
						logger.e(e);
					}
				}
			}
			
			int count = partArray.size();
			Part[] parts = new Part[count];
			for (int i = 0; i < count; i++) {
				parts[i] = partArray.get(i);
			}
			
			entity = new MultipartEntity(parts);
		} else {
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			
			for (Part part : partArray) {
				StringPart spart = (StringPart) part;
				params.add(new BasicNameValuePair(spart.getName(), URLDecoder.decode(spart.getValue()))); 
			}
			
			entity = new UrlEncodedFormEntity(params, "UTF-8");
		}
		
		HttpPost httpPost = (HttpPost)httpMessage;
		httpPost.setEntity(entity);
	}
	
	protected FileCache doPreload() {
		try {
			logger.d("TIME: doPreload : %s", System.currentTimeMillis());
			
			final FileCache cache = FileCacheHelper.get(ApacheJsonWorker.application.getUserId(), getUrl());
			
			if (cache != null) {
				logger.d("Cache exists: %s", cache.getCachedDate());

				if (syncMode) {
					resultObj = cache.getModel();
					((PreloadJsonListener) jsonListener).onPreload(cache.getModel(), cache.getCachedDate());
					return cache;
				}
				
				getHandler().post(new Runnable() {
					public void run() {
						((PreloadJsonListener) jsonListener).onPreload(cache.getModel(), cache.getCachedDate());
					}
				});
			} else {
				getHandler().post(new Runnable() {
					public void run() {
						((PreloadJsonListener) jsonListener).onPreload(null, null);
					}
				});
			}
			
			return cache;
		} catch (Exception e) {
			logger.e(e);
		}
		
		return null;
	}
	
	@SuppressWarnings("unchecked")
	protected void onSuccess(HttpResponse response, InputStream is, FileCache cache) {
		if (jsonListener != null) {
			String json = M2baseUtility.convertStreamToString(is);
			logger.d("var ret = %s", json);
			
			if (cache != null) {
				if (cache.getJson().equals(json)) {
					logger.d("skip onSuccess");
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
						jsonListener.onSuccess(obj);
					}
				});
			}
			
			if (isPreload()) {
				FileCacheHelper.put(ApacheJsonWorker.application.getUserId(), getUrl(), json);
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
	
	protected void onError(HttpResponse response, InputStream is) {
		if (jsonListener != null) {
			final BaseObj obj = BaseObj.parse(is, BaseObj.class);
			final int statusCode = response.getStatusLine().getStatusCode();
			
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
	
	protected void onNetworkError() {
		try {
			if (jsonListener != null) {
				getHandler().post(new Runnable() {
					public void run() {
						ApiResponse response = new ApiResponse();
						response.setCode(RET_FAIL);

						response.setMessage("Unstable network connection. Check your network status.");
						response.setDescription("Unstable network connection. Check your network status.");
						
						resultObj = response;
						jsonListener.onError(999, response);
					}
				});
			}
		} catch (Exception e) {
			logger.e(e);
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
