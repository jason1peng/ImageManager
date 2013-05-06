package idv.jason.lib.imagemanager.db;

import java.util.Locale;

import android.net.Uri;

public class ImageTable {
	public static final String TABLE_NAME = "IMAGES";
	
	public static final Uri IMAGE_URI = Uri.parse("content://"
			+ DatabaseHelper.AUTHORITY + "/" + TABLE_NAME.toLowerCase(Locale.getDefault()));
	
	public static final Uri CONTENT_URI = IMAGE_URI;
	
	public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.imagemanager.image";
	public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.imagemanager.image";

	public static final String COLUMN_ID = "id"; // db id
	public static final String COLUMN_IMAGE_URL = "image_url";
	public static final String COLUMN_URL = "url";
	public static final String COLUMN_STATUS = "status"; // processing/done
	public static final String COLUMN_ACCESS_TIME = "access_time";
	public static final String COLUMN_ATTRIBUTE = "attribute";
	
	public static final String DEFAULT_SORT_ORDER = COLUMN_ACCESS_TIME+" DESC";

	public static final int COLUMN_POS_ID 			= 1;
	public static final int COLUMN_POS_IMAGE_URL	= 2;
	public static final int COLUMN_POS_URL 			= 3;
	public static final int COLUMN_POS_STATUS 		= 4;
	public static final int COLUMN_POS_ACCESS_TIME 	= 5;
	public static final int COLUMN_POS_ATTRIBUTE 	= 6;
	
	private ImageTable() {
	}
}