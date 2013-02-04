package com.nhn.android.archetype.base.worker;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.os.AsyncTask;

import com.nhn.android.archetype.base.BaseApplication;
import com.nhn.android.archetype.base.object.ApiResponse;
import com.nhn.android.archetype.base.util.internal.M2baseLogger;
import com.nhn.android.archetype.base.util.internal.M2baseUtility;
import com.nhn.android.archetype.base.worker.listener.ApiRequestListener;

public class FileDownloader extends AsyncTask<Void, Void, File> {
	private static M2baseLogger logger = M2baseLogger.getLogger(FileDownloader.class);

	private String url;
	private String savePath;
	private ApiRequestListener<File, ApiResponse> listener;
	
	private HttpURLConnection connection;
	
	public FileDownloader(String url, String savePath, ApiRequestListener<File, ApiResponse> listener) {
		this.url = url;
		this.savePath = savePath;
		this.listener = listener;
	}
	
	public String getUrl() {
		return url;
	}

	public String getSavePath() {
		return savePath;
	}

	public ApiRequestListener<File, ApiResponse> getListener() {
		return listener;
	}

	public void disconnect() {
		try {
			if (connection != null) {
				connection.disconnect();
			}
			
			connection = null;
		} catch (Exception e) {
			
		}
		
		this.cancel(true);
	}
	
	public File postSync() {
		return doInBackground();
	}
	
	protected File doInBackground(Void... params) {
		InputStream is = null;
		BufferedInputStream bis = null;
		File saveFile = new File(savePath);
		
		try {
			if (M2baseUtility.isUnderFroyo()) {
				System.setProperty("http.keepAlive", "false");
			}
			
			URL url = new URL(getUrl());

			connection = (HttpURLConnection) url.openConnection();
			connection.setDoInput(true);
			
			long startTime = System.currentTimeMillis();
			
			int responseCode = connection.getResponseCode();
			if (responseCode != 200) {
				logger.d("responseCode: %s", responseCode);
				return null;
			}
			
			is = connection.getInputStream();
			
			logger.d("execute: %sms", (System.currentTimeMillis() - startTime));
			
			File folder = BaseApplication._internalInstance.getExternalCacheFolder();
			String fileName = String.format("tdn%s.tmp", System.currentTimeMillis());
			
			File tempFile = new File(folder, fileName);
			FileOutputStream fos = new FileOutputStream(tempFile);
			
			bis = new BufferedInputStream(is);
			byte[] buffer = new byte[6 * 1024];
			
			while (true) {
				int len = bis.read(buffer);
				if (len <= 0) {
					break;
				}
				
				fos.write(buffer, 0, len);
			}
			
			fos.close();
			
			File parent = saveFile.getParentFile();
			if (parent.exists() == false) {
				logger.d("createPath: %s", parent.getAbsolutePath());
				parent.mkdirs();
			}
			
			if (saveFile.exists()) {
				saveFile.delete();
			}
			
			tempFile.renameTo(saveFile);
		} catch (Exception e) {
			logger.d("url: %s", url);
			logger.e(e);
			connection.disconnect();
			
			return null;
		} catch (Error err) {
			logger.e(err);
			connection.disconnect();
			
			return null;
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
				}
			}
			
			if (bis != null) {
				try {
					bis.close();
				} catch (Exception e) {
				}
			}
		}

		return saveFile;
	}
	
	@Override
	protected void onPostExecute(File result) {
		if (listener != null) {
			if (result != null) {
				listener.onSuccess(result);
			} else {
				ApiResponse response = new ApiResponse();
				response.setCode(Integer.toString(JsonWorker.RESULT_CODE_NETWORK_ERROR));
				listener.onError(response);
			}
		}
	}
}