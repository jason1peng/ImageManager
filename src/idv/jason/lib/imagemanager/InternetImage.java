package idv.jason.lib.imagemanager;

import com.jason.lib.imagemanager.conn.HttpInvoker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Display;
import android.view.WindowManager;

public class InternetImage extends BaseImage {
	public static final String TAG = InternetImage.class.getSimpleName();
	private int IMAGE_MAX_WIDTH = 0;
	private int IMAGE_MAX_HEIGHT = 0;
	private Context mContext;
	private String mUrl;
	private Bitmap mBitmap;

	public InternetImage(Context context, String url) {
		mContext = context;
		mUrl = url;
	}
	
	public InternetImage(Context context, String url, int maxWidth, int maxHeight) {
		mContext = context;
		mUrl = url;
		IMAGE_MAX_WIDTH = maxWidth;
		IMAGE_MAX_HEIGHT = maxHeight;
	}

	public Bitmap getBitmap() throws OutOfMemoryError {
		if (mBitmap != null) {
			return mBitmap;
		}

		if (HttpInvoker.isNetworkAvailable(mContext)) {
			BitmapFactory.Options options = new BitmapFactory.Options();

			if(IMAGE_MAX_WIDTH != 0 && IMAGE_MAX_HEIGHT != 0) {
				options.inJustDecodeBounds = true;
				BitmapFactory.decodeStream(HttpInvoker.getInputStreamFromUrl(mUrl),
						null, options);
				options.inJustDecodeBounds = false;
				
				options.inSampleSize = ImageUtil.calculateInSampleSize(options, IMAGE_MAX_WIDTH, IMAGE_MAX_HEIGHT);
			}
			options.inDither = false;
			mBitmap = BitmapFactory.decodeStream(
					HttpInvoker.getInputStreamFromUrl(mUrl), null, options);
		}
		return mBitmap;
	}

	@Override
	public void setBitmap(Bitmap bm) {
		mBitmap = bm;
	}
}
