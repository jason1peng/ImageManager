package idv.jason.lib.imagemanager;

import java.io.File;

import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.provider.MediaStore.Video.Thumbnails;

public class LocalVideoImage extends BaseImage{
	public static final String TAG = LocalVideoImage.class.getSimpleName();
	public static final String PREFIX = "video://";
	private String mPath;
	private Bitmap mBitmap = null;
	
	public LocalVideoImage(String path) {
		mPath = path;
	}
	
	public Bitmap getBitmap() {
		if (mBitmap != null) {
			return mBitmap;
		}
		String path = mPath.substring(PREFIX.length());
		File file = new File(path);
		if(file.exists()){
			mBitmap = ThumbnailUtils.createVideoThumbnail(path, 
			        Thumbnails.MINI_KIND);
		}
	    
	    return mBitmap;
	}
	

	@Override
	public void setBitmap(Bitmap bm) {
		mBitmap = bm;		
	}
}
