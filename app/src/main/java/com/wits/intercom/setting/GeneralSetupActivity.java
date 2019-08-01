package com.wits.intercom.setting;

import org.linphone.LinphoneManager;

import com.keep.lin.R;
import com.wits.intercom.Constance;
import com.wits.intercom.LinphoneWelcomeActivity;
import com.wits.intercom.MyApplication;
import com.wits.intercom.PasswordActivity;


import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;

public class GeneralSetupActivity extends FragmentActivity {
	protected int CLEAR_INPUT_MESSAGE = 10;
	private Handler mHandle = new Handler() {
		public void dispatchMessage(android.os.Message msg) {
			Log.w(Constance.LOGTAG, "get message");

			if(msg.what == CLEAR_INPUT_MESSAGE ){
				LinphoneManager.isInSetting = false;
				startActivity(new Intent(GeneralSetupActivity.this,LinphoneWelcomeActivity.class));
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
		setContentView(R.layout.activity_general_setup);
	}

	@Override  
	public boolean dispatchKeyEvent(KeyEvent event) {  
	   // TODO Auto-generated method stub  
	   // 判断普通按键  
	   int keyCode = event.getKeyCode();
	   if(event.getAction() == KeyEvent.ACTION_DOWN){
		   
		   Log.d("Key Event", "Key code = "+keyCode);
		if(keyCode == 19){                       //left up
			   Intent mBlockPasswordIntent = new Intent(this,PhoneBookSetupActivity.class);
			   startActivity(mBlockPasswordIntent);
			   return true;
			   
		   }else if(keyCode == 20){            //left down
//			  MyApplication.getInstnce().back(2);
			   Intent mSettingIntent = new Intent(this,SettingActivity.class);
			   startActivity(mSettingIntent);
			   return true;
			   
		   }else if(keyCode == 21){          //right up
			   Intent mBlockPasswordIntent = new Intent(this,RoomSetupActivity.class);
			   startActivity(mBlockPasswordIntent);
			   return true;
			   
		   }else if(keyCode == 22){        //right down
			   Intent mBlockPasswordIntent = new Intent(this,BlockSetupActivity.class);
			   startActivity(mBlockPasswordIntent);
			   return true;
			   
		   }
	   }
	   return super.dispatchKeyEvent(event);
	   
	} 

}
