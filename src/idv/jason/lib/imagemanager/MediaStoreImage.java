package idv.jason.lib.imagemanager;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.provider.MediaStore;

public class MediaStoreImage extends BaseImage{
	public static final String TAG = MediaStoreImage.class.getSimpleName();
	public static final String PREFIX = "content://";
	private Context mContext;
	private long mId;
	private Bitmap mBitmap = null;
	
	private static final BitmapFactory.Options sBitmapOptionsCache = new BitmapFactory.Options();
	
	public MediaStoreImage(Context context, String url) {
		mContext = context;
		mId = Long.parseLong(url.substring(PREFIX.length()));

        sBitmapOptionsCache.inPreferredConfig = Bitmap.Config.RGB_565;
        sBitmapOptionsCache.inDither = false;
	}
	
	public static String getId(String url) {
		String id = null;
		if(url.contains(PREFIX)) {
			id = url.substring(PREFIX.length());
		}
		return id;
	}
	
	public Bitmap getBitmap() {
		if (mBitmap != null) {
			return mBitmap;
		}
    	mBitmap = MediaStore.Images.Thumbnails.getThumbnail(
    			mContext.getContentResolver(), mId,
				MediaStore.Images.Thumbnails.MINI_KIND, sBitmapOptionsCache);
    	
    	String[] orientationColumn = { MediaStore.Images.Media.ORIENTATION };
    	Cursor cur = mContext.getContentResolver().query(
    			MediaStore.Images.Media.EXTERNAL_CONTENT_URI, 
    			orientationColumn, MediaStore.Images.Media._ID + "=?", 
    			new String[] {Long.toString(mId)}, null);
    	int orientation = -1;
    	if (cur != null) {
    		if(cur.moveToFirst()) {
    			orientation = cur.getInt(cur.getColumnIndex(orientationColumn[0]));
    		}
    		cur.close();
    	}
    	
    	if(orientation != 0 && orientation != -1) {
	    	Matrix matrix = new Matrix();
	    	matrix.postRotate(orientation);
	    	
	    	if(mBitmap != null) {
				int height = mBitmap.getHeight();
				int width = mBitmap.getWidth();
				mBitmap = Bitmap.createBitmap(mBitmap, 0, 0, width, height, matrix,
						true);
			}
    	}
	    
	    return mBitmap;
	}
	

	@Override
	public void setBitmap(Bitmap bm) {
		mBitmap = bm;		
	}
}
