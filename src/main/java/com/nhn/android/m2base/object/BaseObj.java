/*
 * @(#)ItemObj.java $$version ${date}
 *
 * Copyright 2007 NHN Corp. All rights Reserved.
 * NHN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.nhn.android.m2base.object;

import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;

import android.graphics.drawable.Drawable;

import com.nhn.android.m2base.util.internal.M2baseLogger;
import com.nhn.android.m2base.util.internal.M2baseUtility;

@SuppressWarnings("serial")
public class BaseObj implements Serializable {
	private static M2baseLogger logger = M2baseLogger.getLogger(BaseObj.class);

	private static final String CODE = "code";
	private static final String MESSAGE = "message";

	private Map<String, Object> dataMap;
	private Map<String, Object> objectCacheMap;

	public BaseObj() {
	}

	public BaseObj(BaseObj baseObj) {
		this(baseObj.getDataMap());
	}

	public BaseObj(Map<String, Object> map) {
		setDataMap(map);
	}

	public Map<String, Object> getDataMap() {
		if (dataMap == null) {
			dataMap = new LinkedHashMap<String, Object>();
		}

		return dataMap;
	}

	public void setDataMap(Map<String, Object> dataMap) {
		this.dataMap = dataMap;
		getObjectCacheMap().clear();
	}

	protected Map<String, Object> getObjectCacheMap() {
		if (objectCacheMap == null) {
			objectCacheMap = new HashMap<String, Object>();
		}

		return objectCacheMap;
	}

	protected void setObjectCacheMap(Map<String, Object> objectCacheMap) {
		this.objectCacheMap = objectCacheMap;
	}

	public boolean contains(String key) {
		return getDataMap().containsKey(key);
	}

	public void remove(String key) {
		if (getDataMap().containsKey(key)) {
			getDataMap().remove(key);
		}

		if (getObjectCacheMap().containsKey(key)) {
			getObjectCacheMap().remove(key);
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void put(String key, Object value) {
		if (value == null) {
			if (getDataMap().containsKey(key)) {
				getDataMap().remove(key);
			}

			if (getObjectCacheMap().containsKey(key)) {
				getObjectCacheMap().remove(key);
			}

			return;
		}

		Map<String, Object> currentMap = getDataMap();
		String currentKey = key;
		boolean putCacheMap = true;

		if (key.indexOf(".") > 0) {
			putCacheMap = false;
			String[] keys = key.split("\\.");

			for (int i = 0; i < keys.length - 1; i++) {
				if (!currentMap.containsKey(keys[i])) {
					return;
				}

				currentMap = (Map<String, Object>) currentMap.get(keys[i]);
			}

			currentKey = keys[keys.length - 1];
		}

		if (value instanceof BaseObj) {
			BaseObj obj = (BaseObj) value;
			currentMap.put(currentKey, obj.getDataMap());

			if (putCacheMap) {
				getObjectCacheMap().put(currentKey, obj);
			}
		} else if (value instanceof List) {
			List list = (List) value;
			if (list.size() > 0) {
				Object item = list.get(0); // 0번아이템의 타입보고 결정. BaseObj라면 object
											// cache에도 추가해야 함.
				if (item instanceof BaseObj) {
					if (putCacheMap) {
						getObjectCacheMap().put(currentKey, value);
					}

					List<Map<String, Object>> dataList = new ArrayList<Map<String, Object>>();
					for (int i = 0; i < list.size(); i++) {
						dataList.add(((BaseObj) list.get(i)).getDataMap());
					}

					currentMap.put(currentKey, dataList);
				} else {
					currentMap.put(currentKey, value);
				}
			} else {
				if (currentMap.containsKey(currentKey)) {
					currentMap.remove(currentKey);
				}

				if (putCacheMap) {
					if (getObjectCacheMap().containsKey(key)) {
						getObjectCacheMap().remove(key);
					}
				}
			}
		} else if (value instanceof Drawable) {
			getObjectCacheMap().put(currentKey, value);
		} else {
			currentMap.put(currentKey, value);
		}
	}

	public Object get(String key) {
		return get(key, null);
	}

	public Object get(String key, Object defaultVale) {
		Map currentMap = getDataMap();
		String currentKey = key;

		if (key.indexOf(".") > 0) {
			String[] keys = key.split("\\.");

			for (int i = 0; i < keys.length - 1; i++) {
				if (!currentMap.containsKey(keys[i])) {
					return defaultVale;
				}

				currentMap = (Map) currentMap.get(keys[i]);
			}

			currentKey = keys[keys.length - 1];
		}

		if (currentMap.containsKey(currentKey)) {
			return currentMap.get(currentKey);
		}

		return defaultVale;
	}

	public int getInt(String key) {
		return getInt(key, 0);
	}

	public int getInt(String key, int defaultValue) {
		if (contains(key)) {
			Object obj = get(key);

			if (obj instanceof Integer) {
				return (Integer) obj;
			}

			if (obj instanceof Long) {
				return ((Long) obj).intValue();
			}

			if (obj instanceof String) {
				try {
					return Integer.parseInt((String) obj);
				} catch (Exception e) {
				}
			}
		}
		return defaultValue;
	}

	public long getLong(String key) {
		return getLong(key, 0);
	}

	public long getLong(String key, long defaultValue) {
		if (contains(key)) {
			Object obj = get(key);

			if (obj instanceof Long) {
				return (Long) obj;
			}

			if (obj instanceof Integer) {
				return (Integer) obj;
			}

			if (obj instanceof String) {
				try {
					return Long.parseLong((String) obj);
				} catch (Exception e) {
				}
			}
		}

		return defaultValue;
	}

	public float getFloat(String key) {
		return getFloat(key, 0);
	}

	public float getFloat(String key, float defaultValue) {
		if (contains(key)) {
			Object obj = get(key);

			if (obj instanceof Float) {
				return (Float) obj;
			}

			if (obj instanceof String) {
				try {
					return Float.parseFloat((String) obj);
				} catch (Exception e) {
				}
			}
		}

		return defaultValue;
	}

	public String getString(String key) {
		return getString(key, null);
	}

	public String getString(String key, String defaultValue) {
		Object obj = get(key);
		if (obj != null) {
			return obj.toString();
		}

		return defaultValue;
	}

	public boolean getBoolean(String key) {
		return getBoolean(key, false);
	}

	public boolean getBoolean(String key, boolean defaultValue) {
		if (contains(key)) {
			Object obj = get(key);

			if (obj instanceof Boolean) {
				return (Boolean) obj;
			}

			if (obj instanceof String) {
				try {
					return Boolean.parseBoolean((String) obj);
				} catch (Exception e) {
				}
			}
		}

		return defaultValue;
	}
	
	public Drawable getDrawable(String key) {
		return getDrawable(key, null);
	}

	public Drawable getDrawable(String key, Drawable defaultValue) {
		if (objectCacheMap.containsKey(key)) {
			Object obj = objectCacheMap.get(key);
			
			if (obj instanceof Drawable) {
				return (Drawable) obj;
			}
		}
		return defaultValue;
	}

	@SuppressWarnings("rawtypes")
	public List getList(String key) {
		if (contains(key)) {
			Object obj = get(key);

			if (obj instanceof List) {
				return (List) obj;
			}
		}

		return new ArrayList();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<?> getList(String key, Class<? extends BaseObj> clazz) {
		List list = getList(key);

		if (list == null) {
			return null;
		}

		List retList = null;

		if (contains(key) && getObjectCacheMap().containsKey(key)) {
			try {
				retList = (List) getObjectCacheMap().get(key);
			} catch (Exception e) {
				logger.e(e);

				retList = null;
			}
		}

		if (retList == null) {
			retList = new ArrayList();
			for (Object obj : list) {
				if (obj instanceof Map) {
					BaseObj inst;
					try {
						inst = clazz.newInstance();
						inst.setDataMap((Map<String, Object>) obj);

						retList.add(inst);
					} catch (Exception e) {
						logger.e(e);
					}
				}
			}

			getObjectCacheMap().put(key, retList);
		}

		return retList;
	}

	@SuppressWarnings("rawtypes")
	public Map getMap(String key) {
		if (contains(key)) {
			Object obj = get(key);

			if (obj instanceof Map) {
				return (Map) obj;
			}
		}

		return null;
	}

	public <T extends BaseObj> T getBaseObj(String key, Class<? extends BaseObj> clazz) {
		return getBaseObj(key, clazz, true);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T extends BaseObj> T getBaseObj(String key, Class<? extends BaseObj> clazz, boolean createInstanceIfNotExists) {
		Map map = getMap(key);

		T inst = null;

		if (getObjectCacheMap().containsKey(key)) {
			try {
				inst = (T) getObjectCacheMap().get(key);
			} catch (Exception e) {
				logger.e(e);
				inst = null;
			}
		}

		if (map != null && inst == null) {
			try {
				inst = (T) clazz.newInstance();
				inst.setDataMap(map);
				getObjectCacheMap().put(key, inst);
			} catch (Exception e) {
				logger.e(e);
			}
		}

		if (inst == null && createInstanceIfNotExists) {
			try {
				inst = (T) clazz.newInstance();
				put(key, inst);
			} catch (Exception e) {
				logger.e(e);
			}
		}

		return inst;
	}

	public boolean hasError() {
		if (getDataMap().containsKey(CODE) && getDataMap().containsKey(MESSAGE)) {
			return true;
		}

		return false;
	}

	public <T extends BaseObj> T as(Class<T> clazz) {
		if (getClass().equals(clazz)) {
			return (T) this;
		}

		try {
			T obj = (T) clazz.newInstance();
			obj.setDataMap(getDataMap());

			return obj;
		} catch (Exception e) {
			return null;
		}
	}

	public ApiResponse asApiResponse() {
		ApiResponse response = new ApiResponse();
		response.setDataMap(getDataMap());

		return response;
	}

	public String toJson() {
		try {
			StringWriter out = new StringWriter();
			JSONValue.writeJSONString(getDataMap(), out);
			return out.toString();
		} catch (Exception e) {
			logger.e(e);
		}

		return null;
	}

	@Override
	public String toString() {
		String json = toJson();

		if (json != null) {
			return json;
		}

		return super.toString();
	}

	// factory methods
	public static BaseObj parse(InputStream is) {
		return parse(is, BaseObj.class);
	}

	public static <T extends BaseObj> T parse(InputStream is, Class<? extends BaseObj> clazz) {
		return parse(M2baseUtility.convertStreamToString(is), clazz);
	}

	public static BaseObj parse(String json) {
		return parse(json, BaseObj.class);
	}

	@SuppressWarnings("unchecked")
	public static <T extends BaseObj> T parse(String json, Class<? extends BaseObj> clazz) {
		T item = null;

		if (json == null) {
			logger.w("parse(), json is null");
			return null;
		}

		json = json.trim();
		if (json.startsWith("[") && json.endsWith("]")) {
			json = String.format("{\"data\" : %s}", json);
		}

		try {
			item = (T) clazz.newInstance();
		} catch (Exception e) {
			logger.e(e);
			return null;
		}

		Map<String, Object> dataMap = null;

		try {
			JSONParser parser = new JSONParser();
			dataMap = (Map) parser.parse(json);
		} catch (Exception e) {
			logger.e(e);
			
			dataMap = new LinkedHashMap<String, Object>();
			dataMap.put(CODE, -1);
			dataMap.put(MESSAGE, "Unexpected Error");
		}

		item.setDataMap(dataMap);

		return item;
	}

	public static <T extends BaseObj> T parse(URL url, Class<? extends BaseObj> clazz) throws Exception {
		InputStream is = url.openStream();

		try {
			return parse(is, clazz);
		} catch (Exception e) {
			throw e;
		} finally {
			try {
				if (is != null) {
					is.close();
					is = null;
				}
			} catch (Exception e) {
				logger.e(e);
			}
		}
	}
}
