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

public class RotateImage extends ImageDecorator {
	private BaseImage mImage = null;
	private Bitmap mBitmap = null;
    private int mDegree;

	public RotateImage(BaseImage image, int degree) {
		mImage = image;
        mDegree = degree;
	}

	@Override
	public Bitmap getBitmap() {
		if (mBitmap == null && mImage != null) {
			Bitmap originBitmap = mImage.getBitmap();
			if(originBitmap == null)
				return null;
			int width = originBitmap.getWidth();
			int height = originBitmap.getHeight();

            Matrix matrix = new Matrix();
            matrix.postRotate(mDegree);

            mBitmap = Bitmap.createBitmap(originBitmap, 0, 0,
                    width, height, matrix, true);
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
