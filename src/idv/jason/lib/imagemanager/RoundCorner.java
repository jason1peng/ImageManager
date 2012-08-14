package idv.jason.lib.imagemanager;

import android.graphics.Bitmap;

public class RoundCorner extends ImageDecorator {
	private BaseImage mImage;
	private int mPixels;
	private Bitmap mBitmap = null;
	
	public RoundCorner(BaseImage image, int roundPixels) {
		this.mImage = image;
		this.mPixels = roundPixels;
	}

	@Override
	public Bitmap getBitmap() {
		if(mBitmap == null) {
			mBitmap = mImage.getBitmap();
			mBitmap = ImageUtil.getRoundedCornerBitmap(mBitmap, mPixels);
		}
		return mBitmap;
	}
	
	@Override
	public void setBitmap(Bitmap bm) {
		mBitmap = bm;
	}
}
