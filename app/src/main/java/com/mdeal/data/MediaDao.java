package com.mdeal.data;

import java.util.ArrayList;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class MediaDao {
	private DBOpenHelper helper;

	public MediaDao(Context context) {
		helper = new DBOpenHelper(context);
	}

	
	public ArrayList<Media> getMedias() {
		ArrayList<Media> Medias = null;
		SQLiteDatabase db = helper.getReadableDatabase();
		Log.d("phone book", "will get phone book from sql");
		Cursor c = db.query(DBOpenHelper.VIDEOB_TABLE_NAME, null, null, null, null, null, null);
		if (c != null && c.getCount() > 0) {
			Medias = new ArrayList<Media>();
			while (c.moveToNext()) {
				
				Media media = fillMedia(c);
				Log.d("phone book", "get phone book from sql.name = "+media.getName());
				Medias.add(media);
			}
			c.close();
		}
		db.close();
		return Medias;
	}

	private Media fillMedia (Cursor c) {
		Media media = new Media();
		media.setId(c.getInt(c.getColumnIndex("id")));
		media.setPath(c.getString(c.getColumnIndex("path")));
		media.setName(c.getString(c.getColumnIndex("nickname")));
		return media;
	}


	public ArrayList<String> getMediasPaths() {
		ArrayList<String> MediasPaths= null;
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor c = db.query(DBOpenHelper.VIDEOB_TABLE_NAME, null, null, null, null, null, null);
		if (c != null && c.getCount() > 0) {
			MediasPaths = new ArrayList<String>();
			while (c.moveToNext()) {
				String mediaPath = fillMediaPaths(c);
				MediasPaths.add(mediaPath);
			}
			c.close();
		}
		db.close();
		return MediasPaths;
	}

	private String  fillMediaPaths(Cursor c) {
		return c.getString(c.getColumnIndex("path"));
	}
	

	public long addMedia(Media media) {
		SQLiteDatabase db = helper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("path", media.getPath());
		values.put("nickname", media.getName());
		long rowId = db.insert(DBOpenHelper.VIDEOB_TABLE_NAME, "path", values);
		Log.d("phone book", "add data to sql.result= "+rowId);
		db.close();
		return rowId;
	}


	
	
	public boolean existsWithOherName(Media media,String name){
		boolean isExists = false;
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor c = db.query(DBOpenHelper.VIDEOB_TABLE_NAME, null, "path=?", new String[]{media.getPath()}, null, null, null);
		if(c!=null && c.moveToFirst()){
			if(!name.equals(c.getString(c.getColumnIndex("nickname")))){
				isExists = true;
			}
			c.close();
			
		}
		db.close();
		return isExists;
	}
	
	public boolean exists(Media media){
		boolean isExists = false;
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor c = db.query(DBOpenHelper.VIDEOB_TABLE_NAME, null, "path=?", new String[]{media.getPath()}, null, null, null);
		if(c!=null && c.moveToFirst()){
			isExists = true;
			c.close();
		}
		db.close();
		return isExists;
	}
	
	

	public int removeMedia(int id) {
		SQLiteDatabase db = helper.getWritableDatabase();
		int count = db.delete(DBOpenHelper.VIDEOB_TABLE_NAME, "_id=?", new String[]{""+id});
		db.close();
		return count;
	}

	public int removeMedia(String path) {
		SQLiteDatabase db = helper.getWritableDatabase();
		int count = db.delete(DBOpenHelper.VIDEOB_TABLE_NAME, "path=?", new String[]{path});
		db.close();
		return count;
	}
	
	
	public Media getMedia(int id) {
		Media media = null;
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor c = db.rawQuery("select * from " + DBOpenHelper.VIDEOB_TABLE_NAME + " where _id=?", new String[]{""+id});
		if(c!=null && c.moveToFirst()){
			media = fillMedia(c);
		}
		db.close();
		return media;
	}
}
