package idv.jason.lib.imagemanager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;

public class BlendImage extends ImageDecorator{
	private BaseImage baseBitmap = null;
	private Bitmap mBlendBitmap = null;
	private Bitmap mFinalBitmap = null;
	
	public BlendImage(Context context, BaseImage bitmap, int blendResId) {
		this.baseBitmap = bitmap;
		this.mBlendBitmap = ((BitmapDrawable) context.getResources()
				.getDrawable(blendResId)).getBitmap();
	}

	@Override
	public Bitmap getBitmap() {
		if (mFinalBitmap != null) {
			return mFinalBitmap;
		}

		Bitmap base = baseBitmap.getBitmap();
		mFinalBitmap = Bitmap.createBitmap(base.getWidth(), base.getHeight(),
				base.getConfig());
		Canvas canvas = new Canvas(mFinalBitmap);
		canvas.drawBitmap(base, new Matrix(), null);
		canvas.drawBitmap(mBlendBitmap, 0, 0, null);
		return mFinalBitmap;
	}

	@Override
	public void setBitmap(Bitmap bm) {
		mFinalBitmap = bm;
	}
}
