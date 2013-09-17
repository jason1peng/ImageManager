package idv.jason.lib.imagemanager;

import com.jason.lib.imagemanager.conn.HttpInvoker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class InternetImage extends BaseImage {
	public static final String TAG = InternetImage.class.getSimpleName();
	private int IMAGE_MAX_WIDTH = 0;
	private int IMAGE_MAX_HEIGHT = 0;
	private Context mContext;
	private String mUrl;
	private Bitmap mBitmap;
	
	private boolean mHighQuality = false;

	public InternetImage(Context context, String url) {
		mContext = context;
		mUrl = url;
	}
	
	public void setImaggMaxSize(int height, int width) {
		IMAGE_MAX_WIDTH = width;
		IMAGE_MAX_HEIGHT = height;		
	}
	
	public void setHighQuality(boolean highQuality) {
		mHighQuality = highQuality;
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
				
				if(mHighQuality == false)
					options.inPreferredConfig = Bitmap.Config.RGB_565;
				options.inSampleSize = ImageUtil.calculateInSampleSize(options, 0, IMAGE_MAX_WIDTH, IMAGE_MAX_HEIGHT);
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
