package idv.jason.lib.imagemanager;

import com.lightbox.android.photoprocessing.PhotoProcessing;

import android.graphics.Bitmap;

public class FilterImage extends ImageDecorator{
	private BaseImage mImage = null;
	private int mFilterId;
	private Bitmap mBitmap = null;
	
	public FilterImage(BaseImage image, int filter) {
		mImage = image;
		mFilterId = filter;
	}
	
	@Override
	public Bitmap getBitmap() {
		if(mBitmap == null) {
			Bitmap bitmap = mImage.getBitmap();
			if(bitmap != null) {
				PhotoProcessing.sendBitmapToNative(bitmap);
				bitmap.recycle();
				PhotoProcessing.filterPhoto(mFilterId);
				return PhotoProcessing.getBitmapFromNative(null);
			} else {
				return null;
			}
		} else {
			return mBitmap;
		}
	}
	
	@Override
	public void setBitmap(Bitmap bm) {
		mBitmap = bm;
	}
}
