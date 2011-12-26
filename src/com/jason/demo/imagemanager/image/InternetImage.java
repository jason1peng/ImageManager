package com.jason.demo.imagemanager.image;

import java.io.InputStream;

import com.jason.demo.imagemanager.imagesource.HttpInvoker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class InternetImage extends BaseImage{
	public static final String TAG = InternetImage.class.getSimpleName();
	private Context mContext;
	private String mUrl;
	
	public InternetImage(Context context, String url) {
		mContext = context;
		mUrl = url;
	}
	
	public Bitmap getBitmap() {
		Bitmap bitmap = null;
		try {
			if (HttpInvoker.isNetworkAvailable(mContext)) {
				bitmap = downloadBitmap(mUrl);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return bitmap;
	}
	
	public Bitmap downloadBitmap(String url) {
		InputStream is = HttpInvoker.getInputStreamFromUrl(url);
		if(is != null)
			return BitmapFactory.decodeStream(new HttpInvoker.FlushedInputStream(is));
        return null;
	}
}
