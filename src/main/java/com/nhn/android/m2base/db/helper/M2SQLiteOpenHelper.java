package com.nhn.android.m2base.db.helper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import com.nhn.android.m2base.BaseApplication;
import com.nhn.android.m2base.db.DBCacheManager.ColumnData;
import com.nhn.android.m2base.db.annotation.Column;
import com.nhn.android.m2base.db.annotation.Table;
import com.nhn.android.m2base.object.BaseObj;
import com.nhn.android.m2base.util.internal.M2baseLogger;
import com.nhn.android.m2base.util.internal.M2baseUtility;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

public class M2SQLiteOpenHelper extends SQLiteOpenHelper {
	private static M2baseLogger logger = M2baseLogger.getLogger(M2SQLiteOpenHelper.class);
	
	private Class<? extends BaseObj> targetClass;
	private Table tableInfo;
	private LinkedHashMap<String, ColumnData> columnList;
	//private SQLiteDatabase db;

	public M2SQLiteOpenHelper(Class<? extends BaseObj> clazz, Table tableInfo, LinkedHashMap<String, ColumnData> columnList) {
		super(BaseApplication._internalInstance, tableInfo.name(), null, tableInfo.version());

		this.targetClass = clazz;
		this.tableInfo = tableInfo;
		this.columnList = columnList;
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		StringBuilder sb = new StringBuilder();
		
		if (columnList != null) {
			for (Entry<String, ColumnData> columnEntry : columnList.entrySet()) {
				String type = columnEntry.getValue().getType();
				if (M2baseUtility.isNotNullOrEmpty(type)) {
					sb.append(String.format(", %s %s", columnEntry.getKey(), columnEntry.getValue().getType()));
				}
			}
		}
		
		String query = String.format("CREATE TABLE %s (_id INTEGER PRIMARY KEY AUTOINCREMENT, _key TEXT NOT NULL, _json TEXT NOT NULL, _tag TEXT, _timespan LONG NOT NULL %s);", 
				tableInfo.name(), sb.toString());
		logger.d("query: %s", query);
		db.execSQL(query);
		
		query = String.format("CREATE INDEX %s_key ON %s(_key)", tableInfo.name(), tableInfo.name());
		logger.d("query: %s", query);
		db.execSQL(query);
		
		query = String.format("CREATE INDEX %s_tag ON %s(_tag)", tableInfo.name(), tableInfo.name());
		logger.d("query: %s", query);
		db.execSQL(query);
		
		if (columnList != null) {
			for (Entry<String, ColumnData> columnEntry : columnList.entrySet()) {
				Column column = columnEntry.getValue().column;
				if (column.index()) {
					query = String.format("CREATE INDEX %s_%s ON %s(%s)", tableInfo.name(), column.name(), tableInfo.name(), column.name());
					logger.d("query: %s", query);
					db.execSQL(query);
				}
			}
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL(String.format("DROP TABLE IF EXISTS %s", tableInfo.name()));
		onCreate(db);
	}
	
	private List<DBData> fetchDataList(List<? extends BaseObj> objList) {
		List<DBData> dataList = new ArrayList<DBData>();
		
		for (BaseObj obj : objList) {
			DBData data = new DBData();
			dataList.add(data);
			
			data.key = obj.getString(tableInfo.key());
			data.json = obj.toJson();

			for (Entry<String, ColumnData> columnEntry : columnList.entrySet()) {
				if (columnEntry.getValue().equals(Integer.class) || columnEntry.getValue().equals(Long.class)) {
					data.dataList.put(columnEntry.getKey(), new Long(obj.getLong(columnEntry.getKey())));
				} else {
					data.dataList.put(columnEntry.getKey(), obj.getString(columnEntry.getKey()));
				}
			}
		}
		
		return dataList;
	}
	
	public void insert(List<? extends BaseObj> objList, String tag) {
		if (objList == null || objList.size() == 0) {
			return;
		}
		
		SQLiteDatabase db = null;
		
		try {
			db = getWritableDatabase();
			db.beginTransaction();
			
			List<DBData> fetchList = fetchDataList(objList);
			if (fetchList != null && fetchList.size() > 0) {
				// delete old datas
				/*
				StringBuilder sb = new StringBuilder();
				for (int i=0;i<fetchList.size()-1;i++) {
					sb.append(String.format("'%s',", fetchList.get(i).key));
				}
				
				sb.append(String.format("'%s'", fetchList.get(fetchList.size()-1).key));
				
				String deleteQuery = String.format("DELETE FROM %s WHERE _key IN (%s)", tableInfo.name(), sb.toString());
				logger.d("Query: %s", deleteQuery);
				db.execSQL(deleteQuery);
				*/
				
				// insert new datas
				StringBuilder sb = new StringBuilder();
				StringBuilder valuesQuestionMark = new StringBuilder();
				if (columnList != null) {
					for (Entry<String, ColumnData> columnEntry : columnList.entrySet()) {
						String type = columnEntry.getValue().getType();
						if (M2baseUtility.isNotNullOrEmpty(type)) {
							sb.append(String.format(", %s", columnEntry.getKey()));
							valuesQuestionMark.append(",?");
						}
					}
				}
				
				String insertQuery = String.format("REPLACE INTO %s (_key, _json, _tag, _timespan%s) VALUES (?,?,?,?%s)", tableInfo.name(), sb.toString(), valuesQuestionMark.toString());
				logger.d("Query: %s", insertQuery);
				
				SQLiteStatement insertStat = db.compileStatement(insertQuery);
				
				long time = System.currentTimeMillis();
				for (DBData data : fetchList) {
					int idx = 1;
					insertStat.bindString(idx++, data.key);
					insertStat.bindString(idx++, data.json);
					
					if (tag == null) {
						insertStat.bindNull(idx++);
					} else {
						insertStat.bindString(idx++, tag);
					}
					
					insertStat.bindLong(idx++, time);
					
					for (Entry<String, Object> entry : data.dataList.entrySet()) {
						Object value = entry.getValue();
						
						if (value == null) {
							insertStat.bindNull(idx++);
							continue;
						}
						
						if (value instanceof Long) {
							insertStat.bindLong(idx++, (Long) value);
						} else {
							insertStat.bindString(idx++, value.toString());
						}
					}
					
					insertStat.executeInsert();
				}
			}
			
			db.setTransactionSuccessful();
		} catch (Exception e) {
			logger.e(e);
		} finally {
			if (db != null) {
				db.endTransaction();
				
				db.close();
				db = null;
			}
		}
	}
	
	public List<BaseObj> select(String whereCause) {
		SQLiteDatabase db = null;
		Cursor cursor = null;
		
		try {
			db = getReadableDatabase();
			
			if (M2baseUtility.isNullOrEmpty(whereCause)) {
				whereCause = "";
			}
			
			String query = String.format("SELECT * FROM %s %s", tableInfo.name(), whereCause);
			logger.d("Query: %s", query);
			cursor = db.rawQuery(query, null);
			
			List<BaseObj> ret = new ArrayList<BaseObj>();
			
			while (cursor.moveToNext()) {
				DBData data = new DBData();
				data.key = cursor.getString(1);
				data.json = cursor.getString(2);
				
				try {
					BaseObj obj = BaseObj.parse(data.json);
					ret.add(obj.as(targetClass));
				} catch (Exception e) {
				}
			}
			
			return ret;
		} catch (Exception e) {
			logger.e(e);
		} finally {
			if (cursor != null) {
				cursor.close();
				cursor = null;
			}
			
			if (db != null) {
				db.close();
				db = null;
			}
		}
		
		return null;
	}
	
	private class DBData {
		public String key;
		public String json;
		public LinkedHashMap<String, Object> dataList = new LinkedHashMap<String, Object>();
	}
}
