package idv.jason.lib.imagemanager;

import android.graphics.Bitmap;

public abstract class BaseImage {		
	public Bitmap getBitmap() throws OutOfMemoryError {
		return null;
	}
	
	public abstract void setBitmap(Bitmap bm);
}
