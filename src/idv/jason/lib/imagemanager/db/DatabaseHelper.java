package idv.jason.lib.imagemanager.db;

import java.text.MessageFormat;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper{
	public static final String AUTHORITY = "idv.jason.lib.imagemanager";
	private static final String DATABASE_NAME = "imagemanager.db";
	private static final int DATABASE_VERSION = 1;
	
	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("Create Table {0} (")
				  .append(ImageTable.COLUMN_ID+" INTEGER PRIMARY KEY,")
				  .append(ImageTable.COLUMN_IMAGE_URL+" TEXT NOT NULL,")
				  .append(ImageTable.COLUMN_URL+" TEXT ,")
				  .append(ImageTable.COLUMN_STATUS+" TEXT ,")
				  .append(ImageTable.COLUMN_ACCESS_TIME+" TEXT ,")
				  .append(ImageTable.COLUMN_ATTRIBUTE + " TEXT);");
		createTable(db, sqlBuilder.toString(), ImageTable.TABLE_NAME);
		
		String INDEX_NAME = "IDX_"+ImageTable.TABLE_NAME;
		String sql = "CREATE INDEX "+ INDEX_NAME +" on "+ImageTable.TABLE_NAME+"("+ImageTable.COLUMN_IMAGE_URL+");";
		db.execSQL(sql);
	}
	
	private void createTable(SQLiteDatabase db, String sql, String tableName) {
		String createSql = MessageFormat.format(sql, tableName);
		db.execSQL(createSql);
	}

	@Override
	public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
		// TODO Auto-generated method stub
		
	}

}
