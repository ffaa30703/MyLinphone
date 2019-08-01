package com.wits.intercom.setting;

import org.linphone.LinphoneManager;

import com.keep.lin.R;
import com.wits.intercom.Constance;
import com.wits.intercom.LinphoneWelcomeActivity;
import com.wits.intercom.MyApplication;
import com.wits.linphone.LinphoneI2CInterface;

import android.os.Bundle;
import android.os.Handler;
import android.R.integer;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.widget.ImageView;

public class KeyColorActivity extends FragmentActivity {
	private String  type;
	private ImageView mRed;
	private ImageView mGreen;
	private ImageView mBlue;
	private boolean mRedSelected = false;
	private boolean mGreenSelected = false;
	private boolean mBlueSelected = false;
	private byte redColor = 0x00;
	private byte greenColor = 0x00;
	private byte blueColor = 0x00;
	private SharedPreferences mSharedPreferences;
	private Editor mEditor;
	private LinphoneI2CInterface mLinphoneI2CInterface;
	private int commandType;
	protected int CLEAR_INPUT_MESSAGE = 10;
	private Handler mHandle = new Handler() {
		public void dispatchMessage(android.os.Message msg) {
			Log.w(Constance.LOGTAG, "get message");

			if(msg.what == CLEAR_INPUT_MESSAGE ){
				LinphoneManager.isInSetting = false;
				startActivity(new Intent(KeyColorActivity.this,LinphoneWelcomeActivity.class));
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
		setContentView(R.layout.activity_key_color);
		mRed = (ImageView)findViewById(R.id.red);
		mGreen = (ImageView)findViewById(R.id.green);
		mBlue = (ImageView)findViewById(R.id.blue);
		
		mSharedPreferences=getSharedPreferences("deviceInfo", 0);
		mEditor = mSharedPreferences.edit();
		mLinphoneI2CInterface = new LinphoneI2CInterface();
		
		type = getIntent().getStringExtra("type");
		if(type.equals("key")){
			commandType = 0x11;
		}else if(type.equals("board")){
			commandType = 0x12;

		}
		
		mRedSelected = mSharedPreferences.getBoolean("red"+type, false);
		mGreenSelected = mSharedPreferences.getBoolean("green"+type, false);
		mBlueSelected = mSharedPreferences.getBoolean("blue"+type, false);
		
		if(mRedSelected){
			   mRed.setBackgroundResource(R.drawable.wits_red_selected);
			   
		 }else{
			   mRed.setBackgroundResource(R.drawable.wits_red);
		 }
		
		if(mGreenSelected){
			   mGreen.setBackgroundResource(R.drawable.wits_greenselected);
			   
		}else{
			   mGreen.setBackgroundResource(R.drawable.wits_green);
		}
		
		if(mBlueSelected){
			   mBlue.setBackgroundResource(R.drawable.wits_blue_selected);
			   
		}else{
			   mBlue.setBackgroundResource(R.drawable.wits_blue);
		}
		
		
	}

	@Override  
	public boolean dispatchKeyEvent(KeyEvent event) {  
	   // TODO Auto-generated method stub  
	   // 判断普通按键  
	   int keyCode = event.getKeyCode();
	   if(event.getAction() == KeyEvent.ACTION_DOWN){                //bit0 : blue, bit1 : green, bit2 : red
		   
		   Log.d("Key Event", "Key code = "+keyCode);
		   if(keyCode == 19){                       //left up  red
			   if(!mRedSelected){
				   mRedSelected = true;
				   mRed.setBackgroundResource(R.drawable.wits_red_selected);
				   redColor = 0x04;
				   
			   }else{
				   mRedSelected = false;
				   mRed.setBackgroundResource(R.drawable.wits_red);
				   redColor = 0x00;
			   }
			   mEditor.putBoolean("red"+type, mRedSelected);
			   mEditor.commit();
			   int resultSetColor = mLinphoneI2CInterface.SetLedRGB(commandType, redColor | greenColor | blueColor);
			   Log.d("Set Color", "set result = "+resultSetColor);
			   return true;
			   
		   }else if(keyCode == 20){            //left down
			   finish();
			   return true;
			   
		   }else if(keyCode == 21){          //right up green
			   if(!mGreenSelected){
				   mGreenSelected = true;
				   mGreen.setBackgroundResource(R.drawable.wits_greenselected);
				   greenColor = 0x02;
				   
			   }else{
				   mGreenSelected = false;
				   mGreen.setBackgroundResource(R.drawable.wits_green);
				   greenColor = 0x00;
			   }
			   
			   mEditor.putBoolean("green"+type, mGreenSelected);
			   mEditor.commit();
			   
			   mLinphoneI2CInterface.SetLedRGB(commandType, redColor | greenColor | blueColor);
			   return true;
			   
		   }else if(keyCode == 22){        //right down  blue
			   if(!mBlueSelected){
				   mBlueSelected = true;
				   mBlue.setBackgroundResource(R.drawable.wits_blue_selected);
				   blueColor = 0x01;
				   
			   }else{
				   mBlueSelected = false;
				   mBlue.setBackgroundResource(R.drawable.wits_blue);
				   blueColor = 0x00;
			   }
			   mEditor.putBoolean("blue"+type, mBlueSelected);
			   mEditor.commit();
			   mLinphoneI2CInterface.SetLedRGB(commandType, redColor | greenColor | blueColor);
			   return true;
			   
		   }
	   }
	   return super.dispatchKeyEvent(event);
	   
	} 

}
