package idv.jason.lib.imagemanager;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader.TileMode;

public class ReflectionImage extends ImageDecorator {
	private BaseImage mImage = null;
	private Bitmap mBitmap = null;

	public ReflectionImage(BaseImage image) {
		mImage = image;
	}

	@Override
	public Bitmap getBitmap() {
		if (mBitmap == null && mImage != null) {
			Bitmap originBitmap = mImage.getBitmap();
			if(originBitmap == null)
				return null;
			int width = originBitmap.getWidth();
			int height = originBitmap.getHeight();

			Matrix flipHorizontalMatrix = new Matrix();
			flipHorizontalMatrix.setScale(1,-1);
			flipHorizontalMatrix.postTranslate(0,originBitmap.getHeight());
			
			// Create a new bitmap with same width but taller to fit reflection
			mBitmap = Bitmap.createBitmap(width,
					height/3, Config.ARGB_8888);

			// Create a new Canvas with the bitmap that's big enough for
			// the image plus gap plus reflection
			Canvas canvas = new Canvas(mBitmap);
			// Draw in the original image
			canvas.drawBitmap(originBitmap, flipHorizontalMatrix, null);
			
			// Create a shader that is a linear gradient that covers the
			// reflection
			Paint paint = new Paint();
			LinearGradient shader = new LinearGradient(
					0, 0, 0, height/3, new int[] { 0x90ffffff, 0x30ffffff, 0x00ffffff}, new float[] { 0, (float) 0.5, 1}, TileMode.CLAMP);
			// Set the paint to use this shader (linear gradient)
			paint.setShader(shader);
			// Set the Transfer mode to be porter duff and destination in
			paint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
			// Draw a rectangle using the paint with our linear gradient
			canvas.drawRect(0, 0, width, height/3, paint);

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
