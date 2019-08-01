package com.wits.intercom.setting;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Locale;

import org.linphone.LinphoneManager;

import com.keep.lin.R;
import com.mdeal.data.Media;
import com.mdeal.data.MediaDao;
import com.wits.intercom.CallInfor;
import com.wits.intercom.CallPageActivity;
import com.wits.intercom.Constance;
import com.wits.intercom.LanguageAdapter;
import com.wits.intercom.LinphoneWelcomeActivity;
import com.wits.intercom.MainFunctionsActivity;
import com.wits.intercom.MyApplication;

import com.wits.intercom.PhoneBook;
import com.wits.intercom.dialing.EditToCallActivity;
import com.wits.intercom.util.Logger;

import android.R.integer;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ListView;

public class LanguageSetupActivity extends FragmentActivity{
	private final String TAG = LanguageSetupActivity.class.getSimpleName();
	
	private ListView mListView;
	private ArrayList<PhoneBook> phoneBooks;
	private int iFirstOrLastItemSelected ;
	
	private MediaDao mMediaDao;
	private ArrayList<Media> mMediaList;
	private int currentPosition = 0;
	private ArrayList<String> mCurrentList;
	private int selector = 0;
	private LanguageAdapter mAdapter;
	protected int CLEAR_INPUT_MESSAGE = 10;
	private Handler mHandle = new Handler() {
		public void dispatchMessage(android.os.Message msg) {
			Log.w(Constance.LOGTAG, "get message");

			if(msg.what == CLEAR_INPUT_MESSAGE ){
				LinphoneManager.isInSetting = false;
				startActivity(new Intent(LanguageSetupActivity.this,LinphoneWelcomeActivity.class));
			}
			
		};
	};
	
	@Override
	protected void onResume(){
		super.onResume();
		mHandle.sendEmptyMessageDelayed(CLEAR_INPUT_MESSAGE, 60000);
	}
	
	@Override
	protected void onPause(){
		super.onPause();
		mHandle.removeMessages(CLEAR_INPUT_MESSAGE);
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MyApplication.getInstnce().addActivity(this);
		setContentView(R.layout.activity_language_setup);
		mCurrentList = new ArrayList<String>();
		getResources().getString(R.string.english_language);
		mCurrentList.add(getResources().getString(R.string.english_language));
		mCurrentList.add(getResources().getString(R.string.turkish_language));
		
		mListView=(ListView)findViewById(R.id.phone_book_listview);
		mAdapter = new LanguageAdapter(this, mCurrentList);
		mListView.setAdapter(mAdapter);
		//监听Window中的选中(高亮)的item  
    	mListView.setOnItemSelectedListener(itemSelectedListener);  
    	
    	mListView.setOnItemClickListener(clickListener);
    	
	}
	

   public 	OnItemClickListener clickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
			//在此可实现响应item被点击后的功能
			String languageString = mCurrentList.get(position);
			switch (position) {
			case 0:
				Locale mEnglisLocale = new Locale("en");
				updateLanguage(mEnglisLocale);   //will invoke onCreate() automaticlly
//				finish();
				break;
			case 1:
				Locale mTurkishLocale = new Locale("tr_TR");
				updateLanguage(mTurkishLocale);
//				finish();
				break;
			default:
				break;
			}
		}
	};
	
	private void updateLanguage(Locale locale) {  
	    Log.d("ANDROID_LAB", locale.toString());  
	    try {  
	        Object objIActMag, objActMagNative;  
	        Class clzIActMag = Class.forName("android.app.IActivityManager");  
	        Class clzActMagNative = Class.forName("android.app.ActivityManagerNative");  
	        Method mtdActMagNative$getDefault = clzActMagNative.getDeclaredMethod("getDefault");  
	        // IActivityManager iActMag = ActivityManagerNative.getDefault();   
	        objIActMag = mtdActMagNative$getDefault.invoke(clzActMagNative);  
	        // Configuration config = iActMag.getConfiguration();   
	        Method mtdIActMag$getConfiguration = clzIActMag.getDeclaredMethod("getConfiguration");  
	        Configuration config = (Configuration) mtdIActMag$getConfiguration.invoke(objIActMag);  
	        config.locale = locale;  
	        // iActMag.updateConfiguration(config);   
	        // 此处需要声明权限:android.permission.CHANGE_CONFIGURATION   
	        // 会重新调用 onCreate();   
	        Class[] clzParams = { Configuration.class };  
	        Method mtdIActMag$updateConfiguration = clzIActMag.getDeclaredMethod(  
	                "updateConfiguration", clzParams);  
	       mtdIActMag$updateConfiguration.invoke(objIActMag, config);
	    } catch (Exception e) {  
	        e.printStackTrace();  
	    }  
	}  

	
   public  OnItemSelectedListener itemSelectedListener = new OnItemSelectedListener() {  
		@Override
		public void onItemSelected(AdapterView<?> arg0, View view, int position,
				long id) {
			mAdapter.setCurrentItem(position);
			mAdapter.notifySetDataChange();
		}
		@Override
		public void onNothingSelected(AdapterView<?> arg0) {	
			  Log.d("itemselected", "nothing selected ");
		}   
    };  
	
	private CallInfor initCallInfor(int role,String sipString){
		CallInfor callInfor=CallInfor.getInstance();
//		callInfor.sipip=sipString;
//		callInfor.role=role;
		CallInfor.setSipip(sipString);
		CallInfor.setRole(role);
		
		return callInfor;
	}
	
	public boolean dispatchKeyEvent(KeyEvent event) {  
		   // TODO Auto-generated method stub  
		   // 判断普通按键  
		   int keyCode = event.getKeyCode();
		   if(event.getAction() == KeyEvent.ACTION_DOWN){
			   
			   Log.d("Key Event", "Key code = "+keyCode);
			if(keyCode == 19){             //left up
				switch (selector) {
				case 0:
					Locale mEnglisLocale = new Locale("en");
					updateLanguage(mEnglisLocale);   //will invoke onCreate() automaticlly
//					finish();
					break;
				case 1:
					Locale mTurkishLocale = new Locale("tr_TR");
					updateLanguage(mTurkishLocale);
//					finish();
					break;
				default:
					break;
				}
//				MyApplication.getInstnce().close();
				startActivity(new Intent(LanguageSetupActivity.this,LinphoneWelcomeActivity.class));
				   return true;
			   }else if(keyCode == 20){   //left down
				  MyApplication.getInstnce().back(1);
				  return true;
			   }else if(keyCode == 21){   //right up
				   if(selector>0){
					   selector--;
				   }else{
					   selector = mCurrentList.size()-1;
				   }
				   mListView.setSelection(selector);
				   return true;
				   
			   }else if(keyCode == 22){   //right down
				   if(selector<6 && selector <(mCurrentList.size()-1)){
						  selector++;
					  }else{
						  selector = 0;
					  }
					  mListView.setSelection(selector);
				   return true;
				   
			   }else if(keyCode == 67){
				   finish();
				   return true;
			   }
		   }
		   return super.dispatchKeyEvent(event);
		   
		} 
	
	
}
