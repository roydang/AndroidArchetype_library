/*
 * @(#)Worker.java $$version ${date}
 *
 * Copyright 2007 NHN Corp. All rights Reserved.
 * NHN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.nhn.android.archetype.base.worker;

import android.os.Handler;

import com.nhn.android.archetype.base.BaseApplication;
import com.nhn.android.archetype.base.util.internal.M2baseLogger;

/**
 * Thread 로직의 알고리즘을 정의한다.
 * @author nhn
 *
 */
public abstract class Worker implements Runnable {
	private static M2baseLogger logger = M2baseLogger.getLogger(Worker.class);

	/**
	 * 취소 Flag
	 */
	private boolean canceled = false;
	private boolean completed = false;
	private boolean started = false;

	/**
	 * '작업 중' 메시지를 전달한다.
	 */
	private Runnable working;
	/**
	 * '작업 완료' 메시지를 전달한다.
	 */
	private Runnable workComplete;

	private WorkComplete currentRunner;

	/**
	 * Runnable - run()
	 */
	public void run() {
		started = true;
		canceled = false;
		completed = false;
		logger.d("=================== < doWork > =================");
		doWork();
		completed = true;
		started = false;
		logger.d("=================== < endWork > =================");
		endWork();
	}

	/**
	 * Thread의 작업.
	 */
	abstract protected void doWork();

	/**
	 * endWork후 후처리 알고리즘.
	 * Main Thread에서 실행된다.
	 */
	protected void endWorkComplete() {
		//do something
	}

	/**
	 * 작업 중간중간 처리할 때 호출한다.
	 * Main Thread로 메시지를 보낸다.
	 */
	public void doingWork() {
		final Handler handler = BaseApplication._internalInstance.getHandler();
		if (working != null && handler != null) {
			handler.post(working);
		}
	}

	/**
	 * 작업 완료시 호출한다.
	 * Main Thread로 메시지를 보낸다.
	 */
	public void endWork() {
		if (BaseApplication._internalInstance != null) {
			final Handler handler = BaseApplication._internalInstance.getHandler();

			if (workComplete != null && handler != null) {
				currentRunner = new WorkComplete(this, workComplete);
				handler.post(currentRunner);
			}
		}
	}

	/**
	 * 취소 Flag를 셋팅한다.
	 * 되도록 빨리 작업이 종료되도록 로직을 구성한다.
	 */
	public void cancel() {
		canceled = true;
	}

	public void cancel(boolean forceKill) {
		canceled = true;
	}

	/**
	 *
	 * @return
	 */
	public boolean isCanceled() {
		return canceled;
	}

	/**
	 *
	 * @return
	 */
	public boolean isCompleted() {
		return completed;
	}

	/**
	 *
	 * @return
	 */
	public boolean isStarted() {
		return started;
	}

	/**
	 *
	 * @param workComplete
	 */
	public void setWorkComplete(Runnable workComplete) {
		this.workComplete = workComplete;
	}

	/**
	 *
	 * @param working
	 */
	public void setWorking(Runnable working) {
		this.working = working;
	}

	/**
	 * ThreadPool에 작업을 등록한다.
	 */
	public void post() {
		BaseApplication._internalInstance.addWorker(this);
	}
	
	public void postStats() {
		BaseApplication._internalInstance.addStatsWorker(this);
	}
	
	protected Handler getHandler() {
		return BaseApplication._internalInstance.getHandler();
	}

	/**
	 * doWork 작업 후 endWorkComplete를 호출하기 위한 Wrappter Class
	 * @author telltale
	 *
	 */
	static class WorkComplete implements Runnable {
		Runnable originRunnable;
		Worker worker;

		/**
		 *
		 * @param worker
		 * @param runnable
		 */
		public WorkComplete(Worker worker, Runnable runnable) {
			this.originRunnable = runnable;
			this.worker = worker;
		}

		/**
		 *
		 */
		public void run() {
			if (this.originRunnable != null) {
				this.originRunnable.run();
			}
			if (this.worker != null) {
				this.worker.endWorkComplete();
			}
		}
	}
}
