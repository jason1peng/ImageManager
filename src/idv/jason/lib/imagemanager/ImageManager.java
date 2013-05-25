/**
 * Copyright 2011 Jason Peng
 * This program is free software under the GNU General Public License.
 * If not, see <http://www.gnu.org/licenses/gpl.txt>.
 */

package idv.jason.lib.imagemanager;

import idv.jason.lib.imagemanager.model.UrlInfo;
import idv.jason.lib.imagemanager.tasks.ImageManagerThreadFactory;
import idv.jason.lib.imagemanager.db.DatabaseHelper;
import idv.jason.lib.imagemanager.db.ImageTable;
import idv.jason.lib.imagemanager.util.LifoAsyncTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

public class ImageManager implements ImageFileBasicOperation{
	private volatile static ImageManager mInstance;
	
	public static final String TAG = ImageManager.class.getSimpleName();

	private static boolean DEBUG = false;
	private static boolean DEBUG_CACHE = true;
	private static boolean DEBUG_BUFFER = false;
	private static boolean DEBUG_URL = false;
	
	private Context mContext;

	private File mDownloadPath = null;
	
	private ImageFactory mFactory;
	
	public static final String THREAD_FILTERS = "filters_thread";
	public static final String THREAD_MEDIASTORE = "mediastore_thread";
	public static final String THREAD_LOCAL = "local_thread";
	
	private ExecutorService mFilterThreadExecutor;
	private ExecutorService mMediaStoreThreadExecutor;
	private ExecutorService mLocalThreadExecutor;
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mWritableDb;

	private ImageManager(Context c) {
		mContext = c;
		
		mDbHelper = new DatabaseHelper(c);
		mWritableDb = mDbHelper.getWritableDatabase();
		
		mFactory = new ImageFactory();
		mFactory.setImageBasicOperation(this);
		mFilterThreadExecutor = Executors.newSingleThreadExecutor(new ImageManagerThreadFactory(THREAD_FILTERS));
		mMediaStoreThreadExecutor = Executors.newFixedThreadPool(2, new ImageManagerThreadFactory(THREAD_MEDIASTORE));
		mLocalThreadExecutor = Executors.newFixedThreadPool(3, new ImageManagerThreadFactory(THREAD_LOCAL));
		setDownloadPath(null);
	}
	
	public void setImageFactory(ImageFactory factory) {
		mFactory = factory;
		mFactory.setImageBasicOperation(this);
	}

	public void setDownloadPath(String path) {
		if (path != null && path.length() > 0) {
			mDownloadPath = new File(path);
		} else {
			mDownloadPath = new File(mContext.getCacheDir() + "/images");
		}

		if (!mDownloadPath.exists())
			mDownloadPath.mkdir();
	}

	public String getDownloadPath() {
		return mDownloadPath.getAbsolutePath();
	}

	public static ImageManager getInstance(Context c) {
		if (mInstance == null) {
			synchronized (ImageManager.class) {
				if (mInstance == null) {
					mInstance = new ImageManager(c);
				}
			}
		}
		return mInstance;
	}

	public Bitmap getImage(String host, String path, String params,
			ImageAttribute attr) {
		if (TextUtils.isEmpty(host) || TextUtils.isEmpty(path)) {
			return null;
		}
		resetPurgeTimer();

		String url = host + path;
		if (TextUtils.isEmpty(params) == false)
			url = url + "&" + params;

		return setupGetProcess(new UrlInfo(path, url), attr);
	}

	public Bitmap getImage(String url, ImageAttribute attr) {
		if (TextUtils.isEmpty(url)) {
			return null;
		}
		resetPurgeTimer();

		return setupGetProcess(new UrlInfo(url), attr);
	}
	
	public Bitmap getImageWithoutThread(String url, ImageAttribute attr) {
		return getImageWithoutThread(url, attr, true);
	}
	
	public Bitmap getImageWithoutThread(String url, ImageAttribute attr, boolean loadBitmap) {
		Bitmap bitmap = null;
		String id = getImageId(url, attr);
		
		if(loadBitmap) {
			if(id != null) {
				bitmap = getBitmapFromCache(id, attr);
			}
			if(bitmap == null) {
				if(DEBUG_CACHE)
					Log.d(TAG, "download new");
				bitmap = getBitmap(null, new UrlInfo(url), attr);
			} else {
				if(DEBUG_CACHE)
					Log.d(TAG, "exist in file");
			}
		} else {
			// just make sure bitmap exist
			if(id != null) {
				if(new File(mDownloadPath, id).exists() == false) {
					if(DEBUG_CACHE)
						Log.d(TAG, "download new");
					getBitmap(null, new UrlInfo(url), attr);
				} else {
					if(DEBUG_CACHE)
						Log.d(TAG, "exist in file");
				}
			}
		}
		return bitmap;
	}
	
	public Bitmap getMediaStoreImageThumbnail(String mediaStoreId, ImageAttribute attr) {
		Bitmap bitmap = null;
		UrlInfo info = new UrlInfo(MediaStoreImage.PREFIX + mediaStoreId);
		if(attr != null && attr.shouldLoadFromThread()) {
			getProcess(mediaStoreId, info, attr);
		} else {
			bitmap = getBitmapFromMediaStore(mediaStoreId, attr);
			removePotentialView(info.getUniquePath(), attr);
			doneProcess(info.getUniquePath(), attr, bitmap);
		}
		return bitmap;
	}
	
	// directly return bitmap if exist and don't need to handle it from thread
	private Bitmap setupGetProcess(UrlInfo url, ImageAttribute attr) {
		Bitmap bitmap = null;
		String id = getImageId(url.getUniquePath(), attr);
		if(id != null) {
			// exist, determine how to load bitmap
			if(attr != null && attr.shouldLoadFromThread()) {
				getProcess(id, url, attr);
				
			} else {
				removePotentialView(url.getUniquePath(), attr);
				bitmap = getBitmapFromCache(id, attr);
				if(bitmap != null)
					doneProcess(url.getUniquePath(), attr, bitmap);
				else
					getProcess(id, url, attr);
			}
		}
		else {
			// need download
			getProcess(id, url, attr);
		}
		return bitmap;
	}

	private void doneProcess(String path, ImageAttribute attr, Bitmap bitmap) {
		if (attr != null) {
			ImageView view = attr.getView();
			updateView(view, attr, bitmap);
			if(attr.mCallback != null) {
				attr.mCallback.imageDone(attr.mParam, bitmap);
				attr.mCallback = null;
			}
		}
	}
	
	private void updateView(ImageView view, ImageAttribute attr, Bitmap bitmap) {
		if (view != null) {
			if (bitmap != null) {
				if (attr.viewAttr != null)
					view.setScaleType(attr.viewAttr.doneScaleType);
				if (attr.shouldApplyWithAnimation())
					setImage(view, bitmap);
				else
					view.setImageBitmap(bitmap);
			} else {
				if (attr.viewAttr != null && attr.viewAttr.failResId != -1) {
					view.setScaleType(attr.viewAttr.failScaleType);
					view.setImageResource(attr.viewAttr.failResId);
				}
			}
		}
	}

	private Bitmap getDrawableBitmap(Drawable drawable, ImageAttribute attr) {
		String cacheIndex = getImageId(
				Integer.toString(attr.viewAttr.defaultResId), attr);
		Bitmap bm = getBitmapFromCache(cacheIndex, attr);
		if (bm == null) {
			if(DEBUG)
				Log.d(TAG, "getDrawableBitmap : generate");
			if (attr.getMaxHeight() == 0 || attr.getMaxWidth() == 0) {
				bm = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_4444);
				bm.eraseColor(Color.TRANSPARENT);
			} else {
				drawable.setBounds(0, 0, attr.getMaxWidth(), attr.getMaxHeight());
				bm = Bitmap.createBitmap(attr.getMaxWidth(), attr.getMaxHeight(),
						Bitmap.Config.ARGB_8888);
				bm.eraseColor(Color.TRANSPARENT);
				Canvas canvas = new Canvas(bm);
				drawable.draw(canvas);
			}
			setBitmapToCache(bm, cacheIndex);
		} else {
			if(DEBUG)
				Log.d(TAG, "getDrawableBitmap : use cache");
		}
		return bm;
	}

	private void getProcess(String id, UrlInfo url, ImageAttribute attr) {
		if (removePotentialView(url.getUniquePath(), attr)) {
			GetImageTask task = new GetImageTask(id, url, attr);
			ImageView view = null;
			if (attr != null) {
				view = attr.getView();
				if (view != null) {
					Bitmap bm = null;
					Drawable downloadedDrawable = null;
					if(attr.viewAttr.defaultResId != -1) {
						Drawable drawable = mContext.getResources().getDrawable(
								attr.viewAttr.defaultResId);
						if (drawable instanceof BitmapDrawable) {
							BitmapDrawable draw = (BitmapDrawable) drawable;
							bm = draw.getBitmap();
						} else {
							bm = getDrawableBitmap(drawable, attr);
						}

						view.setScaleType(attr.viewAttr.defaultScaleType);
						
						downloadedDrawable = new DownloadedBitmapDrawable(task, bm);
					} else if(attr.viewAttr.defaultColor != -1) {
						downloadedDrawable = new DownloadedColorDrawable(task, attr.viewAttr.defaultColor);
						
					} else {
						downloadedDrawable = new DownloadedBitmapDrawable(
								task, null);
					}
					
					view.setImageDrawable(downloadedDrawable);
				}
			}
			if(DEBUG_URL)
				Log.d(TAG, "getProcess: id=" + id + " url=" + url.getDownloadUrl());
			if(attr != null && attr.filterPhoto != 0 && id != null)
				task.executeOnExecutor(mFilterThreadExecutor, null, null, null);
			else if(url.isMediaStoreFile() && id != null) {
				task.executeOnExecutor(mMediaStoreThreadExecutor, null, null, null);
			} else if(url.isLocalFile()  && id != null) {
				task.executeOnExecutor(mLocalThreadExecutor, null, null, null);
			}
			else
				task.executeOnExecutor(LifoAsyncTask.LIFO_THREAD_POOL_EXECUTOR, null, null, null);
		}
	}

	class GetImageTask extends LifoAsyncTask<Void, Void, Bitmap> {
		private UrlInfo mUrl;
		private ImageAttribute mAttr;
		private boolean mSkipUpdateView = false;
		private String mImageId = null;
		
		public String getDownloadPath() {
			return mUrl.getUniquePath();
		}

		public GetImageTask(String id, UrlInfo url, ImageAttribute attr) {
			mImageId = id;
			mUrl = url;
			mAttr = attr;
		}

		public void setSkipUpdateView() {
			mSkipUpdateView = true;
		}

		@Override
		protected Bitmap doInBackground(Void... params) {
			
			return getBitmap(mImageId, mUrl, mAttr);
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			GetImageTask task = getTask(mAttr.getView());
			if(DEBUG)
				Log.d(TAG, "onPostExecute: " + mUrl.getUniquePath());
			if (task == this && mAttr != null && mAttr.getView() != null
					&& mSkipUpdateView == false) {
				if(DEBUG) {
					Log.d(TAG, "handle");
				}
				doneProcess(mUrl.getUniquePath(), mAttr, bitmap);
			} else {
				if(DEBUG) {
					Log.d(TAG, "skip");
					if(task != this)
						Log.d(TAG, "task target change");
					if(mAttr == null)
						Log.d(TAG, "mAttr == null");
					if(mAttr!=null && mAttr.getView() == null)
						Log.d(TAG, "imageview == null");
				}
			}
		}
	}
	
	private void setImage(ImageView image, Bitmap bitmap) {
		if (bitmap != null) {
			Drawable[] layers = new Drawable[2];
			layers[0] = new ColorDrawable(Color.TRANSPARENT);
			layers[1] = new BitmapDrawable(bitmap);
			TransitionDrawable drawable = new TransitionDrawable(layers);
			image.setImageDrawable(drawable);
			drawable.startTransition(300);
		}
	}
	
	private Bitmap getBitmap(String imageId, UrlInfo url, ImageAttribute attr) {
		Bitmap bitmap = null;
		try {
			if(DEBUG)
				Log.d(TAG, "handle image id = " + imageId);
			if(imageId != null) {
				// downloaded before, just need load it from thread
				if(url.isMediaStoreFile())
					bitmap = getBitmapFromMediaStore(imageId, attr);
				else {
					bitmap = getBitmapFromCache(imageId, attr);
					if(bitmap == null) {
						Log.d(TAG, "image [" + imageId + "] file been deleted");
						// if null means file been deleted from user, need process again
						mWritableDb.delete(ImageTable.TABLE_NAME, ImageTable.COLUMN_ID + "=?", new String[] {imageId});
					}
				}
			}
			
			if(imageId == null || bitmap==null) {
				BaseImage image = mFactory.getImage(mContext, url.getDownloadUrl(), attr);
				if(attr != null && attr.containsAttribute()) {
					// trying to get image without any modify
					String pureId = getImageId( url.getDownloadUrl(), null);
					bitmap = getBitmapFromCache(pureId, attr);
					if(bitmap != null) {
						if(DEBUG_CACHE)
							Log.d(TAG, "modify from origin");
						image.setBitmap(bitmap);
					}
					image = mFactory.postProcessImage(mContext,  url.getDownloadUrl(), attr, image);
				}
				if(bitmap == null) {
					if(DEBUG_CACHE)
						Log.d(TAG, "new");
				}
				bitmap = image.getBitmap();
				
				imageId = setBitmapToFile(bitmap, url.getUniquePath(), attr==null?null:attr.getStringAttr(), attr==null?false:attr.highQuality());

				setBitmapToCache(bitmap, imageId);
			}
		} catch (OutOfMemoryError e) {
			Log.e(TAG, "OutOfMemoryError", e);
		}
		
		return bitmap;
	}
	
	private Bitmap getBitmapFromMediaStore(String imageId, ImageAttribute attr) {

		synchronized (sMemoryCache) {
			final Bitmap bitmap = sMemoryCache.get(MediaStoreImage.PREFIX + imageId);
			if (bitmap != null) {
				if (DEBUG_CACHE)
					Log.d(TAG, "memory cache");
				// Bitmap found in hard cache
				// Move element to first position, so that it is removed last
				sMemoryCache.remove(MediaStoreImage.PREFIX + imageId);
				sMemoryCache.put(MediaStoreImage.PREFIX + imageId, bitmap);
				return bitmap;
			}
		}
		MediaStoreImage image = new MediaStoreImage(mContext, MediaStoreImage.PREFIX + imageId);
		try {
			Bitmap bm = image.getBitmap();
			if (bm != null) {
				if (DEBUG_CACHE)
					Log.d(TAG, "media store cache");
				synchronized (sMemoryCache) {
					sMemoryCache.put(MediaStoreImage.PREFIX + imageId, bm);
				}
				return bm;
			}
		} catch (OutOfMemoryError e) {
			Log.e(TAG, "OutOfMemoryError", e);
		}
		return null;
	}

	private Bitmap getBitmapFromCache(String cacheIndex, ImageAttribute attr) {
		if(TextUtils.isEmpty(cacheIndex))
			return null;
		
		// First try the memory cache
		synchronized (sMemoryCache) {
			final Bitmap bitmap = sMemoryCache.get(cacheIndex);
			if (bitmap != null) {
				if (DEBUG_CACHE)
					Log.d(TAG, "memory cache");
				// Bitmap found in hard cache
				// Move element to first position, so that it is removed last
				sMemoryCache.remove(cacheIndex);
				sMemoryCache.put(cacheIndex, bitmap);
				return bitmap;
			}
		}

		File file = new File(mDownloadPath, cacheIndex);
		if (file.exists()) {
			LocalImage image = null;
			image = new LocalImage(mContext, file.getAbsolutePath());
			if(attr != null && attr.maxHeight != 0 && attr.maxWidth != 0) {
				image.setImaggMaxSize(attr.maxWidth, attr.maxHeight);
				image.setHighQuality(attr.highQuality());
			}
			try {
				Bitmap bm = image.getBitmap();
				if (bm != null) {
					if (DEBUG_CACHE)
						Log.d(TAG, "file cache");
					synchronized (sMemoryCache) {
						sMemoryCache.put(cacheIndex, bm);
					}
					return bm;
				}
			} catch (OutOfMemoryError e) {
				Log.e(TAG, "OutOfMemoryError", e);
			}
		}
		return null;
	}

	private void setBitmapToCache(Bitmap bitmap, String cacheIndex) {
		if(bitmap == null)
			return;
		if (DEBUG) {
			Log.d(TAG, "add filename:" + cacheIndex);
		}
		synchronized (sMemoryCache) {
			if(sMemoryCache.get(cacheIndex) == null) {
				sMemoryCache.put(cacheIndex, bitmap);
			} else {
				sMemoryCache.remove(cacheIndex);
				sMemoryCache.put(cacheIndex, bitmap);
			}
		}
	}
	
	public String setBitmapToFile(Bitmap bitmap, String url, String attr, boolean hasAlpha) {
		if(bitmap == null)
			return null;
		
		String id = Long.toString(setImageExist(url, attr));

		File file = new File(mDownloadPath, id);
		FileOutputStream out;
		try {
			out = new FileOutputStream(file);
			if(hasAlpha)
				bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
			else
				bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
		} catch (FileNotFoundException e) {
			Log.e(TAG, e.getMessage());
		}
		return id;
	}
	
	public String getImageId(String url, ImageAttribute attr) {
		String id = null;
		Cursor c = null;
		
		if(attr == null || attr.containsAttribute() == false) {
			c = mWritableDb.query(ImageTable.TABLE_NAME, 
					new String[] { ImageTable.COLUMN_ID }, 
					ImageTable.COLUMN_IMAGE_URL + " =? AND " + ImageTable.COLUMN_ATTRIBUTE + " =? ",
					new String[] { url, "NULL" }, null, null, ImageTable.DEFAULT_SORT_ORDER);
		} else {
			String imageId = MediaStoreImage.getId(url);
			if(imageId != null) {
				id = imageId;
			} else {
				c = mWritableDb.query(ImageTable.TABLE_NAME, 
						new String[] { ImageTable.COLUMN_ID }, 
						ImageTable.COLUMN_IMAGE_URL + " =? AND " + ImageTable.COLUMN_ATTRIBUTE + " =? ", 
						new String[] { url , attr.getStringAttr() }, null, null, ImageTable.DEFAULT_SORT_ORDER);	
			}
		}
		if(c != null) {
			if(c.getCount() > 0) {
				c.moveToFirst();
				id = c.getString(0);
			}
			c.close();
		}
		return id;
	}
	
	public long setImageExist(String url, String attr) {
		ContentValues cv = new ContentValues();
		cv.put(ImageTable.COLUMN_IMAGE_URL, url);
		cv.put(ImageTable.COLUMN_ATTRIBUTE, TextUtils.isEmpty(attr)?"NULL":attr);
		cv.put(ImageTable.COLUMN_ACCESS_TIME, Calendar.getInstance().getTime().toGMTString());
		return mWritableDb.insert(ImageTable.TABLE_NAME, "", cv);
	}

	public interface ImageDoneCallback {
		public void imageDone(Object id, Bitmap bitmap);
	}

	private static final int HARD_CACHE_CAPACITY = 10 * 1024 * 1024;
	private static final int DELAY_BEFORE_PURGE = 10 * 1000; // in milliseconds

	// Hard cache, with a fixed maximum capacity and a life duration
	private final LruCache<String, Bitmap> sMemoryCache = new LruCache<String, Bitmap>(
			HARD_CACHE_CAPACITY) {
		protected int sizeOf(String key, Bitmap value) {
			int size = (value.getRowBytes() * value.getHeight());
			if (DEBUG_BUFFER)
				Log.d(TAG, "current size:" + size + " - " + this.size() + "/"
						+ HARD_CACHE_CAPACITY);
			return size;

		}

		protected void entryRemoved(boolean evicted, String key,
				Bitmap oldValue, Bitmap newValue) {
			if (DEBUG_BUFFER)
				Log.d(TAG, "entryRemoved:" + this.size() + "/"
						+ HARD_CACHE_CAPACITY);
		}
	};

	private final Handler purgeHandler = new Handler();
	private final Runnable purger = new Runnable() {
		@Override
		public void run() {
			clearCache();
		}
	};

	public void clearCache() {
		sMemoryCache.evictAll();
	}

	private void resetPurgeTimer() {
		purgeHandler.removeCallbacks(purger);
		purgeHandler.postDelayed(purger, DELAY_BEFORE_PURGE);
	}

	/**
	 * A fake Drawable that will be attached to the imageView while the download
	 * is in progress.
	 * 
	 * <p>
	 * Contains a reference to the actual download task, so that a download task
	 * can be stopped if a new binding is required, and makes sure that only the
	 * last started download process can bind its result, independently of the
	 * download finish order.
	 * </p>
	 */
	public static class DownloadedBitmapDrawable extends BitmapDrawable {
		private final WeakReference<GetImageTask> bitmapGetImageTaskReference;

		public DownloadedBitmapDrawable(GetImageTask bitmapDownloaderTask, Bitmap bm) {
			super(bm);
			bitmapGetImageTaskReference = new WeakReference<GetImageTask>(
					bitmapDownloaderTask);
		}

		public GetImageTask getTask() {
			return bitmapGetImageTaskReference.get();
		}
	}
	
	public static class DownloadedColorDrawable extends ColorDrawable {
		private final WeakReference<GetImageTask> bitmapGetImageTaskReference;

		public DownloadedColorDrawable(GetImageTask bitmapDownloaderTask, int color) {
			super(color);
			bitmapGetImageTaskReference = new WeakReference<GetImageTask>(
					bitmapDownloaderTask);
		}

		public GetImageTask getTask() {
			return bitmapGetImageTaskReference.get();
		}
	}

	/**
	 * Returns true if the current download has been canceled or if there was no
	 * download in progress on this image view. Returns false if the download in
	 * progress deals with the same url. The download is not stopped in that
	 * case.
	 */
	private static boolean removePotentialView(String path, ImageAttribute attr) {
		ImageView view = null;
		if (attr != null)
			view = attr.getView();
		GetImageTask task = getTask(view);
		if (task != null) {
			if (!task.getDownloadPath().equals(path)) {
				if (DEBUG) {
					Log.d(TAG, "should skip");
				}
				task.setSkipUpdateView();
			} else
				return false;
		}
		return true;
	}

	/**
	 * @param imageView
	 *            Any imageView
	 * @return Retrieve the currently active download task (if any) associated
	 *         with this imageView. null if there is no such task.
	 */
	private static GetImageTask getTask(ImageView imageView) {
		if (imageView != null) {
			Drawable drawable = imageView.getDrawable();
			if (drawable instanceof DownloadedBitmapDrawable) {
				DownloadedBitmapDrawable downloadedDrawable = (DownloadedBitmapDrawable) drawable;
				return downloadedDrawable.getTask();
			} else if (drawable instanceof DownloadedColorDrawable) {
				DownloadedColorDrawable downloadedDrawable = (DownloadedColorDrawable) drawable;
				return downloadedDrawable.getTask();
			}
		}
		return null;
	}
}
