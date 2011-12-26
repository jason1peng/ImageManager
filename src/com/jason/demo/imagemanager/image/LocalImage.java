package com.jason.demo.imagemanager.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

        BitmapFactory.decodeFile(mPath, o);

        int scale = 1;
        if (o.outHeight > IMAGE_MAX_HEIGHT || o.outWidth > IMAGE_MAX_WIDTH) {
            scale = (int)Math.pow(2, (int) Math.round(Math.log(Math.max(IMAGE_MAX_HEIGHT, IMAGE_MAX_WIDTH) / (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
        }

        //Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        b = BitmapFactory.decodeFile(mPath, o2);
	    return b;
	}
}
