package idv.jason.lib.imagemanager;

import android.graphics.Bitmap;

public class ResizeImage extends ImageDecorator{
	private BaseImage mImage = null;
	private int mWidth;
	private int mHeight;
	private Bitmap mBitmap = null;
	
	public ResizeImage(BaseImage image, int width, int height) {
		mImage = image;
		mWidth = width;
		mHeight = height;
	}
	
	@Override
	public Bitmap getBitmap() {
		if(mBitmap == null) {
			Bitmap bitmap = mImage.getBitmap();
			if(bitmap != null) {
				float ratio = (float)bitmap.getWidth() / (float)bitmap.getHeight();

				int finalWidth = 0;
				int finalHeight = 0;
				if(bitmap.getWidth() > bitmap.getHeight()) {
					finalWidth = mWidth;
					finalHeight = (int) (finalWidth / ratio);
				} else {
					finalHeight = mHeight;
					finalWidth = (int) (finalHeight * ratio);
				}
				
				mBitmap = Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, false);
			}
			return mBitmap;
		} else {
			return mBitmap;
		}
	}
	
	@Override
	public void setBitmap(Bitmap bm) {
		mBitmap = bm;
	}
}
