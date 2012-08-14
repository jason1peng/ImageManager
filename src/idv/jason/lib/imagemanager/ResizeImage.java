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
			mBitmap = ImageUtil.extractThumbnail(mImage.getBitmap(), mWidth, mHeight);
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
