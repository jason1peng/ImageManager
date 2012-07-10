/**
 * Copyright 2011 Jason Peng
 * This program is free software under the GNU General Public License.
 * If not, see <http://www.gnu.org/licenses/gpl.txt>.
 */

package com.jason.lib.imagemanager.image;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;

public class ImageManager {
	private volatile static ImageManager mInstance;
	public static final String TAG = ImageManager.class.getSimpleName();
	private static final boolean DEBUG = true;
	private static final boolean DEBUG_URL = true;
	
	private static final int IMAGE_MANAGER_CALLBACK = 100;
	private static final int CONCURRENT_GET_IMAGE_TASK_COUNT = 2;
	private Context mContext;
	
	private String mDownloadPath = null;
	
	private Stack<GetImageTask> mTaskStack = new Stack<GetImageTask>();
	private ArrayList<GetImageTask> mRunningTask = new ArrayList<GetImageTask>(CONCURRENT_GET_IMAGE_TASK_COUNT);

	private ImageManager(Context c) {
		mContext = c;
		mCallbackList = new ArrayList<ImageManagerCallback>();
	}
	
	public void setDownloadPath(String path) {
		if(path != null && path.length() > 0) {
			mDownloadPath = path;
		}
	}
	
	public String getDownloadPath() {
		String path = mDownloadPath;
		if(path == null) {
			path = mContext.getCacheDir() + "/image";
		}
		return path;
	}
	
	public static String getHashString(String input) {
    	String str = Base64.encodeToString((input).getBytes(), Base64.URL_SAFE);
    	if(str.contains("\n"))
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
		return getHashString(url+(attr==null?"":attr.getStringAttr()));
	}
	
	public Bitmap getImage(String url, ImageAttribute attr) {
		if (TextUtils.isEmpty(url)) {
        	return null;
        }
		resetPurgeTimer();
		String id = getImageId(url, attr);
		
		Bitmap bitmap = getBitmapFromCache(id);
		if(bitmap == null) {
        	synchronized(mTaskStack) {
				getProcess(id, url, attr);
			}
		}
		else {
			removePotentialView(id, attr);
			if(DEBUG_URL) {
				Log.d(TAG, "The url already in done");
				Log.d(TAG, "url:"+ url);
			}
			doneProcess(id, attr, bitmap);
		}
		
		return bitmap;
	}
	
	private void doneProcess(String id, ImageAttribute attr, Bitmap bitmap) {
		if(attr != null) {
			ImageView view = attr.getView();
			if(view != null) {
				view.setImageBitmap(bitmap);
				if(attr.viewAttr.backgroundResId != -1)
					view.setBackgroundResource(attr.viewAttr.backgroundResId);
			}
			else if(view == null) {
			}
		}
		ImageManagerCallback(onDoneCallback(id, bitmap));
	}
	
	private void getProcess(String id, String url, ImageAttribute attr) {
		if(removePotentialView(id, attr)) {
			GetImageTask task = new GetImageTask(url, attr);
			ImageView view = null;
			if(attr != null) {
				view = attr.getView();
				if(view != null) {
					DownloadedDrawable drawable = null;
					if(attr.viewAttr.defaultResId != -1) {
						BitmapDrawable draw = (BitmapDrawable)mContext.getResources().getDrawable(attr.viewAttr.defaultResId);
						Bitmap bm = draw.getBitmap();
						drawable = new DownloadedDrawable(task, bm);
					}
					else {
						drawable = new DownloadedDrawable(task, null);
					}
					
		    		view.setImageDrawable(drawable);
		    	}
			}
			if(mRunningTask.size() < CONCURRENT_GET_IMAGE_TASK_COUNT) {
				mRunningTask.add(task);
				task.execute();
				if(DEBUG_URL) {
					Log.d(TAG, "start get image task, running task count=" + mRunningTask.size());
					Log.d(TAG, "url:" + url);
				} 
			}
			else {
				mTaskStack.push(task);
				if(DEBUG_URL) {
					Log.d(TAG, "add to stack, running task count=" + mRunningTask.size() + " pending:" + mTaskStack.size());
					Log.d(TAG, "url:" + url);
				}
			}
		}
	}
	
	class GetImageTask extends AsyncTask<Void, Void, Bitmap> {
		private String mUrl;
		private ImageAttribute mAttr;
		private String mId;
		private boolean mSkipUpdateView = false;
		
		public GetImageTask(String url, ImageAttribute attr) {
			mUrl = url;
			mAttr = attr;
			mId = getImageId(mUrl, mAttr);
		}
		
		public String getTaskImageId() {
			return mId;
		}
		
		public void setSkipUpdateView() {
			mSkipUpdateView = true;
		}

		@Override
		protected Bitmap doInBackground(Void... params) {
			BaseImage image = ImageFactory.getImage(mContext, mUrl, mAttr);
			// check again before processing
			Bitmap bitmap = getBitmapFromCache(mId);
			if(bitmap == null)
				bitmap = image.getBitmap();
			if(DEBUG_URL) {
				Log.d(TAG, "image process done");
				Log.d(TAG, "url:"+mUrl);
			}
			if(bitmap != null)
				setBitmapToCache(bitmap, mId);
			return bitmap;
		}
		
		@Override
        protected void onPostExecute(Bitmap bitmap) {
			decreaseTask();
			if(bitmap == null) {
				Log.e(TAG, "GetImageTask : image is null(url="+mUrl+
						mAttr==null?"":" , attr=" + mAttr.getStringAttr()+").");
				return;
			}
			if(mAttr != null && mAttr.getView() != null && mSkipUpdateView == false) {
				if(DEBUG)
					Log.d(TAG, "skip view");
				updateView(bitmap);
			}
			
			ImageManagerCallback(onDoneCallback(getImageId(mUrl, mAttr), bitmap));
		}
		
		@Override
		protected void onCancelled() {
        	synchronized(mTaskStack) {
        		if(this.getStatus() == AsyncTask.Status.RUNNING)
        			mRunningTask.remove(this);
            	if(DEBUG)
					Log.d(TAG, "canceled, running task count=" + mRunningTask.size()
							+ " pending:" + mTaskStack.size());
            	checkNextTask();
			}
        }
		
		private void updateView(Bitmap bitmap) {
			ImageView view = mAttr.getView();
			if(view != null) {
				GetImageTask task = getTask(view);
				if(task == this)
					view.setImageBitmap(bitmap);
			}
		}
	    
	    private void decreaseTask() {
	    	synchronized (mTaskStack) {
	    		if(this.isCancelled() == false) {
					mRunningTask.remove(this);
					if(DEBUG)
						Log.d(TAG, "done task, running task count="+ mRunningTask.size()
								+" pending:" + mTaskStack.size());
					checkNextTask();
	    		}
			}
	    }
	}
	
	private GetImageTask getNextTask() {
		GetImageTask nextTask = null;
		for(int i=mTaskStack.size()-1; i>=0; --i) {
			GetImageTask task = mTaskStack.get(i);
			if(task.isCancelled()) {
        		if(DEBUG)
        			Log.d(TAG, "queue task canceled, check next");
        		mTaskStack.remove(task);
        		continue;
        	}
			
			for(GetImageTask runningTask : mRunningTask) {
				if(!task.mId.equals(runningTask.mId)) {
					nextTask = task;
					break;
				}
			}
			if(nextTask != null)
				break;
		}
		return nextTask;
	}
    
    private void checkNextTask() {
    	if(mTaskStack.size() > 0) {
    		GetImageTask nextTask = getNextTask();
    		if(nextTask == null) {
    			nextTask = mTaskStack.peek();
    		}
			mTaskStack.remove(nextTask);
    		mRunningTask.add(nextTask);
    		nextTask.execute();
    		if(DEBUG)
    			Log.d(TAG, "ran queued task, task count:" + mRunningTask.size());
		
    	}
    	else {
    		if(DEBUG)
				Log.d(TAG, "queue empty, task count:" + mRunningTask.size());
    	}
    }
	
	private Bitmap getBitmapFromCache(String cacheIndex) {
    	
        // First try the hard reference cache
        synchronized (sHardBitmapCache) {
            final Bitmap bitmap = sHardBitmapCache.get(cacheIndex);
            if (bitmap != null) {
            	if(DEBUG)
            		Log.d(TAG, "hard cache");
                // Bitmap found in hard cache
                // Move element to first position, so that it is removed last
            	sHardBitmapCache.remove(cacheIndex);
            	sHardBitmapCache.put(cacheIndex, bitmap);
                return bitmap;
            }
        }

        // Then try the soft reference cache
        SoftReference<Bitmap> bitmapReference = sSoftBitmapCache.get(cacheIndex);
        if (bitmapReference != null) {
            final Bitmap bitmap = bitmapReference.get();
            if (bitmap != null) {
            	if(DEBUG)
            		Log.d(TAG, "soft cache");
                // Bitmap found in soft cache
                return bitmap;
            } else {
                // Soft reference has been Garbage Collected
            	sSoftBitmapCache.remove(cacheIndex);
            }
        }

        File file;
        if(mDownloadPath == null)
        	file = new File(mContext.getCacheDir()+"/images", cacheIndex);
        else
        	file = new File(mDownloadPath, cacheIndex);
    	if (file.exists()) {
    		Bitmap bm = BitmapFactory.decodeFile(file.getAbsolutePath());
    		if (bm != null) {
    			if(DEBUG)
            		Log.d(TAG, "file cache");
    			synchronized (sHardBitmapCache) {
    				sHardBitmapCache.put(cacheIndex, bm);
                }
    			return bm;
    		}
    	}
        return null;
    }
	
	private void setBitmapToCache(Bitmap bitmap, String cacheIndex) {
    	
    	if(DEBUG) {
    		Log.d(TAG, "add filename:" + cacheIndex);
    	}
        synchronized (sHardBitmapCache) {
        	sHardBitmapCache.put(cacheIndex, bitmap);
        }

        File targetPath;
        if(mDownloadPath == null)
        	targetPath = new File(mContext.getCacheDir()+"/images");
        else
        	targetPath = new File(mDownloadPath);
        
    	if(!targetPath.exists())
    		targetPath.mkdir();
    	        
        File file = new File(targetPath, cacheIndex);
        FileOutputStream out;
		try {
			out = new FileOutputStream(file);
			bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
		} catch (FileNotFoundException e) {
			Log.e(TAG, e.getMessage());
		}
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

    private static ArrayList<ImageManagerCallback> mCallbackList;
    
    public interface ImageManagerCallback {
		public void imageDone(String id, Bitmap bitmap);
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

	protected Runnable onDoneCallback(final String url, final Bitmap bitmap) {
		return new Runnable() {
			public void run() {
				if(mCallbackList != null) {
					for(int i=0; i<mCallbackList.size(); ++i) {
						ImageManagerCallback cb = mCallbackList.get(i);
						cb.imageDone(url, bitmap);	
					}
				}
			}
		};
	}
    
	public void registerCallback(ImageManagerCallback cb) {
		if (mCallbackList != null && cb != null) {
			mCallbackList.add(cb);
		}
	}

	public void removeCallback(ImageManagerCallback cb) {
		if (mCallbackList != null) {
			if (mCallbackList.indexOf(cb) >= 0)
				mCallbackList.remove(cb);
		}
	}
	
	private static final int HARD_CACHE_CAPACITY = 10;
    private static final int DELAY_BEFORE_PURGE = 10 * 1000; // in milliseconds
    
	 // Hard cache, with a fixed maximum capacity and a life duration
    private final HashMap<String, Bitmap> sHardBitmapCache =
        new LinkedHashMap<String, Bitmap>(HARD_CACHE_CAPACITY / 2, 0.75f, true) {
        private static final long serialVersionUID = 8808576849847802011L;

		@Override
        protected boolean removeEldestEntry(LinkedHashMap.Entry<String, Bitmap> eldest) {
            if (size() > HARD_CACHE_CAPACITY) {
                // Entries push-out of hard reference cache are transferred to soft reference cache
                sSoftBitmapCache.put(eldest.getKey(), new SoftReference<Bitmap>(eldest.getValue()));
                return true;
            } else
                return false;
        }
    };
    private final static ConcurrentHashMap<String, SoftReference<Bitmap>> sSoftBitmapCache =
            new ConcurrentHashMap<String, SoftReference<Bitmap>>(HARD_CACHE_CAPACITY / 2);
    
    private final Handler purgeHandler = new Handler();
    private final Runnable purger = new Runnable() {
        public void run() {
            clearCache();
        }
    }; 
    
    public void clearCache() {
        sHardBitmapCache.clear();
        sSoftBitmapCache.clear();
    }
    
    private void resetPurgeTimer() {
        purgeHandler.removeCallbacks(purger);
        purgeHandler.postDelayed(purger, DELAY_BEFORE_PURGE);
    }
    
    /**
     * A fake Drawable that will be attached to the imageView while the download is in progress.
     *
     * <p>Contains a reference to the actual download task, so that a download task can be stopped
     * if a new binding is required, and makes sure that only the last started download process can
     * bind its result, independently of the download finish order.</p>
     */
    public static class DownloadedDrawable extends BitmapDrawable {
        private final WeakReference<GetImageTask> bitmapGetImageTaskReference;

        public DownloadedDrawable(GetImageTask bitmapDownloaderTask, Bitmap bm) {
        	super(bm);
            bitmapGetImageTaskReference =
                new WeakReference<GetImageTask>(bitmapDownloaderTask);
        }

        public GetImageTask getTask() {
            return bitmapGetImageTaskReference.get();
        }
    }
    
    /**
     * Returns true if the current download has been canceled or if there was no download in
     * progress on this image view.
     * Returns false if the download in progress deals with the same url. The download is not
     * stopped in that case.
     */
    private static boolean removePotentialView(String id, ImageAttribute attr) {
    	ImageView view = null;
    	if(attr != null)
    		view = attr.getView();
    	GetImageTask task = getTask(view);
        if (task != null) {
            if (!task.mId.equals(id)) {
            	if(DEBUG) {
            		Log.d(TAG, "should skip");
            	}
                task.setSkipUpdateView();
            }
            else
            	return false;
        }
        return true;
    }
    
    /**
     * @param imageView Any imageView
     * @return Retrieve the currently active download task (if any) associated with this imageView.
     * null if there is no such task.
     */
    private static GetImageTask getTask(ImageView imageView) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof DownloadedDrawable) {
                DownloadedDrawable downloadedDrawable = (DownloadedDrawable)drawable;
                return downloadedDrawable.getTask();
            }
        }
        return null;
    }
}
