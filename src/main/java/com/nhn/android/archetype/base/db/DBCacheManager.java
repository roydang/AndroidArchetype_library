package com.nhn.android.archetype.base.db;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import android.os.AsyncTask;

import com.nhn.android.archetype.base.AABaseApplication;
import com.nhn.android.archetype.base.db.annotation.Column;
import com.nhn.android.archetype.base.db.annotation.Table;
import com.nhn.android.archetype.base.db.helper.M2SQLiteOpenHelper;
import com.nhn.android.archetype.base.db.listener.DBCacheListener;
import com.nhn.android.archetype.base.object.BaseObj;
import com.nhn.android.archetype.base.util.internal.M2baseLogger;
import com.nhn.android.archetype.base.util.internal.M2baseUtility;

public class DBCacheManager {
	private static M2baseLogger logger = M2baseLogger.getLogger(DBCacheManager.class);
	
	private M2SQLiteOpenHelper dbHelper;
	private Class<? extends BaseObj> targetClass;
	
	public DBCacheManager(Class<? extends BaseObj> targetClass) {
		this.targetClass = targetClass;
	}
	
	private boolean open() {
		BaseObj headObj = null;
		
		try { 
			headObj = targetClass.newInstance();
		} catch (Exception e) {
		}
		
		if (headObj == null) {
			return false;
		}
		
		Table tableInfo = null;
		LinkedHashMap<String, ColumnData> columnList = null;
		
		Class<? extends BaseObj> clazz = headObj.getClass();
		if (clazz.isAnnotationPresent(Table.class)) {
			tableInfo = clazz.getAnnotation(Table.class);
		}
		
		if (tableInfo != null) {
			columnList = new LinkedHashMap<String, ColumnData>();
			
			Method[] methods = clazz.getMethods();
			if (methods != null && methods.length > 0) {
				for (Method m : methods) {
					if (m.isAnnotationPresent(Column.class)) {
						Column column = m.getAnnotation(Column.class);
						
						try {
							Object ret = m.invoke(headObj);
							if (ret != null) {
								ColumnData data = new ColumnData();
								data.column = column;
								data.clazz = ret.getClass();
								columnList.put(column.name(), data);
							}
						} catch (Exception e) {
							logger.e(e);
						}
					}
				}
			}
			
			logger.d("table: %s key: %s", tableInfo.name(), tableInfo.key());
			for (Entry<String, ColumnData> columnEntry : columnList.entrySet()) {
				logger.d("column: %s targetClass: %s", columnEntry.getKey(), columnEntry.getValue().toString());
			}
		}

		close();
		dbHelper = new M2SQLiteOpenHelper(targetClass, tableInfo, columnList);
		if (dbHelper != null) {
			return true;
		}
		
		return false;
	}
	
	@Override
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}
	
	protected void close() {
		if (dbHelper != null) {
			try {
				dbHelper.close();	
			} catch (Exception e) {
				logger.e(e);
			}
			
			dbHelper = null;
		}
	}
	
	public void insert(BaseObj obj) {
		insert(obj, null);
	}
	
	public void insert(BaseObj obj, String tag) {
		List<BaseObj> objList = new ArrayList<BaseObj>();
		objList.add(obj);
		
		insert(objList, tag);
	}
	
	public void insertSync(BaseObj obj) {
		insertSync(obj, null);
	}
	
	public void insertSync(BaseObj obj, String tag) {
		List<BaseObj> objList = new ArrayList<BaseObj>();
		objList.add(obj);
		
		insertSync(objList, tag);
	}
	
	public void insert(final List<? extends BaseObj> objList) {
		insert(objList, null);
	}
	
	public void insert(final List<? extends BaseObj> objList, final String tag) {
		AABaseApplication._internalInstance.getBackgroundHandler().post(new Runnable() {
			@Override
			public void run() {
				InsertTask task = new InsertTask(DBCacheManager.this, objList, tag);
				
				if (M2baseUtility.isICSCompatibility()) {
					task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				} else {
					task.execute();
				}
			}
		});
	}
	
	public void insertSync(List<? extends BaseObj> objList) {
		insertSync(objList, null);
	}
	
	public void insertSync(List<? extends BaseObj> objList, String tag) {
		logger.d("insert: %s", objList);
		try {
			if(open()) {
				dbHelper.insert(objList, tag);
			}
		} catch (Exception e) {
			logger.e(e);
		} finally {
			close();
		}
	}
	
	public void select(final String whereCause, final DBCacheListener listener) {
		AABaseApplication._internalInstance.getBackgroundHandler().post(new Runnable() {
			@Override
			public void run() {
				SelectTask task = new SelectTask(DBCacheManager.this, whereCause, listener);
				
				if (M2baseUtility.isICSCompatibility()) {
					task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				} else {
					task.execute();
				}
			}
		});
	}
	
	public List<BaseObj> selectSync(String whereCause) {
		try {
			if (open()) {
				return dbHelper.select(whereCause);
			}
		} catch (Exception e) {
			logger.e(e);
		} finally {
			close();
		}
		
		return null;
	}
	
	public void delete(final String whereCause, final DBCacheListener listener) {
	}
	
	public void deleteSync(String whereCause) {
	}
	
	private static class InsertTask extends AsyncTask<Void, Void, Void> {
		private DBCacheManager mgr;
		private List<? extends BaseObj> items;
		private String tag;
		
		public InsertTask(DBCacheManager mgr, List<? extends BaseObj> items, String tag) {
			this.mgr = mgr;
			this.items = items;
			this.tag = tag;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			mgr.insertSync(items, tag);
			return null;
		}
	}
	
	private static class SelectTask extends AsyncTask<Void, Void, List<BaseObj>> {
		private DBCacheManager mgr;
		private String whereCause;
		private DBCacheListener listener;
		
		public SelectTask(DBCacheManager mgr, String whereCause, DBCacheListener listener) {
			this.mgr = mgr;
			this.whereCause = whereCause;
			this.listener = listener;
		}
		
		@Override
		protected List<BaseObj> doInBackground(Void... params) {
			return mgr.selectSync(whereCause);
		}

		@Override
		protected void onPostExecute(List<BaseObj> result) {
			if (result != null) {
				listener.onSuccess(result);
			} else {
				listener.onError();
			}
		}
	}
	
	public static class ColumnData {
		public Column column;
		public Class clazz;
		
		public String getType() {
			if (clazz.equals(Integer.class) || clazz.equals(Long.class)) {
				return "INTEGER";
			}

			return "TEXT";
		}
	}
}
