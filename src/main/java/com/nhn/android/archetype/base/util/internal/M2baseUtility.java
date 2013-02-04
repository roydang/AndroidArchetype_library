package com.nhn.android.archetype.base.util.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import com.nhn.android.archetype.base.BaseApplication;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;

public class M2baseUtility {
	private static String versionName = null;
	
	public static int getVersionCode(Context context) {
		int versionCode = 0;

		try {
			PackageInfo i = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			versionCode = i.versionCode;
		} catch (Exception e) {
			// do something
		}

		return versionCode;
	}

	public static String getVersionName(Context context) {
		if (versionName == null) {
			try {
				PackageInfo i = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
				versionName = i.versionName;
			} catch (Exception e) {
				// do something
			}
		}

		return versionName;
	}
	
	public final static boolean isJellyBeanCompatibility() {
		return Build.VERSION.SDK_INT >= 16;
	}
	
	public final static boolean isICSCompatibility() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
	}

	public final static boolean isGingerBreadCompatibility() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
	}

	public final static boolean isGingerBreadMR1Compatibility() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1;
	}
	
	public final static boolean isUnderFroyo() {
		return Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO;
	}

	public final static boolean isNullOrEmpty(String str) {
		if (str == null || str.length() == 0) {
			return true;
		}
		return false;
	}

	public final static boolean isStringNullOrEmpty(String str) {
		return !isNotStringOrNullOrEmpty(str);
	}

	public final static boolean isNotStringOrNullOrEmpty(String str) {
		if (str != null && str.length() > 0) {
			str = str.trim();
			if (!"null".equals(str)) {
				return true;
			}
		}
		return false;
	}

	public final static boolean isNotNullOrEmpty(String str) {
		return !isNullOrEmpty(str);
	}
	
	public final static String convertStreamToString(InputStream is) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			//do something
		}
		return sb.toString().trim();
	}
	
	public final static String md5(String value) {
		try {
	        MessageDigest md = MessageDigest.getInstance("MD5");
	        byte[] messageDigest = md.digest(value.getBytes());
	        BigInteger number = new BigInteger(1, messageDigest);
	        String md5 = number.toString(16);

	        while (md5.length() < 32) {
				md5 = "0" + md5;
			}
	        return md5;

	    } catch (NoSuchAlgorithmException e) {
	        e.printStackTrace();
	        return null;
	    }
	}

	public final static Locale getSystemLocale(Context context) {
		if (context == null) {
			context = BaseApplication._internalInstance;
		}

		if (context == null) {
			return Locale.KOREA;
		}

		Locale locale = context.getResources().getConfiguration().locale;
		String language = locale.getLanguage();
		String country = locale.getCountry();

		if (country.equals(Locale.TRADITIONAL_CHINESE.getCountry())) {
			return Locale.TRADITIONAL_CHINESE;
		}

		if (language.equals(Locale.KOREA.getLanguage())) {
			return Locale.KOREA;
		}

		if (language.equals(Locale.CHINA.getLanguage())) {
			return Locale.CHINA;
		}

		if (language.equals(Locale.JAPAN.getLanguage())) {
			return Locale.JAPAN;
		}

		return Locale.US;
	}

	public final static String getSystemLocaleString(Context context) {
		Locale locale = getSystemLocale(context);

		return locale.toString().replace("_", "-");
	}

	public final static String getSystemLocaleStringUsedAppStat(Context context) {

		Locale mlocale = context.getResources().getConfiguration().locale;
		return mlocale.toString().replace("_", "-");
	}

	public final static boolean equals(String str1, String str2) {
		if (str1 == str2) {
			return true;
		}
		
		if (str1 == null) {
			return false;
		}
		
		return str1.equals(str2);
	}
	
	public final static boolean equalsIgnoreCase(String str1, String str2) {
		if (str1 == str2) {
			return true;
		}
		
		if (str1 == null) {
			return false;
		}
		
		return str1.equalsIgnoreCase(str2);
	}
	
	public final static String format(String format, Object...args) {
		try {
			return String.format(format, args);
		} catch (Exception e) {
		}
		
		return format;
	}
}
