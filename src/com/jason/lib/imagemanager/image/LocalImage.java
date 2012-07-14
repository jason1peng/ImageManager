package com.jason.lib.imagemanager.image;

import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.view.Display;
import android.view.WindowManager;

public class LocalImage extends BaseImage{
	private String mPath;
	private int IMAGE_MAX_WIDTH;
	private int IMAGE_MAX_HEIGHT;
	
	public LocalImage(Context context, String path) {
		mPath = path;

		Display display = ((WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		IMAGE_MAX_WIDTH = display.getWidth();
		IMAGE_MAX_HEIGHT = display.getHeight();
	}
	
	public LocalImage(Context context, String path, int width, int height) {
		mPath = path;
		IMAGE_MAX_WIDTH = width;
		IMAGE_MAX_HEIGHT = height;		
	}
	
	public Bitmap getBitmap() {
	    Bitmap b = null;
        //Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;

        String filename = null;
		if (mPath.toString().startsWith("file://")) {
			filename = mPath.toString().substring(7);
		}
		
        BitmapFactory.decodeFile(filename, o);

        int scale = 1;
        if (o.outHeight > IMAGE_MAX_HEIGHT || o.outWidth > IMAGE_MAX_WIDTH) {
            scale = (int)Math.pow(2, (int) Math.round(Math.log(Math.max(IMAGE_MAX_HEIGHT, IMAGE_MAX_WIDTH) / (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
        }

        //Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        b = BitmapFactory.decodeFile(filename, o2);
        
        // Rotate to right direction
        Matrix matrix = new Matrix();
		float rotation = rotationForImage(filename);
		if (rotation != 0f) {
			matrix.preRotate(rotation);
		}

		if(b != null) {
			int height = b.getHeight();
			int width = b.getWidth();
			b = Bitmap.createBitmap(b, 0, 0, width, height, matrix,
					true);
		}
	    return b;
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
}
