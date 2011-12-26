package com.jason.demo.imagemanager.image;

import android.graphics.Bitmap;

public class RoundCorner extends ImageDecorator {
	private BaseImage mImage;
	private int mPixels;
	
	public RoundCorner(BaseImage image, int roundPixels) {
		this.mImage = image;
		this.mPixels = roundPixels;
	}

	@Override
	public Bitmap getBitmap() {
		Bitmap bitmap = mImage.getBitmap();
		if(bitmap != null)
			bitmap = ImageUtil.getRoundedCornerBitmap(bitmap, mPixels);
		return bitmap;
	}
}
