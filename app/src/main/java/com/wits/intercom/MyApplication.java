package com.wits.intercom;

import java.util.LinkedList;
import java.util.Stack;

import android.R.integer;
import android.app.Activity;
import android.app.Application;
import android.util.Log;
import android.widget.Toast;

public class MyApplication extends Application {
	private static MyApplication instance;
	private LinkedList<Activity> activityList = new LinkedList<Activity>();
	
	private Stack<Activity>  mActivityStack = new Stack<Activity>();
	
	private MyApplication() {

	}

	public static MyApplication getInstnce() {
		if (null == instance) {
			instance = new MyApplication();
		}
		return instance;
	};
	
	public void addActivity(Activity activity) {
//		activityList.add(activity);
		mActivityStack.push(activity);
		
	}

	public void exitApplication() {
//		for (Activity cureActivity : activityList) {
//			if (cureActivity != null) {
//				cureActivity.finish();
//			}
//		}
		
		while(!mActivityStack.isEmpty()){                             //add by jimmy at 2014.5.5
			Activity curActivity = mActivityStack.pop();
			curActivity.finish();
		}
		
		System.exit(0);
	}

	public void buildIngToast(Activity activity) {
		Toast.makeText(activity, "this function is building", Toast.LENGTH_LONG)
				.show();

	}

	public void goHome() {
//		for (Activity cureActivity : activityList) {
//			if (cureActivity != null) {
//				if (!cureActivity.getClass().toString()
//						.equals("class com.wits.intercom70.FunctionActivity")
//						&& !cureActivity
//								.getClass()
//								.toString()
//								.equals("class com.wits.intercom70.MainActivity")) {
//					
//					
//					cureActivity.finish();
//				}
//			}
//		}
		
		while(mActivityStack.size()>2){                             //add by jimmy at 2014.5.5
			Activity curActivity = mActivityStack.pop();
			curActivity.finish();
		}
		
	}
	
	public void clear(){
		while(!mActivityStack.isEmpty()){                             //add by jimmy at 2014.5.5
			Activity curActivity = mActivityStack.pop();
			curActivity.finish();
		}
	}
	
	public void back(int pageNumber){
		for(int i = 0;i<pageNumber;i++){
			Activity curActivity = mActivityStack.pop();
			curActivity.finish();
		}
	}

	public void close() {
		Log.w(Constance.LOGTAG,
				"================MyApplication close==================");
//		for (Activity cureActivity : activityList) {
//			if (cureActivity != null) {
//				if (!cureActivity.getClass().toString()
//						.equals("class com.wits.intercom70.MainActivity")) {
//					cureActivity.finish();
//				}
//			}
//		}
		while(mActivityStack.size()>0){                             //add by jimmy at 2014.5.5
			Activity curActivity = mActivityStack.pop();
			curActivity.finish();
		}
	}
	
	public void backToSetting() {
		Log.w(Constance.LOGTAG,
				"================MyApplication close==================");
//		for (Activity cureActivity : activityList) {
//			if (cureActivity != null) {
//				if (!cureActivity.getClass().toString()
//						.equals("class com.wits.intercom70.MainActivity")) {
//					cureActivity.finish();
//				}
//			}
//		}
		while(mActivityStack.size()>2){                             //add by jimmy at 2014.5.5
			Activity curActivity = mActivityStack.pop();
			curActivity.finish();
		}
	}

	public void removeStateChangeListener() {
		Log.w("app", "removeStateChangeListener----------------------");
		for (Activity cureActivity : activityList) {
			if (cureActivity != null) {
				if (cureActivity.getClass().toString()
						.equals("class com.wits.intercom70.intercom.IntercomActivity")) {
					cureActivity.finish();
					Log.w("app", cureActivity.getClass().toString());
				}
			}
		}
	}

	public Activity removeActivity() {
		// TODO Auto-generated method stub
//		activityList.remove(activity);
		return mActivityStack.pop();
	}
}
