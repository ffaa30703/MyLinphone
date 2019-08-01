package com.mdeal.data;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBOpenHelper extends SQLiteOpenHelper{
	

	// 数据库名称常量
    private static final String DATABASE_NAME = "Mdeal.db";
    // 数据库版本常量
    private static final int DATABASE_VERSION = 2;
    // 表名称常量
    public static final String VIDEOA_TABLE_NAME = "video";
    
    public static final String VIDEOB_TABLE_NAME = "media";
    
    public static final String IMAGE_TABLE_NAME = "image";
    
        
    public DBOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	

	@Override
	public void onCreate(SQLiteDatabase db) {
		
		String sql1 = "create table if not exists  " + VIDEOA_TABLE_NAME + 
				"(" +
				"_id integer primary key autoincrement," +			
				"path text not null," +
				"nickname text not null" +
				")";
		
		String sql2 = "create table if not exists  " + VIDEOB_TABLE_NAME + 
				"(id integer primary key," +			
				"path varchar," +
				"nickname varchar" +
				")";
		
		String sql3 = "create table if not exists  " + IMAGE_TABLE_NAME + 
				"(" +
				"_id integer primary key autoincrement," +			
				"path text not null," +
				"nickname text not null" +
				")";
		
		db.execSQL(sql1);
		db.execSQL(sql2);
		db.execSQL(sql3);
		Log.d("phone book", "creat table success");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		  Log.d("phone book", "upgrade table");
		  db.execSQL("DROP TABLE IF EXISTS "+VIDEOB_TABLE_NAME);
	      onCreate(db);		
	}

}
