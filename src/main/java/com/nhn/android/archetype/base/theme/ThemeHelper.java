package com.nhn.android.archetype.base.theme;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.nhn.android.archetype.base.AABaseApplication;
import com.nhn.android.archetype.base.util.internal.M2baseLogger;
import com.nhn.android.archetype.base.util.internal.M2baseUtility;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class ThemeHelper {
	private static M2baseLogger logger = M2baseLogger.getLogger(ThemeHelper.class);
	
	private static WeakReference<Resources> currentThemeResources;
	private static SoftReference<Map<Integer, Integer>> currentThemeIdMap;
	
	public final static boolean isThemeInstalled() {
		return M2baseUtility.isNotNullOrEmpty(AABaseApplication._internalInstance.getSelectedThemePackageName());
	}
	
	public final static synchronized void updateTheme() {
		currentThemeResources = null;
		currentThemeIdMap = null;
		
		AABaseApplication application = AABaseApplication._internalInstance;
		
		try {
			String themePackageName = application.getSelectedThemePackageName();
			if (M2baseUtility.isNotNullOrEmpty(themePackageName)) {
				PackageManager pm = application.getPackageManager();
				Resources themeRes = pm.getResourcesForApplication(themePackageName);
				
				if (themeRes != null) {
					currentThemeResources = new WeakReference<Resources>(themeRes);
				}
			}
		} catch (Exception e) {
			logger.e(e);
			e.printStackTrace();
		}
	}
	
	public final static void getInstalledThemeList(final String permission, GetThemeListListener listener) {
		GetThemeListTask task = new GetThemeListTask(null, new ThemePackageFilter() {
			@Override
			public boolean isValid(PackageManager packageManager, ApplicationInfo info) {
				try {
					PackageInfo packageInfo = packageManager.getPackageInfo(info.packageName, PackageManager.GET_PERMISSIONS);
					if (packageInfo.permissions != null && packageInfo.permissions.length == 1) {
						return permission.equals(packageInfo.permissions[0].name);
					}
				} catch (Exception e) {
				}
				
				return false;
			}
		}, listener);
		if (M2baseUtility.isICSCompatibility()) {
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			task.execute();
		}
	}
	
	public final static void getInstalledThemeList(ThemePackageFilter filter, GetThemeListListener listener) {
		GetThemeListTask task = new GetThemeListTask(null, filter, listener);
		if (M2baseUtility.isICSCompatibility()) {
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			task.execute();
		}
	}
	
	public final static List<String> getInstalledThemeListSync(String permission) {
		AABaseApplication application = AABaseApplication._internalInstance;
		
		List<String> themeList = new ArrayList<String>();
		PackageManager packageManager = application.getPackageManager();
		List<ApplicationInfo> installedApplications = packageManager.getInstalledApplications(0);
		
		for (ApplicationInfo info : installedApplications) {
			try {
				PackageInfo packageInfo = packageManager.getPackageInfo(info.packageName, PackageManager.GET_PERMISSIONS);
				if (packageInfo.permissions != null && packageInfo.permissions.length == 1) {
					if (permission.equals(packageInfo.permissions[0])) {
						themeList.add(info.packageName);
					}
				}
			} catch (Exception e) {
			}
		}
		
		return themeList;
	}
	
	public final static List<String> getInstalledThemeListSync(ThemePackageFilter filter) {
		AABaseApplication application = AABaseApplication._internalInstance;
		
		List<String> themeList = new ArrayList<String>();
		PackageManager packageManager = application.getPackageManager();
		List<ApplicationInfo> installedApplications = packageManager.getInstalledApplications(0);
		
		for (ApplicationInfo info : installedApplications) {
			try {
				if (filter.isValid(packageManager, info)) {
					themeList.add(info.packageName);
				}
			} catch (Exception e) {
			}
		}
		
		return themeList;
	}
	
	public final static Resources getThemeResources() {
		if (isThemeInstalled()) {
			if (currentThemeResources != null && currentThemeResources.get() != null) {
				return currentThemeResources.get();
			}
			
			AABaseApplication application = AABaseApplication._internalInstance;
			
			try {
				String themePackageName = application.getSelectedThemePackageName();
				if (M2baseUtility.isNotNullOrEmpty(themePackageName)) {
					PackageManager pm = application.getPackageManager();
					Resources themeRes = pm.getResourcesForApplication(themePackageName);
					
					if (themeRes != null) {
						currentThemeResources = new WeakReference<Resources>(themeRes);
						
						if (currentThemeIdMap == null || currentThemeIdMap.get() == null) {
							currentThemeIdMap = new SoftReference<Map<Integer,Integer>>(Collections.synchronizedMap(new HashMap<Integer, Integer>()));
						}
						
						return themeRes;
					}
				}
			} catch (Exception e) {
				logger.e(e);
				e.printStackTrace();
			}
		}
		
		return null;
	}
	
	public final static int getThemeId(String packageName, String defName, String name) {
		if (isThemeInstalled()) {
			return getThemeId(getThemeResources(), packageName, defName, name);
		}
		
		return -1;
	}
	
	public final static int getThemeId(Resources res, String packageName, String defName, String name) {
		return res.getIdentifier(name, defName, packageName);
	}
	
	public final static int getThemeId(int id) {
		AABaseApplication application = AABaseApplication._internalInstance;

		if (isThemeInstalled()) {
			try {
				if (currentThemeIdMap != null) {
					Map<Integer, Integer> idMap = currentThemeIdMap.get();
					if (idMap != null && idMap.containsKey(id)) {
						return idMap.get(id);
					}
				}
				
				Resources res = application.getResources();
				String fullName = res.getResourceName(id);
				String packageName = null;
				String defName = null;
				String name = null;
				
				if (fullName.indexOf(":") > 0) {
					// 패키지명 분리
					String[] names = fullName.split(":");
					packageName = application.getSelectedThemePackageName();
					if (names.length > 1) {
						String[] defAndName = names[1].split("/");
						defName = defAndName[0];
						name = defAndName[1];
					}
					
					if (M2baseUtility.isNotNullOrEmpty(packageName) && M2baseUtility.isNotNullOrEmpty(defName) && M2baseUtility.isNotNullOrEmpty(name)) {
						int themeId = getThemeId(packageName, defName, name);
						
						if (currentThemeIdMap == null || currentThemeIdMap.get() == null) {
							currentThemeIdMap = new SoftReference<Map<Integer,Integer>>(Collections.synchronizedMap(new HashMap<Integer, Integer>()));
						}
						
						if (currentThemeIdMap == null) {
							return 0;
						}
						
						currentThemeIdMap.get().put(id, themeId);
						return themeId;
					}
				}
			} catch (Exception e) {
				logger.e(e);
				application.selectThemePackageName(null);
			}
		}
		
		return 0;
	}
	
	public final static Drawable getThemeDrawable(String name) {
		return getThemeDrawable(name, true);
	}
	
	public final static Drawable getThemeDrawable(String name, boolean loadDefault) {
		AABaseApplication application = AABaseApplication._internalInstance;

		if (isThemeInstalled()) {
			try {
				Resources res = getThemeResources();
				int id = res.getIdentifier(name, "drawable", application.getSelectedThemePackageName());
				
				if (id != 0) {
					return res.getDrawable(id);
				}
			} catch (Exception e) {
				logger.e(e);
				application.selectThemePackageName(null);
			}
		}
		
		if (loadDefault) {
			Resources res = application.getResources();
			int id = res.getIdentifier(name, "drawable", application.getPackageName());
			
			return res.getDrawable(id);
		}
		
		return null;
	}
	
	public final static Drawable getThemeDrawable(int id) {
		return getThemeDrawable(id, true);
	}
	
	public final static Drawable getThemeDrawable(int id, boolean loadDefault) {
		AABaseApplication application = AABaseApplication._internalInstance;
		
		if (isThemeInstalled()) {
			try {
				int themeId = getThemeId(id);
				
				if (themeId != 0) {
					Resources res = getThemeResources();
					return res.getDrawable(themeId);
				}
			} catch (Exception e) {
				logger.e(e);
				application.selectThemePackageName(null);
			}
		}
		
		if (loadDefault) {
			Resources res = application.getResources();
			return res.getDrawable(id);
		}
		
		return null;
	}
	
	public final static int getThemeColor(String name) {
		return getThemeColor(name, true);
	}
	
	public final static int getThemeColor(String name, boolean loadDefault) {
		AABaseApplication application = AABaseApplication._internalInstance;

		if (isThemeInstalled()) {
			try {
				Resources res = getThemeResources();
				int id = res.getIdentifier(name, "color", application.getSelectedThemePackageName());
				if (id != 0) {
					return res.getColor(id);
				}
			} catch (Exception e) {
				logger.e(e);
				application.selectThemePackageName(null);
			}
		}
		
		if (loadDefault) {
			Resources res = application.getResources();
			int id = res.getIdentifier(name, "color", application.getPackageName());
			
			return res.getColor(id);
		}
		
		return 0;
	}
	
	public final static Integer getThemeColor(int id) {
		return getThemeColor(id, true);
	}
	
	public final static Integer getThemeColor(int id, boolean loadDefault) {
		AABaseApplication application = AABaseApplication._internalInstance;

		if (isThemeInstalled()) {
			try {
				int themeId = getThemeId(id);
				if (themeId != 0) {
					Resources res = getThemeResources();
					return res.getColor(themeId);
				}
			} catch (Exception e) {
				logger.e(e);
				application.selectThemePackageName(null);
			}
		}
		
		if (loadDefault) {
			Resources res = application.getResources();
			return res.getColor(id);
		}
		
		return null;
	}
	
	public final static void overrideAttributes(View v, AttributeSet attrs) {
		if (isThemeInstalled() == false) {
			return;
		}
		
		int attrCount = attrs.getAttributeCount();
		ImageView iv = null;
		TextView tv = null;
		ListView lv = null;
		
		Drawable backgroundDrawable = null;
		Drawable alternativeBackgroundDrawable = null;
		
		if (v instanceof ImageView) {
			iv = (ImageView) v;
		}
		
		if (v instanceof TextView) {
			tv = (TextView) v;
		}
		
		if (v instanceof ListView) {
			lv = (ListView) v;
		}
		
		for (int i = 0; i < attrCount; i++) {
			String name = attrs.getAttributeName(i);
			String value = attrs.getAttributeValue(i);
			
			if (name.equals("theme_drawable") && iv != null) {
				Drawable dr = null;
				
				if (value.startsWith("@")) {
					int id = Integer.parseInt(value.substring(1));
					dr = getThemeDrawable(id, false);
				} else {
					dr = getThemeDrawable(value, false);
				}
				
				if (dr != null) {
					iv.setImageDrawable(dr);
				}
			} else if (name.equals("theme_color") && tv != null) {
				Integer c = null;
				
				if (value.startsWith("@")) {
					c = getThemeColor(Integer.parseInt(value.substring(1)), true);
				} else {
					c = getThemeColor(value, false);
				}
				
				if (c != null) {
					tv.setTextColor(c);
					tv.setHintTextColor(c);
					tv.setLinkTextColor(c);
				}
			} else if (name.equals("theme_shadowColor") && tv != null) {
				Integer c = null;
				
				if (value.startsWith("@")) {
					c = getThemeColor(Integer.parseInt(value.substring(1)), true);
				} else {
					c = getThemeColor(value, false);
				}
				
				if (c != null) {
					if (c == 0) {
						tv.setShadowLayer(0, 0, 0, 0);
						tv.getPaint().clearShadowLayer();
					} else {
						tv.setShadowLayer(2, 1, 1, c);
					}
				}
			} else if (name.equals("theme_hintColor") && tv != null) {
				Integer c = null;
				
				if (value.startsWith("@")) {
					c = getThemeColor(Integer.parseInt(value.substring(1)), true);
				} else {
					c = getThemeColor(value, false);
				}
				
				if (c != null) {
					tv.setHintTextColor(c);
				}
			} else if (name.equals("theme_backgroundColor")) {
				Integer c = null;
				
				if (value.startsWith("@")) {
					c = getThemeColor(Integer.parseInt(value.substring(1)), true);
				} else {
					c = getThemeColor(value, false);
				}
				
				if (c != null) {
					v.setBackgroundColor(c);
				}
			} else if (name.equals("theme_backgroundDrawable")) {
				Drawable dr = null;
				
				if (value.startsWith("@")) {
					int id = Integer.parseInt(value.substring(1));
					dr = getThemeDrawable(id, false);
				} else {
					dr = getThemeDrawable(value, false);
				}
				
				if (dr != null) {
					v.setBackgroundDrawable(dr);
					backgroundDrawable = dr;
				}
			} else if (name.equals("theme_alternativeBackgroundDrawable")) {
				Drawable dr = null;
				
				if (value.startsWith("@")) {
					int id = Integer.parseInt(value.substring(1));
					dr = getThemeDrawable(id, false);
				} else {
					dr = getThemeDrawable(value, false);
				}
				
				if (dr != null) {
					alternativeBackgroundDrawable = dr;
				}
			} else if (name.equals("theme_divider") && lv != null) {
				Drawable dr = null;
				
				if (value.startsWith("@")) {
					int id = Integer.parseInt(value.substring(1));
					dr = getThemeDrawable(id, false);
				} else {
					dr = getThemeDrawable(value, false);
				}
				
				if (dr != null) {
					lv.setDivider(dr);
				}
			} else if (name.equals("theme_listSelector") && lv != null) {
				Drawable dr = null;
				
				if (value.startsWith("@")) {
					int id = Integer.parseInt(value.substring(1));
					dr = getThemeDrawable(id, false);
				} else {
					dr = getThemeDrawable(value, false);
				}
				
				if (dr != null) {
					lv.setSelector(dr);
				}
			}
		}
		
		if (backgroundDrawable == null && alternativeBackgroundDrawable != null) {
			v.setBackgroundDrawable(alternativeBackgroundDrawable);
		}
	}
	
	public final static boolean isNhnOfficialTheme() {
		AABaseApplication application = AABaseApplication._internalInstance;

		String themePackageName = application.getSelectedThemePackageName();
		String nhnSignaturePartial = "3082028e308201f";
		if(M2baseUtility.isNotNullOrEmpty(themePackageName)) {
			try {
				PackageManager pm = application.getPackageManager();
				PackageInfo info = pm.getPackageInfo(themePackageName, PackageManager.GET_SIGNATURES);
				Signature[] sigs = info.signatures;
				if (sigs != null && sigs.length > 0) {
					for (Signature sig : sigs) {
						logger.d("Signature: %s", sig.toCharsString());
						if (sig.toCharsString().startsWith(nhnSignaturePartial)) {
							return true;
						}
					}
				}
			} catch (Exception e) {
				logger.e(e);
			}
		}
		return false;
	}
	
	public static interface ThemePackageFilter {
		boolean isValid(PackageManager packageManager, ApplicationInfo info);
	}
	
	public static interface GetThemeListListener {
		void onSuccess(List<String> themeList);
	}
	
	private static class GetThemeListTask extends AsyncTask<Void, Void, List<String>> {
		private String permission;
		private ThemePackageFilter filter;
		private GetThemeListListener listener;
		
		public GetThemeListTask(String permission, ThemePackageFilter filter, GetThemeListListener listener) {
			this.permission = permission;
			this.filter = filter;
			this.listener = listener;
		}
		
		@Override
		protected List<String> doInBackground(Void... params) {
			if (M2baseUtility.isNotNullOrEmpty(permission)) {
				return getInstalledThemeListSync(permission);
			} else if (filter != null) {
				return getInstalledThemeListSync(filter);
			}
			
			return null;
		}

		@Override
		protected void onPostExecute(List<String> result) {
			listener.onSuccess(result);
		}
	}
}
