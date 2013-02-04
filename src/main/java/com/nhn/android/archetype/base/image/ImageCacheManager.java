package com.nhn.android.archetype.base.image;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.util.LruCache;

import com.nhn.android.archetype.base.BaseApplication;
import com.nhn.android.archetype.base.theme.ThemeHelper;
import com.nhn.android.archetype.base.util.internal.M2baseLogger;
import com.nhn.android.archetype.base.util.internal.M2baseUtility;

public class ImageCacheManager {
	private static M2baseLogger logger = M2baseLogger.getLogger(ImageCacheManager.class);
	
	private static Map<String, WeakReference<Bitmap>> simpleImageCache;
	private static LruCache<String, Bitmap> simpleLruCache = null;
	private static WeakHashMap<String, File> filePool = null;
	
	static {
		initMemoryCache();
	}
	
	// new File(xxx, xxx) 의 경우 내부적으로 String Join연산이 발생하는데 이 비용이 생각보다 만만찮음
	// ObjectPool 패턴으로 접근하여 메모리 사용량을 줄임
	private synchronized static File newFile(String name) {
		File file = null;
		
		if (filePool.containsKey(name)) {
			file = filePool.get(name);
		}
		
		if (file != null) {
			return file;	
		}
		
		file = new File(BaseApplication._internalInstance.getExternalCacheFolder(), M2baseUtility.format("%s/%s", name.charAt(name.length()-1), name));
		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		
		filePool.put(name, file);
		
		return file;
	}
	
	private static void initMemoryCache() {
		int defaultSize = M2baseUtility.isICSCompatibility() ? 12 : 4;
		simpleLruCache = new LruCache<String, Bitmap>(defaultSize * 1024 * 1024) {
	        protected int sizeOf(String key, Bitmap bitmap) {
	        	try {
	        		int size = bitmap.getRowBytes() * bitmap.getHeight();
	        		
	        		return size;
	        	} catch (Exception e) {
	        	}
	        	
	        	return 100;
	        }
		};
		
		simpleImageCache = new HashMap<String, WeakReference<Bitmap>>();
		
		filePool = new WeakHashMap<String, File>();
	}
	
	public static Bitmap getFromCache(String url) {
		if (M2baseUtility.isNullOrEmpty(url)) {
			return null;
		}
		
		Bitmap bitmap = simpleLruCache.get(url);
		if (bitmap != null) {
			return bitmap;
		}
		
		if (simpleImageCache.containsKey(url)) {
			WeakReference<Bitmap> bitmapRef = simpleImageCache.get(url);
			bitmap = bitmapRef.get();
			if (bitmap == null) {
				simpleImageCache.remove(url);
			}
		}
		
		if (bitmap != null) {
			return bitmap;
		}
		
		if (url.startsWith("http")) {
			// return getFromFile(url);
		} else {
			try {
				char c = url.charAt(0);
				if (Character.isDigit(c)) {
					int resId = Integer.parseInt(url);
					
					BitmapDrawable drawable = (BitmapDrawable) ThemeHelper.getThemeDrawable(resId);
					Bitmap bm = drawable.getBitmap();
					
					return bm;
				}
			} catch (Exception e) {
			} catch (Error err) {
			}
		}	

		return null;
	}

	public static void putIntoCache(String url, Bitmap bitmap) {
		if (M2baseUtility.isNullOrEmpty(url) || bitmap == null) {
			return;
		}
		
		simpleLruCache.put(url, bitmap);
		simpleImageCache.put(url, new WeakReference<Bitmap>(bitmap));	
	}
	
	public static void clearMemoryCache() {
		initMemoryCache();
	}
	
	public static void putIntoFileCache(String url, Bitmap bitmap) {
		Bitmap.CompressFormat format = Bitmap.CompressFormat.JPEG;
		
		if (url.indexOf(".png") > 0) {
			format = Bitmap.CompressFormat.PNG;
		}
		
		putIntoFileCache(url, bitmap, format);
	}
	
	public static void putIntoFileCache(String url, Bitmap bitmap, Bitmap.CompressFormat format) {
		if (M2baseUtility.isNullOrEmpty(url) || bitmap == null) {
			return;
		}
		
		putIntoCache(url, bitmap);
		String key = getKey(url);
		
		File targetFile = newFile(key);
		ImageHelper.saveBitmap(bitmap, targetFile, format, format == Bitmap.CompressFormat.JPEG ? 95 : 100);
	}
	
	public static void putIntoCache(String url, InputStream stream) {
		String key = getKey(url);
		File targetFile = newFile(key);
		File tempFile = newFile("t"+key);
		
		if (tempFile.exists()) {
			tempFile.delete();
		}
		
		FileOutputStream fos = null;
		BufferedInputStream bis = new BufferedInputStream(stream, 1024 * 8);
		boolean success = false;
		
		try {
			fos = new FileOutputStream(tempFile);
			byte[] buffer = new byte[8 * 1024];
			
			while (true) {
				int cnt = bis.read(buffer);
				if (cnt < 0) {
					break;
				}
				
				fos.write(buffer, 0, cnt);
			}
			
			success = true;
		} catch (Exception e) {
			logger.e(e);
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (Exception e) {
				}
			}
		}
		
		if (success) {
			if (targetFile.exists()) {
				targetFile.delete();
			}
			
			tempFile.renameTo(targetFile);
		}
	}
	
	public static Bitmap getFromFile(String url, int sampleWidth) {
		if (M2baseUtility.isNullOrEmpty(url)) {
			return null;
		}
		
		File file = null;
		boolean needPutCache = true;
		boolean adjustOrientation = false;
		
		if (url.startsWith("http")) {
			String key = getKey(url);
			file = newFile(key);
		} else {
			file = new File(url);
			needPutCache = false;
			adjustOrientation = true;
		}
		
		Bitmap bm = null;
		
		if (file.exists()) {
			if (adjustOrientation) {
				BitmapFactory.Options option = new BitmapFactory.Options();
				option.inSampleSize = 16;
				option.inInputShareable = true;
				bm = ImageHelper.decodeFile(file.getAbsolutePath(), option, true);
			} else {
				bm = ImageHelper.decodeFile(file.getAbsolutePath(), sampleWidth);
			}
			
			if (needPutCache && bm != null) {
				file.setLastModified(System.currentTimeMillis());
				putIntoCache(url, bm);
			}
		}
		
		return bm;
	}
	
	public static boolean containFileCache(String url) {
		String key = getKey(url);
		File file = newFile(key);
		
		return file.exists();
	}
	
	public static String getKey(String url) {
		return "cm2_"+M2baseUtility.md5(url);
	}
	
	public static File getFile(String url) {
		String key = getKey(url);
		return newFile(key);
	}
}

