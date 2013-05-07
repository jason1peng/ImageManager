/**
 * Copyright 2011 Jason Peng
 * This program is free software under the GNU General Public License.
 * If not, see <http://www.gnu.org/licenses/gpl.txt>.
 */

package idv.jason.lib.imagemanager;

import idv.jason.lib.imagemanager.tasks.ImageManagerThreadFactory;
import idv.jason.lib.imagemanager.db.DatabaseHelper;
import idv.jason.lib.imagemanager.db.ImageTable;
import idv.jason.lib.imagemanager.util.LifoAsyncTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;

public class ImageManager implements ImageFileBasicOperation{
	private volatile static ImageManager mInstance;
	
	public static final String TAG = ImageManager.class.getSimpleName();

	private static boolean DEBUG = false;
	private static boolean DEBUG_CACHE = false;
	private static boolean DEBUG_BUFFER = false;
	private static boolean DEBUG_URL = false;
	
	private static final int IMAGE_MANAGER_CALLBACK = 100;
	
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
		
		mDoneCallbackList = new ArrayList<ImageDoneCallback>();
		mDownloadedCallbackList = new ArrayList<ImageDownloadedCallback>();
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
	
	class UrlInfo {
		String path;
		String url;
		String imageId;
		
		public UrlInfo(String url) {
			this.path = null;
			this.url = url;
		}
		
		public UrlInfo(String path, String url) {
			this.path = path;
			this.url = url;
		}
		
		public void setImageId(String id) {
			imageId = id;
		}
		
		public String getDownloadUrl() {
			return url;
		}
		
		public String getUniquePath() {
			if(path == null)
				return url;
			else
				return path;
		}
		
		public String getImageId() {
			return imageId;
		}
		
		public boolean isLocalFile() {
			return url.contains(LocalImage.LOCAL_FILE_PREFIX);
		}
		
		public boolean isMediaStoreFile() {
			return url.contains(MediaStoreImage.PREFIX);
		}
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
	
	public Bitmap getMediaStoreImageThumbnail(String mediaStoreId, ImageAttribute attr) {
		Bitmap bitmap = null;
		UrlInfo info = new UrlInfo(MediaStoreImage.PREFIX + mediaStoreId);
		if(attr != null && attr.shouldLoadFromThread()) {
			getProcess(mediaStoreId, info, attr);
		} else {
			removePotentialView(info.getUniquePath(), attr);
			doneProcess(mediaStoreId, attr, bitmap);
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
					doneProcess(id, attr, bitmap);
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

	private void doneProcess(String id, ImageAttribute attr, Bitmap bitmap) {
		if (attr != null) {
			ImageView view = attr.getView();
			if (view != null) {
				if(attr.viewAttr != null)
					view.setScaleType(attr.viewAttr.doneScaleType);
				view.setImageBitmap(bitmap);
				if (attr.viewAttr.backgroundResId != -1)
					view.setBackgroundResource(attr.viewAttr.backgroundResId);
			} else if (view == null) {
			}
			if(attr.mCallback != null) {
				ImageManagerCallback(onIndividualDoneCallback(attr.mCallback, id, bitmap));
				attr.mCallback = null;
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
			Bitmap bitmap = null;
			try {
				if(DEBUG)
					Log.d(TAG, "handle image id = " + mImageId);
				if(mImageId != null) {
					// downloaded before, just need load it from thread
					if(mUrl.isMediaStoreFile())
						bitmap = getBitmapFromMediaStore(mImageId, mAttr);
					else {
						bitmap = getBitmapFromCache(mImageId, mAttr);
						if(bitmap == null) {
							Log.d(TAG, "image [" + mImageId + "] file been deleted");
							// if null means file been deleted from user, need process again
							mWritableDb.delete(ImageTable.TABLE_NAME, ImageTable.COLUMN_ID + "=?", new String[] {mImageId});
						}
					}
				}
				
				if(mImageId == null || bitmap==null) {
					BaseImage image = mFactory.getImage(mContext, mUrl.getDownloadUrl(), mAttr);
					if(mAttr != null && mAttr.containsAttribute()) {
						// trying to get image without any modify
						String pureId = getImageId( mUrl.getDownloadUrl(), null);
						image.setBitmap(getBitmapFromCache(pureId, mAttr));
						image = mFactory.postProcessImage(mContext,  mUrl.getDownloadUrl(), mAttr, image);
					}
					bitmap = image.getBitmap();
					
					mImageId = setBitmapToFile(bitmap, mUrl.getUniquePath(), mAttr==null?null:mAttr.getStringAttr(), mAttr==null?false:mAttr.hasAlpha());

					setBitmapToCache(bitmap, mImageId);
				}
			} catch (OutOfMemoryError e) {
				Log.e(TAG, "OutOfMemoryError", e);
			}
			return bitmap;
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if (bitmap == null) {
				if(DEBUG) {
					if (mAttr == null)
						Log.e(TAG, "GetImageTask : " + mImageId + " is null(url=" + mUrl + ")");
					else
						Log.e(TAG, "GetImageTask : " + mImageId + " is null(url=" + mUrl
								+ " attr=" + mAttr.getStringAttr() + ").");
				}
				updateView(null);
			} else {
				if (mAttr != null && mAttr.getView() != null
						&& mSkipUpdateView == false) {
					if (DEBUG)
						Log.d(TAG, "skip view");
					updateView(bitmap);
				}
			}

			ImageManagerCallback(onDownloadedCallback(mImageId, mUrl.getDownloadUrl()));
			ImageManagerCallback(onDoneCallback(mImageId, bitmap));
		}

		private void updateView(Bitmap bitmap) {
			ImageView view = mAttr.getView();
			if (view != null) {
				GetImageTask task = getTask(view);
				if (task == this) {
					if(bitmap != null) {
						if(mAttr.viewAttr != null)
							view.setScaleType(mAttr.viewAttr.doneScaleType);
						if (mAttr.shouldApplyWithAnimation())
							setImage(view, bitmap);
						else
							view.setImageBitmap(bitmap);
					} else {
						if(mAttr.viewAttr != null && mAttr.viewAttr.failResId != -1) {
							view.setScaleType(mAttr.viewAttr.failScaleType);
							view.setImageResource(mAttr.viewAttr.failResId);
						}
					}
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

	private static Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case IMAGE_MANAGER_CALLBACK:
				((Runnable) msg.obj).run();
				break;
			}
		}
	};

	private static ArrayList<ImageDoneCallback> mDoneCallbackList;
	private static ArrayList<ImageDownloadedCallback> mDownloadedCallbackList;

	public interface ImageDoneCallback {
		public void imageDone(String id, Bitmap bitmap);
	}
	
	public interface ImageDownloadedCallback {
		public void imageDonwnloaded(String id, String url);
	}

	protected void ImageManagerCallback(Runnable callback) {
		if (callback == null) {
			throw new NullPointerException();
		}
		Message message = Message.obtain();
		message.what = IMAGE_MANAGER_CALLBACK;
		message.obj = callback;
		mHandler.sendMessage(message);
	}
	
	protected Runnable onIndividualDoneCallback(final ImageDoneCallback callback, final String id, final Bitmap bitmap) {
		return new Runnable() {
			public void run() {
				callback.imageDone(id, bitmap);
			}
		};
	}

	protected Runnable onDoneCallback(final String id, final Bitmap bitmap) {
		return new Runnable() {
			public void run() {
				if (mDoneCallbackList != null) {
					for (int i = 0; i < mDoneCallbackList.size(); ++i) {
						ImageDoneCallback cb = mDoneCallbackList.get(i);
						cb.imageDone(id, bitmap);
					}
				}
			}
		};
	}
	
	protected Runnable onDownloadedCallback(final String id, final String url) {
		return new Runnable() {
			public void run() {
				if (mDownloadedCallbackList != null) {
					for (int i = 0; i < mDownloadedCallbackList.size(); ++i) {
						ImageDownloadedCallback cb = mDownloadedCallbackList.get(i);
						cb.imageDonwnloaded(id, url);
					}
				}
			}
		};
	}

	public void registerDoneCallback(ImageDoneCallback cb) {
		if (mDoneCallbackList != null && cb != null) {
			mDoneCallbackList.add(cb);
		}
	}

	public void removeDoneCallback(ImageDoneCallback cb) {
		if (mDoneCallbackList != null) {
			if (mDoneCallbackList.indexOf(cb) >= 0)
				mDoneCallbackList.remove(cb);
		}
	}
	
	public void registerDownloadedCallback(ImageDownloadedCallback cb) {
		if (mDownloadedCallbackList != null && cb != null) {
			mDownloadedCallbackList.add(cb);
		}
	}

	public void removeDownloadedCallback(ImageDownloadedCallback cb) {
		if (mDownloadedCallbackList != null) {
			if (mDownloadedCallbackList.indexOf(cb) >= 0)
				mDownloadedCallbackList.remove(cb);
		}
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
