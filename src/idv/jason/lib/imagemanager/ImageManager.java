/**
 * Copyright 2011 Jason Peng
 * This program is free software under the GNU General Public License.
 * If not, see <http://www.gnu.org/licenses/gpl.txt>.
 */

package idv.jason.lib.imagemanager;

import idv.jason.lib.imagemanager.tasks.ImageManagerThreadFactory;
import idv.jason.lib.imagemanager.util.LifoAsyncTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;

public class ImageManager implements ImageFileBasicOperation{
	private volatile static ImageManager mInstance;
	
	public static final String TAG = ImageManager.class.getSimpleName();

	private static boolean DEBUG = false;
	private static boolean DEBUG_CACHE = false;
	
	public static final int DEBUG_FLAG_ALL 		= 1;
	public static final int DEBUG_FLAG_NORMAL 	= 2;
	public static final int DEBUG_FLAG_URL 		= 4;
	public static final int DEBUG_FLAG_CACHE 	= 8;

	private static final int IMAGE_MANAGER_CALLBACK = 100;
	
	private Context mContext;

	private File mDownloadPath = null;
	
	private ImageFactory mFactory;
	
	private boolean mSaveOrigin = false;
	
	public static final String THREAD_FILTERS = "filters_thread";
	public static final String THREAD_MEDIASTORE = "mediastore_thread";
	
	private ExecutorService mSingleThreadExecutor;
	private ExecutorService mLocalThreadExecutor;

	private ImageManager(Context c) {
		mContext = c;
		mDoneCallbackList = new ArrayList<ImageDoneCallback>();
		mDownloadedCallbackList = new ArrayList<ImageDownloadedCallback>();
		mFactory = new ImageFactory();
		mFactory.setImageBasicOperation(this);
		mSingleThreadExecutor = Executors.newSingleThreadExecutor(new ImageManagerThreadFactory(THREAD_FILTERS));
		mLocalThreadExecutor = Executors.newFixedThreadPool(5, new ImageManagerThreadFactory(THREAD_MEDIASTORE));
		setDownloadPath(null);
	}
	
	public void setDebugFlag(int flag) {
		if((flag & DEBUG_FLAG_ALL) == DEBUG_FLAG_ALL)
			DEBUG = DEBUG_CACHE = true;
		if((flag & DEBUG_FLAG_NORMAL) == DEBUG_FLAG_NORMAL)
			DEBUG = true;
		if((flag & DEBUG_FLAG_CACHE) == DEBUG_FLAG_CACHE)
			DEBUG_CACHE = true;
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

	public static String getHashString(String input) {
		String str = Base64.encodeToString((input).getBytes(), Base64.URL_SAFE);
		if (str.contains("\n"))
			str = str.replace("\n", "");

		return str;
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

	public static String getImageId(String url, ImageAttribute attr) {
		return getHashString(url + (attr == null ? "" : attr.getStringAttr()));
	}

	public Bitmap getImage(String host, String path, String params,
			ImageAttribute attr) {
		if (TextUtils.isEmpty(host) || TextUtils.isEmpty(path)) {
			return null;
		}
		resetPurgeTimer();
		String id = getImageId(path, attr);

		String url = host + path;
		if (TextUtils.isEmpty(params) == false)
			url = url + "&" + params;

		return setupGetProcess(id, path, url, attr);
	}

	public Bitmap getImage(String url, ImageAttribute attr) {
		if (TextUtils.isEmpty(url)) {
			return null;
		}
		resetPurgeTimer();
		String id = getImageId(url, attr);

		return setupGetProcess(id, null, url, attr);
	}
	
	public Bitmap getMediaStoreImageThumbnail(String id, ImageAttribute attr) {
		Bitmap bitmap = null;
		if(attr != null && attr.shouldLoadFromThread()) {
			getProcess(id, null, MediaStoreImage.PREFIX + id, attr);
			
		} else {
			removePotentialView(id, attr);
			doneProcess(id, attr, bitmap);
		}
		return bitmap;
	}
	
	// directly return bitmap if exist and don't need to handle it from thread
	private Bitmap setupGetProcess(String id, String path, String url, ImageAttribute attr) {
		Bitmap bitmap = null;
		if(isImageExist(id)) {
			// exist, determine how to load bitmap
			if(attr != null && attr.shouldLoadFromThread()) {
				getProcess(id, path, url, attr);
				
			} else {
				removePotentialView(id, attr);
				bitmap = getBitmapFromCache(id, attr);
				doneProcess(id, attr, bitmap);
			}
		}
		else {
			// need download
			getProcess(id, path, url, attr);
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
			if (attr.maxHeight == 0 || attr.maxWidth == 0) {
				bm = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_4444);
				bm.eraseColor(Color.TRANSPARENT);
			} else {
				drawable.setBounds(0, 0, attr.maxWidth, attr.maxHeight);
				bm = Bitmap.createBitmap(attr.maxWidth, attr.maxHeight,
						Bitmap.Config.ARGB_8888);
				bm.eraseColor(Color.TRANSPARENT);
				Canvas canvas = new Canvas(bm);
				drawable.draw(canvas);
			}
			setBitmapToCache(bm, cacheIndex, false, false);
		} else {
			if(DEBUG)
				Log.d(TAG, "getDrawableBitmap : use cache");
		}
		return bm;
	}

	private void getProcess(String id, String path, String url,
			ImageAttribute attr) {
		if (removePotentialView(id, attr)) {
			GetImageTask task = new GetImageTask(id, path, url, attr);
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
			Log.d(TAG, "url:" + url);
			if(attr != null && attr.filterPhoto != 0 && isImageExist(id) == false)
				task.executeOnExecutor(mSingleThreadExecutor, null, null, null);
			else if(url.contains(MediaStoreImage.PREFIX) || url.contains(LocalImage.LOCAL_FILE_PREFIX) || isImageExist(id)) {
				Log.d(TAG, "local thread");
				task.executeOnExecutor(mLocalThreadExecutor, null, null, null);
			}
			else
				task.executeOnExecutor(LifoAsyncTask.LIFO_THREAD_POOL_EXECUTOR, null, null, null);
		}
	}

	class GetImageTask extends LifoAsyncTask<Void, Void, Bitmap> {
		private String mUrl;
		private ImageAttribute mAttr;
		private String mId;
		private boolean mSkipUpdateView = false;
		private String mPath;

		public GetImageTask(String id, String path, String url,
				ImageAttribute attr) {
			mUrl = url;
			mAttr = attr;
			mId = id;
			mPath = path;
		}

		public String getTaskImageId() {
			return mId;
		}

		public void setSkipUpdateView() {
			mSkipUpdateView = true;
		}

		@Override
		protected Bitmap doInBackground(Void... params) {
			Bitmap bitmap = null;
			try {
				// origin bitmap without any attribute
				String id = null;
				if(mPath == null)
					id = getImageId(mUrl, null);
				else
					id = getImageId(mPath, null);
				
				BaseImage image = mFactory.getImage(mContext, mUrl, mAttr);
				// check again before processing, there are possibility that bitmap exist
				// but we need to handle it from thread
				Bitmap originBitmap = null;
				Bitmap diskBitmap = getBitmapFromCache(mId, mAttr);
				if (diskBitmap == null) {
					// first trying to get origin image, if exist, no need download again
					originBitmap = getBitmapFromCache(id, null);
					image.setBitmap(originBitmap);
				} else {
					image.setBitmap(diskBitmap);
					return diskBitmap;
				}
								
				// optional: save origin file to seed up the modification process for the same image
				if(mSaveOrigin && diskBitmap == null && originBitmap == null && mAttr != null) {
					setBitmapToFile(image.getBitmap(), id, mAttr.hasAlpha);
				}
				
				// process attribute setting
				if(mAttr != null) {
					image = mFactory.postProcessImage(mContext, mUrl, mAttr, image);
					bitmap = image.getBitmap();
				}
	
				setBitmapToCache(bitmap, mId, true, mAttr==null?false:mAttr.hasAlpha);
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
						Log.e(TAG, "GetImageTask : " + mId + " is null(url=" + mUrl + ")");
					else
						Log.e(TAG, "GetImageTask : " + mId + " is null(url=" + mUrl
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

			ImageManagerCallback(onDownloadedCallback(mId, mUrl));
			ImageManagerCallback(onDoneCallback(mId, bitmap));
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

	private Bitmap getBitmapFromCache(String cacheIndex, ImageAttribute attr) {
		// First try the hard reference cache
		synchronized (sHardBitmapCache) {
			final Bitmap bitmap = sHardBitmapCache.get(cacheIndex);
			if (bitmap != null) {
				if (DEBUG_CACHE)
					Log.d(TAG, "memory cache");
				// Bitmap found in hard cache
				// Move element to first position, so that it is removed last
				sHardBitmapCache.remove(cacheIndex);
				sHardBitmapCache.put(cacheIndex, bitmap);
				return bitmap;
			}
		}

		File file = new File(mDownloadPath, cacheIndex);
		if (file.exists()) {
			LocalImage image = null;
			if(attr != null && attr.maxHeight != 0 && attr.maxWidth != 0) {
				image = new LocalImage(mContext, file.getAbsolutePath(), attr.maxWidth, attr.maxHeight);
				if(attr.highQuality == true)
					image.setHighQuality(true);
			} else
				image = new LocalImage(mContext, file.getAbsolutePath());
			try {
				Bitmap bm = image.getBitmap();
				if (bm != null) {
					if (DEBUG_CACHE)
						Log.d(TAG, "file cache");
					synchronized (sHardBitmapCache) {
						sHardBitmapCache.put(cacheIndex, bm);
					}
					return bm;
				}
			} catch (OutOfMemoryError e) {
				Log.e(TAG, "OutOfMemoryError", e);
			}
		}
		return null;
	}

	private void setBitmapToCache(Bitmap bitmap, String cacheIndex, boolean savefile, boolean hasAlpha) {
		if(bitmap == null)
			return;
		if (DEBUG) {
			Log.d(TAG, "add filename:" + cacheIndex);
		}
		synchronized (sHardBitmapCache) {
			sHardBitmapCache.put(cacheIndex, bitmap);
		}

		if(savefile)
			setBitmapToFile(bitmap, cacheIndex, hasAlpha);
	}
	
	public void setBitmapToFile(Bitmap bitmap, String cacheIndex, boolean hasAlpha) {
		if(bitmap == null)
			return;

		File file = new File(mDownloadPath, cacheIndex);
		if(file.exists()) {
			if(DEBUG)
				Log.d(TAG, "file exist, ignore save");
		} else {
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
		}
	}
	
	public boolean isImageExist(String id) {
		return new File(mDownloadPath, id).exists();
	}
	
	public String getImagePath(String id) {
		return new File(mDownloadPath, id).getAbsolutePath();
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
	private final LruCache<String, Bitmap> sHardBitmapCache = new LruCache<String, Bitmap>(
			HARD_CACHE_CAPACITY) {
		protected int sizeOf(String key, Bitmap value) {
			int size = (value.getRowBytes() * value.getHeight());
			if (DEBUG)
				Log.d(TAG, "current size:" + size + " - " + this.size() + "/"
						+ HARD_CACHE_CAPACITY);
			return size;

		}

		protected void entryRemoved(boolean evicted, String key,
				Bitmap oldValue, Bitmap newValue) {
			if (DEBUG)
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
		sHardBitmapCache.evictAll();
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
	private static boolean removePotentialView(String id, ImageAttribute attr) {
		ImageView view = null;
		if (attr != null)
			view = attr.getView();
		GetImageTask task = getTask(view);
		if (task != null) {
			if (!task.mId.equals(id)) {
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
