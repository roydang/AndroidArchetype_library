package com.nhn.android.archetype.base.image;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.MediaStore;

import com.nhn.android.archetype.base.AABaseApplication;
import com.nhn.android.archetype.base.image.listener.ImageLoadListener;
import com.nhn.android.archetype.base.image.listener.ProgressiveImageLoadListener;
import com.nhn.android.archetype.base.object.ApiResponse;
import com.nhn.android.archetype.base.util.internal.M2baseLogger;
import com.nhn.android.archetype.base.util.internal.M2baseUtility;

public class ImageHelper {
	public static final int SAMPLING_WIDTH_DEFAULT = 160;
	public static final int SAMPLING_WIDTH_NONE = -1;

	public static final String THUMB_ORIGINAL = "original"; // http://me2day.phinf.naver.net
	public static final String THUMB_DOMAIN_ORIGINAL = "me2day.phinf.naver.net";
	public static final String THUMB_DOMAIN = "me2daythumb.phinf.naver.net";
	public static final String THUMB_S40 = "s40";
	public static final String THUMB_S44 = "s44";
	public static final String THUMB_S75 = "s75";
	public static final String THUMB_W25 = "w25";
	public static final String THUMB_W100 = "w100";
	public static final String THUMB_W200 = "w200";
	public static final String THUMB_W358 = "w358";
	public static final String THUMB_W500 = "w500";
	public static final String THUMB_W578 = "w578";
	public static final String THUMB_W640 = "w640";
	public static final String THUMB_M180 = "m180";
	public static final String THUMB_F122_90 = "f122_90";
	public static final String THUMB_F100_113 = "f100_113";
	public static final String THUMB_F100_90 = "f100_90";
	public static final String THUMB_F114_114 = "f100_90";
	public static final String THUMB_F199_143 = "f199_143";
	public static final String THUMB_F158_183 = "f158_183";
	public static final String THUMB_F158_141 = "f158_141";
	public static final String THUMB_F320 = "f320";
	public static final String THUMB_F640 = "f640";
	public static final String THUMB_M2500_2500 = "m2500_2500";

	private static M2baseLogger logger = M2baseLogger.getLogger(ImageHelper.class);

	public final static Bitmap getFromCache(String url) {
		return ImageCacheManager.getFromCache(url);
	}

	public final static Bitmap getFromCache(String url, Object maskImageKey) {
		return ImageCacheManager.getFromCache(getCacheKey(url, maskImageKey));
	}
	
	public final static File getFileFromCache(String url) {
		return ImageCacheManager.getFile(url);
	}

	public final static String getCacheKey(String url, Object maskImageKey) {
		if (maskImageKey instanceof MaskData) {
			MaskData data = (MaskData) maskImageKey;
			return getCacheKey(url, data.getMaskKey(), data.getWidth(), data.getHeight());		
		}
		
		return getCacheKey(url, maskImageKey, 0, 0);
	}
	
	public final static String getCacheKey(String url, Object maskImageKey, int maskWidth, int maskHeight) {
		if (M2baseUtility.isNullOrEmpty(url)) {
			return url;
		}
		
		if (maskImageKey != null) {
			String maskKeyStr = maskImageKey.toString();
			
			if (maskWidth != 0 && maskHeight != 0) {
				maskKeyStr = M2baseUtility.format("%s%s%s", maskKeyStr, maskWidth, maskHeight);
			}
			
			int len = url.length();
			String tempUrl = url;
			
			if (len > 15) {
				int hashCode = url.hashCode();
				tempUrl = M2baseUtility.format("%s%s%s%s:%s", url.substring(0, 8), url.substring(len - 15), hashCode, len, maskKeyStr);
			} else {
				tempUrl = M2baseUtility.format("%s:%s", url, maskKeyStr);
			}
			
			logger.d("getCacheKey: %s -> %s", url, tempUrl);
			
			url = tempUrl;
			
			if (url.startsWith("http")) {
				return url;
			} else {
				AABaseApplication application = AABaseApplication._internalInstance;
				return M2baseUtility.format("http://%s/%s/%s", url, M2baseUtility.getVersionName(application), application.getSelectedThemePackageName());
			}
		}

		return url;
	}

	public final static void putIntoCache(String url, Bitmap bitmap) {
		ImageCacheManager.putIntoCache(url, bitmap);
	}

	public final static void loadImage(String url, ImageLoadListener listener) {
		loadImage(url, SAMPLING_WIDTH_DEFAULT, listener);
	}

	public final static void loadImage(String url, int sampleWidth, ImageLoadListener listener) {
		loadImage(url, false, sampleWidth, listener);
	}

	public final static void loadImage(String url, boolean force, int sampleWidth, final ImageLoadListener listener) {
		loadImage(url, force, sampleWidth, null, listener);
	}

	public final static void loadImage(String url, boolean force, int sampleWidth, String maskKey, Object maskDrawable, ImageLoadListener listener) {
		loadImage(url, force, sampleWidth, new MaskData(maskKey, maskDrawable, 0, 0), listener);
	}
	

	@SuppressLint("NewApi")
	public final static void loadImage(final String url, final boolean force, final int sampleWidth, final MaskData maskData, final ImageLoadListener listener) {
		if (M2baseUtility.isNullOrEmpty(url)) {
			return;
		}

		final Bitmap image = getFromCache(url, maskData);
		Handler handler = AABaseApplication._internalInstance.getHandler();

		if (image == null) {
			Handler backgroundHandler = AABaseApplication._internalInstance.getBackgroundHandler();
			if (backgroundHandler != null) {
				backgroundHandler.post(new Runnable() {
					@Override
					public void run() {
						//logger.d("run in background");
						
						if (listener instanceof ProgressiveImageLoadListener) {
							if (ImageCacheManager.containFileCache(url)) {
								ImageLoadManager.push(url, force, sampleWidth, null, false, listener);
								return;
							}

							ImageProgressiveLoadTask data = new ImageProgressiveLoadTask(url, force, sampleWidth, (ProgressiveImageLoadListener) listener);

							if (M2baseUtility.isICSCompatibility()) {
								data.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
							} else {
								data.execute();
							}
						} else {
							ImageLoadManager.push(url, force, sampleWidth, maskData, false, listener);
						}
					}
				});
			} else {
				if (listener instanceof ProgressiveImageLoadListener) {
					if (ImageCacheManager.containFileCache(url)) {
						ImageLoadManager.push(url, force, sampleWidth, null, false, listener);
						return;
					}

					ImageProgressiveLoadTask data = new ImageProgressiveLoadTask(url, force, sampleWidth, (ProgressiveImageLoadListener) listener);

					if (M2baseUtility.isICSCompatibility()) {
						data.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
					} else {
						data.execute();
					}
				} else {
					ImageLoadManager.push(url, force, sampleWidth, maskData, false, listener);
				}
			}
		} else {
			if (handler != null) {
				handler.post(new Runnable() {
					public void run() {
						listener.onSuccess(image);
					}
				});
			} else {
				try {
					listener.onSuccess(image);
				} catch (Exception e) {
					logger.e(e);
				}
			}
		}
	}

	public final static void downloadImage(String url) {
		ImageLoadManager.push(url, false, ImageHelper.SAMPLING_WIDTH_NONE, null, true, null);
	}

	public final static void downloadImage(String url, ImageLoadListener listener) {
		ImageLoadManager.push(url, false, ImageHelper.SAMPLING_WIDTH_NONE, null, true, listener);
	}

	public final static void cancelRequest() {
		ImageLoadManager.cancelRequest();
	}

	public final static String saveBitmap(Bitmap bitmap, String fileName) {
		File file = null;

		if (fileName.startsWith("/")) {
			file = new File(fileName);
		} else {
			File sdCard = AABaseApplication._internalInstance.getExternalImagesFolder();
			file = new File(sdCard, fileName);
		}

		if (file.exists()) {
			boolean result = file.delete();
			if (result) {
				// do something
			}
		}

		return saveBitmap(bitmap, file, CompressFormat.JPEG, 95);
	}

	public final static String saveBitmap(Bitmap bitmap, File file, Bitmap.CompressFormat format, int percent) {
		if (bitmap == null) {
			return null;
		}

		FileOutputStream fos = null;

		try {
			String path = file.getAbsolutePath();
			File dir = new File(path.substring(0, path.lastIndexOf('/')));
			if (dir.exists() == false) {
				boolean result = dir.mkdirs();
				if (result) {
					// do something
				}
			}

			fos = new FileOutputStream(file);
			bitmap.compress(format, percent, fos);
			//logger.d("saveBitmap(%s)", path);

			return path;
		} catch (Exception e) {
			e.printStackTrace();
			logger.e(e);
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
					logger.e(e);
				}
			}
		}

		return null;
	}

	public final static String copyFileMediaScan(File original, String fileName, Context context) {
		if (original == null || original.exists() == false) {
			return null;
		}

		File sdCard = AABaseApplication._internalInstance.getExternalImagesFolder();
		File file = new File(sdCard, fileName);
		if (file.exists()) {
			boolean result = file.delete();
			if (result) {
				// do something
			}
		}
		
		FileInputStream fis = null;
		FileOutputStream fos = null;
		
		String filePath = null;
		
		try {
			fis = new FileInputStream(original);
			fos = new FileOutputStream(file);
			
			FileChannel inChannel = fis.getChannel();
			FileChannel outChannel = fos.getChannel();
			
			inChannel.transferTo(0, fis.available(), outChannel);
			
			filePath = file.getAbsolutePath();
		} catch (Exception e) {
			logger.e(e);
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
				}
			}
			
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
				}
			}
		}
		
		if (filePath != null) {
			context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + filePath)));
		}
		
		return filePath;
	}
	
	public final static String saveBitmapMediaScan(Bitmap bitmap, String fileName, Context context) {
		if (bitmap == null) {
			return null;
		}

		File sdCard = AABaseApplication._internalInstance.getExternalImagesFolder();
		File file = new File(sdCard, fileName);
		if (file.exists()) {
			boolean result = file.delete();
			if (result) {
				// do something
			}
		}
		String filePath = saveBitmap(bitmap, file, CompressFormat.JPEG, 95);
		//logger.d("saveBitmapMediaScan filePath(%s)", filePath);

		if (filePath != null) {
			context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + filePath)));
		}
		return filePath;
	}

	public final static Bitmap convertDrawable(Drawable drawable, int w, int h) {
		drawable = drawable.mutate();

		if (drawable instanceof NinePatchDrawable) {
			NinePatchDrawable npd = (NinePatchDrawable) drawable;
			npd.setBounds(0, 0, w, h);
		} else if (drawable instanceof BitmapDrawable) {
			return ((BitmapDrawable) drawable).getBitmap();
		}

		Bitmap ret = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(ret);
		drawable.draw(canvas);

		return ret;
	}

	public final static Bitmap maskBitmap(Bitmap bitmap, Bitmap mask) {
		int targetWidth = mask.getWidth();
		int targetHeight = mask.getHeight();
		
		if (targetWidth == 0 || targetHeight == 0) {
			targetWidth = bitmap.getWidth();
			targetHeight = bitmap.getHeight();
		}
		
		Bitmap result = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);

		Canvas c = new Canvas(result);
		c.drawBitmap(bitmap, new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()), new Rect(0, 0, mask.getWidth(), mask.getHeight()), null);

		Paint paint = new Paint();
		paint.setFilterBitmap(false);
		paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));

		c.drawBitmap(mask, 0, 0, paint);

		return result;
	}

	public final static String getThumbnailType(String url) {
		int idx = url.indexOf("type=");
		String tail = url.substring(idx + "type=".length());

		int ampIdx = tail.indexOf("&");
		if (ampIdx >= 0) {
			tail = tail.substring(0, ampIdx);
		}

		return tail;
	}

	public final static String getThumbnailUrl(String url, String thumbnailType) {
		if (M2baseUtility.isNullOrEmpty(thumbnailType)) {
			return url;
		}

		if (M2baseUtility.isNullOrEmpty(url)) {
			return url;
		}

		if (thumbnailType.equals(THUMB_ORIGINAL)) {
			return url.split("\\?")[0];
		} else {
			if (url.indexOf(THUMB_DOMAIN_ORIGINAL) > -1) {
				url = url.replace(THUMB_DOMAIN_ORIGINAL, THUMB_DOMAIN);
			}
		}

		url = url.trim();
		String typeStr = "type=" + thumbnailType;

		int idx = url.indexOf("type=");
		if (idx < 0) {
			if (url.indexOf("?") < 0) {
				url += "?";
			}

			return url + typeStr;
		}

		String head = url.substring(0, idx);
		String tail = url.substring(idx);

		int ampIdx = tail.indexOf("&");
		if (ampIdx > 0) {
			return head + typeStr + tail.substring(ampIdx);
		}

		return head + typeStr;
	}

	public final static Bitmap decodeFile(String path) {
		return decodeFile(path, 160);
	}

	public final static Bitmap decodeFile(String path, int sampleWidth) {
		//logger.d("decodeFile(%s, %s)", path, sampleWidth);

		if (sampleWidth <= 0) {
			BitmapFactory.Options option = new BitmapFactory.Options();
			option.inDither = true;

			option.inPurgeable = true;
			option.inInputShareable = true;
			option.inScaled = false;
			
			return decodeFile(path, option);
		}

		BitmapFactory.Options option = new BitmapFactory.Options();
		option.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, option);

		int w = option.outWidth;
		option = new BitmapFactory.Options();

		int sampleSize = 1;
		while (true) {
			w = w / 2;

			if (w < sampleWidth) {
				break;
			}

			sampleSize++;
		}

		option.inSampleSize = sampleSize;
		option.inDither = true;
		
		// OutOfMemory 리스크 낮춤
		option.inPurgeable = true;
		option.inInputShareable = true;
		option.inScaled = false;

		return decodeFile(path, option);
	}

	public final static Bitmap decodeFile(String path, BitmapFactory.Options option) {
		return decodeFile(path, option, false);
	}

	public final static Bitmap decodeFile(String path, BitmapFactory.Options option, boolean adjustOrientation) {
		Bitmap bm = null;
		
		InputStream is = null;
		
		try {
			if (M2baseUtility.isJellyBeanCompatibility()) {
				bm = BitmapFactory.decodeFile(path, option);
			} else if (M2baseUtility.isICSCompatibility()) {
				// 갤럭시계열 ICS버전에 decodeFile 호출시 IO Lock이 걸리면서 스크롤이 튀는 현상이 있음
				// 오버메모리가 가능한 ICS에는  파일을 직접 로딩하여 디코딩함.
				is = new FileInputStream(path);
				byte[] buffer = new byte[is.available()];
				
				is.read(buffer);
				bm = BitmapFactory.decodeByteArray(buffer, 0, buffer.length, option);
			} else {
				bm = BitmapFactory.decodeFile(path, option);
			}
		} catch (Exception e) {
			logger.e(e);
		} catch (Error err) {
			logger.e(err);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
				}
			}
		}

		if (bm == null) {
			return null;
		}
		
		if (!adjustOrientation) {
			return bm;
		}

		int orientation = queryExifOrientation(path);
		if (orientation != 0) {
			bm = getResizedBitmapConstrained(bm, Math.min(bm.getWidth(), bm.getHeight()), orientation);
		}

		return bm;
	}

	public final static File getFilePathFromUri(Context context, Uri uri) {
		if (uri != null && M2baseUtility.equals(uri.getScheme(), "file")) {
			logger.d("path: %s", uri.getPath());
			return new File(uri.getPath());
		}
		
		Cursor c = null;
		
		try {
			c = context.getContentResolver().query(uri, null,null,null,null);
			c.moveToNext();
			String path = c.getString(c.getColumnIndex(MediaStore.MediaColumns.DATA));
			return new File(path);
		} catch (Exception e) {
			logger.e(e);
		} finally {
			if (c != null) {
				c.close();
				c = null;
			}
		}
		
		return null;
	}
	
	public final static Bitmap getThumbnailFromMediaStore(Context context, long id, String uri) {
		ContentResolver crThumb = context.getContentResolver();
		long videoId = id;
		int orientation = 0;

		if (videoId <= 0) {
			String[] proj = { MediaStore.Images.ImageColumns._ID, MediaStore.Images.ImageColumns.DATA, MediaStore.Images.ImageColumns.DISPLAY_NAME, MediaStore.Images.ImageColumns.ORIENTATION};
			String displayName = uri.substring(uri.lastIndexOf("/") + 1);
			String selection = MediaStore.Images.ImageColumns.DISPLAY_NAME + " = \"" + displayName + "\"";
			Cursor thumbCursor = null;
			try {
				thumbCursor = crThumb.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, proj, selection, null, null);
				if (thumbCursor != null && thumbCursor.moveToFirst()) {
					int tid = thumbCursor.getColumnIndex(MediaStore.Images.ImageColumns._ID);
					videoId = thumbCursor.getLong(tid);
					
					tid = thumbCursor.getColumnIndex(MediaStore.Images.ImageColumns.ORIENTATION);
					orientation = thumbCursor.getInt(tid);
				}
			} catch (Exception e) {
				logger.e(e);
			} finally {
				if (thumbCursor != null) {
					thumbCursor.close();
					thumbCursor = null;
				}
			}
		}

		Bitmap curThumb = null;
		
		try {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inSampleSize = 128;
			//options.inSampleSize = 8;
			curThumb = MediaStore.Images.Thumbnails.getThumbnail(crThumb, videoId, MediaStore.Images.Thumbnails.MICRO_KIND, options);
		} catch (IllegalArgumentException ex) {
			logger.e(ex);
		} catch (Exception ex) {
			logger.e(ex);
		} finally {
			try {
				// retriever.release();
			} catch (RuntimeException ex) {
			}
		}
		
		//만약 media store에서 찾지 못한 이미지는 sampling size를 8로 설정 TODO
		if (curThumb == null) {
			logger.d("uri: %s", uri);
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inSampleSize = 8;
			curThumb = decodeFile(uri, options);
			//curThumb = decodeFile(uri);
		}

		if (curThumb != null) {
			if (orientation == 0) {
				orientation = queryExifOrientation(uri);	
			}
			
			if (curThumb.getWidth() != curThumb.getHeight()) {
				int w = Math.min(curThumb.getWidth(), curThumb.getHeight());
				int h = w;
				int tw = curThumb.getWidth();
				int th = curThumb.getHeight();
				
				Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
				Canvas canvas = new Canvas(bm);
				
				int l = (int)((w - tw) / 2.0);
				int t = (int)((h - th) / 2.0);
				
				canvas.drawBitmap(curThumb, new Rect(0, 0, tw, th), new Rect(l, t, l + tw, t + th), null);
				
				curThumb = bm;
			}
			
			if (orientation != 0) {
				curThumb = getResizedBitmapConstrained(curThumb, Math.min(curThumb.getWidth(), curThumb.getHeight()), orientation);
			}
		}

		return curThumb;
	}

	// orientation 처리
	public enum Axis {
		Horizontal, Vertical
	};

	static private Constructor<?> exifConstructor = null;
	static private Method exifGetAttributeInt = null;

	public final static int queryExifOrientation(String path) {
		int orientation = 0;

		if (exifConstructor == null) {
			try {
				exifConstructor = ExifInterface.class.getConstructor(new Class[] { String.class });
				exifGetAttributeInt = ExifInterface.class.getMethod("getAttributeInt", new Class[] { String.class, int.class });
			} catch (NoSuchMethodException e) {
				logger.e(e);
				return 0;
			}
		}

		Integer exifOrientation = 0;
		try {
			Object obj = exifConstructor.newInstance(new Object[] { path });
			exifOrientation = (Integer) exifGetAttributeInt.invoke(obj, ExifInterface.TAG_ORIENTATION, Integer.valueOf(0));
		} catch (Exception e) {
			logger.e(e);
			return 0;
		}

		switch (exifOrientation) {
		case ExifInterface.ORIENTATION_ROTATE_90:
			orientation = 90;
			break;
		case ExifInterface.ORIENTATION_ROTATE_180:
			orientation = 180;
			break;
		case ExifInterface.ORIENTATION_ROTATE_270:
			orientation = 270;
		}

		return orientation;
	}

	public final static Bitmap getResizedBitmap(Bitmap orig, int pixels, Axis axis, int orientation) {
		int ow = orig.getWidth();
		int oh = orig.getHeight();

		int nw, nh;

		if (orientation == 90 || orientation == 270) {
			if (axis == Axis.Horizontal) {
				nh = Math.min(pixels, oh);
				nw = (int) (nh * ((float) ow / oh));
			} else {
				nw = Math.min(pixels, ow);
				nh = (int) (nw * ((float) oh / ow));
			}
		} else {
			if (axis == Axis.Horizontal) {
				nw = Math.min(pixels, ow);
				nh = (int) (nw * ((float) oh / ow));
			} else {
				nh = Math.min(pixels, oh);
				nw = (int) (nh * ((float) ow / oh));
			}
		}

		float sw = (float) nw / ow;
		float sh = (float) nh / oh;

		Matrix matrix = new Matrix();
		matrix.postScale(sw, sh);

		if (orientation != 0) {
			matrix.postRotate((float) orientation);
		}

		return Bitmap.createBitmap(orig, 0, 0, ow, oh, matrix, true);
	}

	public final static Bitmap getResizedBitmapConstrained(Bitmap orig, int pixels, int orientation) {
		Axis axis;

		if (orig.getWidth() > orig.getHeight()) {
			axis = Axis.Horizontal;
		} else {
			axis = Axis.Vertical;
		}

		if (pixels <= 0) {
			if (axis == Axis.Horizontal) {
				pixels = orig.getWidth();
			} else {
				pixels = orig.getHeight();
			}
		}

		return getResizedBitmap(orig, pixels, axis, orientation);
	}

	private static class ImageProgressiveLoadTask extends AsyncTask<Void, Void, Void> {
		private static final String[] W = { THUMB_W640, THUMB_W578, THUMB_W500, THUMB_W358, THUMB_W200, THUMB_W100 };

		private static final String[] F = { THUMB_F640, THUMB_F320, THUMB_S75 };

		private volatile boolean origImageLoaded = false;
		private String url;
		private String thumbnailType;
		private boolean force;
		private int sampleWidth;

		private ProgressiveImageLoadListener listener;

		public ImageProgressiveLoadTask(String url, boolean force, int sampleWidth, ProgressiveImageLoadListener listener) {
			this.url = url;
			this.force = force;
			this.sampleWidth = sampleWidth;

			this.listener = listener;
		}

		public void load() {
			this.thumbnailType = getThumbnailType(url);

			if (this.thumbnailType == null) {
				this.thumbnailType = THUMB_W640;
			}

			String nearestType = findNearestThumbnailType();
			logger.d("load(%s)", nearestType);

			if (nearestType.equals(thumbnailType)) {
				ImageHelper.loadImage(url, force, sampleWidth, new ImageLoadListener() {
					public void onSuccess(Bitmap result) {
						logger.d("onSuccess");
						listener.onSuccess(result);
					}

					public void onError(ApiResponse result) {
						listener.onError(result);
					}
				});
			} else {
				ImageHelper.loadImage(getThumbnailUrl(url, nearestType), new ImageLoadListener() {
					public void onSuccess(Bitmap result) {
						if (origImageLoaded) {
							return;
						}

						logger.d("onPreload");
						listener.onPreload(result);
					}

					public void onError(ApiResponse result) {
					}
				});

				ImageHelper.loadImage(url, force, sampleWidth, new ImageLoadListener() {
					public void onSuccess(Bitmap result) {
						origImageLoaded = true;

						logger.d("onSuccess");
						listener.onSuccess(result);
					}

					public void onError(ApiResponse result) {
						logger.d("onError");
						listener.onError(result);
					}
				});
			}
		}

		private String findNearestThumbnailType() {
			char type = thumbnailType.charAt(0);
			String[] thumbnailTypes = null;

			if (type == 'w') {
				thumbnailTypes = W;
			} else if (type == 'f' || type == 's') {
				thumbnailTypes = F;
			}

			if (thumbnailTypes == null) {
				return thumbnailType;
			}

			boolean foundCurrent = false;
			for (String thumbType : thumbnailTypes) {
				if (thumbType.equals(thumbnailType)) {
					foundCurrent = true;
				}

				if (foundCurrent) {
					if (existsCacheFile(thumbType)) {
						return thumbType;
					}
				}
			}

			return thumbnailTypes[thumbnailTypes.length - 1]; // last one
		}

		private boolean existsCacheFile(String thumbnailType) {
			String tempUrl = getThumbnailUrl(url, thumbnailType);
			boolean exists = ImageCacheManager.containFileCache(tempUrl);

			logger.d("existsCacheFile: %s %s", exists, tempUrl);

			return exists;
		}

		@Override
		protected Void doInBackground(Void... params) {
			load();
			return null;
		}
	}
	
	public static class MaskData {
		private String maskKey;
		private Object maskObject;
		private int width;
		private int height;
		
		public MaskData(String maskKey, Object maskObject, int width, int height) {
			this.maskKey = maskKey;
			this.maskObject = maskObject;
			this.width = width;
			this.height = height;
		}

		public String getMaskKey() {
			return maskKey;
		}

		public void setMaskKey(String maskKey) {
			this.maskKey = maskKey;
		}

		public Object getMaskObject() {
			return maskObject;
		}

		public void setMaskObject(Object maskObject) {
			this.maskObject = maskObject;
		}

		public int getWidth() {
			return width;
		}

		public void setWidth(int width) {
			this.width = width;
		}

		public int getHeight() {
			return height;
		}

		public void setHeight(int height) {
			this.height = height;
		}
	}
}
