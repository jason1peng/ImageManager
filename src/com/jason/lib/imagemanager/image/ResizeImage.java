package com.jason.lib.imagemanager.image;

import android.graphics.Bitmap;

public class ResizeImage extends ImageDecorator{
	private BaseImage mImage = null;
	private int mWidth;
	private int mHeight;
	
	public ResizeImage(BaseImage image, int width, int height) {
		mImage = image;
		mWidth = width;
		mHeight = height;
	}
	
	@Override
	public Bitmap getBitmap() {
		return ImageUtil.extractThumbnail(mImage.getBitmap(), mWidth, mHeight);
	}

}
