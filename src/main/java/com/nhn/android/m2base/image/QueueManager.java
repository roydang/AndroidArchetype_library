package com.nhn.android.m2base.image;

import java.util.concurrent.BlockingQueue;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;

public class QueueManager {
	private static BlockingQueue<AsyncTask<Void, Void, Bitmap>> queue;
	
	private static BlockingQueue<AsyncTask<Void, Void, Bitmap>> createQueue() {
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
				return new java.util.concurrent.LinkedBlockingDeque<AsyncTask<Void, Void, Bitmap>>();
			}
		} catch (Exception e) {
		} catch (Error err) {
		}
		
		return new java.util.concurrent.LinkedBlockingQueue<AsyncTask<Void, Void, Bitmap>>();
	}
	
	public static BlockingQueue<AsyncTask<Void, Void, Bitmap>> getQueue() {
		if (queue == null) {
			queue = createQueue();
		}
		
		return queue;
	}
	
	public static AsyncTask<Void, Void, Bitmap> take() throws InterruptedException {
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
				return ((java.util.concurrent.LinkedBlockingDeque<AsyncTask<Void, Void, Bitmap>>) getQueue()).takeFirst();
			}
		} catch (InterruptedException ie) {
			throw ie;
		} catch (Exception e) {
		} catch (Error err) {
		}
		
		return getQueue().take();
	}
	
	public static void put(AsyncTask<Void, Void, Bitmap> task) throws InterruptedException {
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
				((java.util.concurrent.LinkedBlockingDeque<AsyncTask<Void, Void, Bitmap>>) getQueue()).putFirst(task);
				return;
			}
		} catch (Exception e) {
		} catch (Error err) {
		}
		
		getQueue().put(task);
	}
	
	public static void add(AsyncTask<Void, Void, Bitmap> task) {
		getQueue().add(task);
	}
	
	public static void clear() {
		getQueue().clear();
	}
}
