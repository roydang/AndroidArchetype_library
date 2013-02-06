package com.nhn.android.archetype.base.image;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.RejectedExecutionException;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

import com.nhn.android.archetype.base.image.ImageHelper.MaskData;
import com.nhn.android.archetype.base.object.ApiResponse;
import com.nhn.android.archetype.base.util.internal.M2baseLogger;
import com.nhn.android.archetype.base.util.internal.M2baseUtility;
import com.nhn.android.archetype.base.worker.listener.ApiRequestListener;

public class ImageLoadManager implements Runnable {
	private static final int MAX_BALANCER_THREAD = 1;
	
	private static M2baseLogger logger = M2baseLogger.getLogger(ImageLoadManager.class);
	private static ImageLoadManager instance = new ImageLoadManager();

	private Map<String, ImageTaskData> taskDataMap = null;
	
	private Thread[] threads;
	
	private ImageLoadManager() {
	}
	
	private synchronized static ImageLoadManager getInstance() {
		if (instance == null) {
			instance = new ImageLoadManager();
		}
		
		return instance;
	}

	private Map<String, ImageTaskData> getTaskDataMap() {
		if (taskDataMap == null) {
			taskDataMap = Collections.synchronizedMap(new HashMap<String, ImageTaskData>());
		}
		
		return taskDataMap;
	}
	
	public static void clearTask(String url, Object maskImageKey) {
		String key = ImageHelper.getCacheKey(url, maskImageKey);
		
		ImageLoadManager instance = getInstance();
		instance.getTaskDataMap().remove(key);
	}
	
	public static synchronized void stopThread() {
		if (instance == null || instance.threads == null) {
			return;
		}
		
		for (Thread t : instance.threads) {
			try {
				if (t != null && t.isAlive()) {
					t.interrupt();
				}
			} catch (Exception e) {
				logger.e(e);
			} catch (Error e) {
				logger.e(e);
			}
		}
		
		instance.threads = null;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static synchronized void cancelRequest() {
		logger.d("cancelRequest");
		
		Map<String, ImageTaskData> dataMap = getInstance().getTaskDataMap();
		
		try {
			ApiResponse response = new ApiResponse();
			response.setCode("500");
			response.setMessage("cancelrequest");
			
			for (Entry<String, ImageTaskData> entry : dataMap.entrySet()) {
				try {
					ImageTaskData imageTaskData = entry.getValue();
					AsyncTask<Void, Void, Bitmap> taskData = imageTaskData.getTask();

					if (taskData instanceof ImageLoadTask) {
						((ImageLoadTask) taskData).disconnect();
					}
					
					for (ApiRequestListener listener : imageTaskData.getListener()) {
						try {
							listener.onError(response);
						} catch (Exception e) {
						}
					}
				} catch (Exception ex) {
				} catch (Error err) {
				}
			}
		} catch (Exception excep) {
		} catch (Error error) {
		} finally {
			QueueManager.clear();
			dataMap.clear();	
		}
	}
	
	public synchronized static void push(String url, boolean force, int sampleWidth, MaskData maskData, boolean downloadOnly, ApiRequestListener<Bitmap, ApiResponse> listener) {
		String key = ImageHelper.getCacheKey(url, maskData);

		ImageLoadManager instance = getInstance();
		Map<String, ImageTaskData> taskDataMap = instance.getTaskDataMap();
		ImageTaskData taskData = null;
		
		if (instance.getTaskDataMap().containsKey(key)) {
			taskData = taskDataMap.get(key);
			
			if (force) {
				AsyncTask<Void, Void, Bitmap> task = taskData.getTask();
				try {
					if (task instanceof ImageLoadTask) {
						((ImageLoadTask) task).disconnect();
					}
				} catch (Exception e) {
				}
				
				if (taskData.getMaskData() == null) {
					task = new ImageLoadTask(taskData);
				} else {
					task = new MaskedImageLoadTask(taskData);
				}
				
				taskData.setTask(task);
				taskData.getListener().add(listener);
				
				taskDataMap.put(key, taskData);
				return;
			} else {
				taskData.getListener().add(listener);
				return;
			}
		}

		taskData = new ImageTaskData(url, sampleWidth, maskData, downloadOnly);
		AsyncTask<Void, Void, Bitmap> task = null;
		
		if (taskData.getMaskData() == null) {
			task = new ImageLoadTask(taskData);
		} else {
			task = new MaskedImageLoadTask(taskData);
		}
		
		taskData.setTask(task);	
		taskData.getListener().add(listener);
		taskDataMap.put(key, taskData);
		
		try {
			QueueManager.put(task);
		} catch (Exception e) {
			logger.e(e);
			QueueManager.add(task);
		}
		
		if (instance.threads == null) {
			instance.threads = new Thread[MAX_BALANCER_THREAD];
		}
		
		for (int i=0;i<instance.threads.length;i++) {
			if (instance.threads[i] == null || instance.threads[i].isAlive() == false) {
				instance.threads[i] = new Thread(instance);
				instance.threads[i].start();
			}
		}
	}
	
	public void start() {
		new Thread(this).start();
	}

	@SuppressLint("NewApi")
	public void run() {
		boolean isOverHoneycomb = M2baseUtility.isICSCompatibility();
		
		while (true) {
			AsyncTask<Void, Void, Bitmap> task = null; 
			
			try {
				task = QueueManager.take();	
				if (isOverHoneycomb) {
					task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				} else {
					task.execute();
				}
			} catch (InterruptedException ie) {
			} catch (RejectedExecutionException ree) {
				if (task != null) {
					QueueManager.add(task);
				}
			} catch (Exception e) {
				logger.e(e);
			}
			try {
				Thread.sleep(new Random().nextInt(30));
			} catch (InterruptedException e) {
			}
		}
	}
}
