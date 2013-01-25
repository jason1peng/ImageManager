package idv.jason.lib.imagemanager;

import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

public class LocalImage extends BaseImage{
	private String mPath;
	private int IMAGE_MAX_WIDTH = 0;
	private int IMAGE_MAX_HEIGHT = 0;
	private Bitmap mBitmap = null;
	
	public LocalImage(Context context, String path) {
		mPath = path;
	}
	
	public LocalImage(Context context, String path, int width, int height) {
		mPath = path;
		IMAGE_MAX_WIDTH = 1024;
		IMAGE_MAX_HEIGHT = 1024;		
	}
	
	public Bitmap getBitmap() throws OutOfMemoryError{
		if (mBitmap != null) {
			return mBitmap;
		}

        String filename = null;	
        //Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        if(IMAGE_MAX_WIDTH != 0 && IMAGE_MAX_HEIGHT != 0) {
	        o.inJustDecodeBounds = true;
	
			if (mPath.toString().startsWith("file://")) {
				filename = mPath.toString().substring(7);
			} else {
				filename = mPath;
			}
			
	        BitmapFactory.decodeFile(filename, o);
	
	        //Decode with inSampleSize
	        o.inJustDecodeBounds = false;
	        o.inSampleSize = ImageUtil.calculateInSampleSize(o, IMAGE_MAX_WIDTH, IMAGE_MAX_HEIGHT);
	        mBitmap = BitmapFactory.decodeFile(filename, o);
        }
        
        // Rotate to right direction
        Matrix matrix = new Matrix();
		float rotation = rotationForImage(filename);
		if (rotation != 0f) {
			matrix.preRotate(rotation);
		}

		if(mBitmap != null) {
			int height = mBitmap.getHeight();
			int width = mBitmap.getWidth();
			mBitmap = Bitmap.createBitmap(mBitmap, 0, 0, width, height, matrix,
					true);
		}
	    return mBitmap;
	}
	
	public static int rotationForImage(String filename) {
		int rotation = 0;
		try {
			ExifInterface exif = new ExifInterface(filename);
			rotation = (int) exifOrientationToDegrees(exif.getAttributeInt(
					ExifInterface.TAG_ORIENTATION,
					ExifInterface.ORIENTATION_NORMAL));
		} catch (IOException e) {

			e.printStackTrace();
		}
		return rotation;
	}

	public static float exifOrientationToDegrees(int exifOrientation) {
		if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
			return 90;
		} else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
			return 180;
		} else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
			return 270;
		}
		return 0;
	}
	
	@Override
	public void setBitmap(Bitmap bm) {
		mBitmap = bm;
	}
}
