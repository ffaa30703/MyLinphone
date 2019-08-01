package com.wits.intercom.setting;

import org.linphone.LinphoneManager;

import com.keep.lin.R;
import com.wits.intercom.CallPageActivity;
import com.wits.intercom.Constance;
import com.wits.intercom.LinphoneWelcomeActivity;
import com.wits.intercom.MyApplication;
import com.wits.intercom.PasswordActivity;
import com.wits.intercom.PhoneBookActivity;


import com.wits.intercom.dialing.EditToCallActivity;

import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;

public class SettingActivity extends FragmentActivity implements OnTouchListener {
	
protected int CLEAR_INPUT_MESSAGE = 10;
private ImageView backImageView;	
private Handler mHandle = new Handler() {
		public void dispatchMessage(android.os.Message msg) {
			Log.w(Constance.LOGTAG, "get message");

			if(msg.what == CLEAR_INPUT_MESSAGE ){
				LinphoneManager.isInSetting = false;
				startActivity(new Intent(SettingActivity.this,LinphoneWelcomeActivity.class));
			}
			
		};
	};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setting);
		backImageView = (ImageView)findViewById(R.id.back_home);
		backImageView.setOnTouchListener(this);
	}
	
	@Override  
	public boolean dispatchKeyEvent(KeyEvent event) {  
	   // TODO Auto-generated method stub  
	   // 判断普通按键  
	   int keyCode = event.getKeyCode();
	   if(event.getAction() == KeyEvent.ACTION_DOWN){
		   mHandle.removeMessages(CLEAR_INPUT_MESSAGE);
		   Log.d("Key Event", "Key code = "+keyCode);
		if(keyCode == 19){                       //left up
			   Intent mBlockPasswordIntent = new Intent(this,CheckPasswordActivity.class);
			   mBlockPasswordIntent.putExtra("type", "block");
			   startActivity(mBlockPasswordIntent);
			   return true;
			   
		   }else if(keyCode == 20){            //left down
			   LinphoneManager.isInSetting = false;
			   startActivity(new Intent(SettingActivity.this,LinphoneWelcomeActivity.class));
			   return true;
			   
		   }else if(keyCode == 21){          //right up
			   Intent mSystenPasswordIntent = new Intent(this,CheckPasswordActivity.class);
			   mSystenPasswordIntent.putExtra("type", "system");
			   startActivity(mSystenPasswordIntent);
			   return true;
			   
		   }else if(keyCode == 22){        //right down
			   Intent mBlockPasswordIntent = new Intent(this,CheckPasswordActivity.class);
			   mBlockPasswordIntent.putExtra("type", "parameters");
			   startActivity(mBlockPasswordIntent);
			   return true;
			   
		   }
	   }
	   return super.dispatchKeyEvent(event);
	   
	}
	
	@Override
	protected void onPause(){
		super.onPause();
		mHandle.removeMessages(CLEAR_INPUT_MESSAGE);
	}
	
	@Override 
	protected void onResume(){
		super.onResume();
		mHandle.sendEmptyMessageDelayed(CLEAR_INPUT_MESSAGE, 10000);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		LinphoneManager.isInSetting = false;
		 ComponentName mComponentName  = new ComponentName("com.android.launcher", "com.android.launcher2.Launcher");
		 Intent mIntent = new Intent();
		 mIntent.setComponent(mComponentName);
		 startActivity(mIntent);
		return true;
	}

}
