package org.linphone.ui.witsui;

import java.util.Calendar;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class Tools {
	private Calendar calendar=Calendar.getInstance();
	
	public String getSystemY(){
		return String.valueOf(calendar.get(Calendar.YEAR));
	}
	
	public String getSystemM(){
		return String.valueOf(calendar.get(Calendar.MONTH));
	}
	
	public String getSystemD(){
		return String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
	}
	
	public String getHour(){
		return String.valueOf(calendar.get(Calendar.HOUR_OF_DAY));
	}
	public String getMin(){
		return String.valueOf(calendar.get(Calendar.MINUTE));
	}
	
	
	public static String orderTime(){  //正确时间显示
		ClientTime ct=ClientTime.getInstance();
		int minunt=ct.minunt;
		return minunt<10?(ct.hour+":0"+minunt):(ct.hour+":"+minunt);
	}
	
	public static void setBlock(Context context,String blockno){
		SharedPreferences sh=context.getSharedPreferences(Constant.SHARDPREFERENCE_NAME, Activity.MODE_PRIVATE);
		sh.edit().putString(Constant.BLOCK_NO, blockno).commit();
	}
	public static String getBlock(Context context){
		SharedPreferences sh=context.getSharedPreferences(Constant.SHARDPREFERENCE_NAME, Activity.MODE_PRIVATE);
		return sh.getString(Constant.BLOCK_NO, "01");
	}
	
	public static void setFlat(Context context,String flatno){
		SharedPreferences sh=context.getSharedPreferences(Constant.SHARDPREFERENCE_NAME, Activity.MODE_PRIVATE);
		sh.edit().putString(Constant.FLAT_NO, flatno).commit();
	}
	public static String getFlat(Context context){
		return context.getSharedPreferences(Constant.SHARDPREFERENCE_NAME, Activity.MODE_PRIVATE).getString(Constant.FLAT_NO, "801");
	}
	public static void setSitName(Context context,String sitName){
	context.getSharedPreferences(Constant.SHARDPREFERENCE_NAME, Activity.MODE_PRIVATE).edit().putString(Constant.SITE_NAME, sitName).commit();
	}
	public static String getSitName(Context context){
		return context.getSharedPreferences(Constant.SHARDPREFERENCE_NAME, Activity.MODE_PRIVATE).getString(Constant.SITE_NAME,"SILVERSTONE'S" );
	}
}
 