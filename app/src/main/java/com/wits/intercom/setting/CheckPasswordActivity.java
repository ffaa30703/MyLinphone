package com.wits.intercom.setting;

import org.linphone.LinphoneManager;
import org.linphone.ui.witsui.Constant;

import com.keep.lin.R;
import com.wits.intercom.CheckPasswordStartActivity;
import com.wits.intercom.DeladyProcess.IdelayMethodrd;
import com.wits.intercom.CallPageActivity;
import com.wits.intercom.LinphoneWelcomeActivity;
import com.wits.intercom.MyApplication;
import com.wits.intercom.dialing.EditToCallActivity;
import com.wits.intercom.setting.GeneralSetupActivity;
import com.wits.intercom.util.Logger;
import com.wits.linphone.LinphoneI2CInterface;

import android.R.integer;
import android.R.string;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class CheckPasswordActivity extends FragmentActivity implements OnClickListener,IdelayMethodrd{
    private final String TAG = CheckPasswordActivity.class.getSimpleName();
	private String password,password1,password2,password3,password4;
	private SharedPreferences mSharedPreferences;
	private  StringBuffer passwordStringBuffer=new StringBuffer();
	private EditText password1EditText,password2EditText,password3EditText,password4EditText;
	private int fromActivityTag;
	
	private LinphoneI2CInterface mI2cInterface;
	
	private int downCount = 0;
	
	private String passwordType;
	
	private final int COMPARE_PASSWORD = 10;
	private final int TIME_OUT = 11;
	
	private  Handler mHandler=new  Handler(){
		public void handleMessage(Message msg) {
			
			if(msg.what == COMPARE_PASSWORD){
					
					if(passwordType.equals("door")){
						password=mSharedPreferences.getString(Constant.DOOR_PANER_PASSWORD, "0000");
						Intent intet=new Intent(CheckPasswordActivity.this,CheckPasswordStartActivity.class);
						Bundle bundle=new Bundle();
						if(password.equals(passwordStringBuffer.toString())){
							mI2cInterface.SetDoorRelease();
							bundle.putBoolean(Constant.PASSWORD_CHECKED_RESULT, true);
						
						}else{
							bundle.putBoolean(Constant.PASSWORD_CHECKED_RESULT, false);
						}
						bundle.putInt(Constant.ENTRY_TAG,fromActivityTag);
						intet.putExtras(bundle);
						startActivity(intet);
					}else if(passwordType.equals("block")){
						password=mSharedPreferences.getString(Constant.BLOCK_PASSWORD, "0000");
						if(password.equals(passwordStringBuffer.toString())){
							Intent mGeneralSetupIntent = new Intent(CheckPasswordActivity.this,GeneralSetupActivity.class);
							startActivity(mGeneralSetupIntent);
						}else{
							Intent mMainIntent = new Intent(CheckPasswordActivity.this,LinphoneWelcomeActivity.class);
							startActivity(mMainIntent);
						}
					}else if(passwordType.equals("system")){
						password=mSharedPreferences.getString(Constant.SYSTEM_PASSWORD, "0000");
						Log.d("Settings", "system password = "+password);
						if(password.equals(passwordStringBuffer.toString())){
							
							Intent mSystemSetupIntent = new Intent(CheckPasswordActivity.this,SystemSetupActivity.class);
							startActivity(mSystemSetupIntent);
						}else{
							Intent mMainIntent = new Intent(CheckPasswordActivity.this,LinphoneWelcomeActivity.class);
							startActivity(mMainIntent);
						}
						
					}else if(passwordType.equals("parameters")){
						password=mSharedPreferences.getString(Constant.BLOCK_PASSWORD, "0000");
						if(password.equals(passwordStringBuffer.toString())){
							Intent mGeneralSetupIntent = new Intent(CheckPasswordActivity.this,ParametersSetupActivity.class);
							startActivity(mGeneralSetupIntent);
						}else{
							Intent mMainIntent = new Intent(CheckPasswordActivity.this,LinphoneWelcomeActivity.class);
							startActivity(mMainIntent);
						}
					}
					
			}else if(msg.what ==  TIME_OUT){
				startActivity(new Intent(CheckPasswordActivity.this,LinphoneWelcomeActivity.class));
			}
		};
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setup_password);
		passwordType = getIntent().getStringExtra("type");
		Log.d("password", "password type is "+passwordType);
		EditText mPasswordTypeEditText = (EditText)findViewById(R.id.password_type);
		if(passwordType.equals("door")){
			mPasswordTypeEditText.setText(R.string.door_password);
		}else if(passwordType.equals("block")){
			mPasswordTypeEditText.setText(R.string.block_password);
		}else if(passwordType.equals("system")){
			mPasswordTypeEditText.setText(R.string.system_password);
		}else if(passwordType.equals("parameters")){
			mPasswordTypeEditText.setText(R.string.block_password);
		}
		mI2cInterface = new LinphoneI2CInterface();
		
		if(passwordStringBuffer.length()!=0){passwordStringBuffer.delete(0, passwordStringBuffer.length()-1);}
		mSharedPreferences=getSharedPreferences("deviceInfo", 0);
		findViewAndOperation();
	}
	private void findViewAndOperation() {
		RelativeLayout relativeLayout=(RelativeLayout)findViewById(R.id.password_layout);
		relativeLayout.setOnClickListener(this);

		password1EditText=(EditText)findViewById(R.id.password1);
//		 password1EditText.addTextChangedListener(new password1watch());
		 
		password2EditText=(EditText)findViewById(R.id.password2);
//		password2EditText.addTextChangedListener(new password2watch());
		
		password3EditText=(EditText)findViewById(R.id.password3);
//		password3EditText.addTextChangedListener(new password3watch());
		
		password4EditText=(EditText)findViewById(R.id.password4);
//		password4EditText.addTextChangedListener(new password4watch());
	}
	@Override
	protected void onStart() {
		super.onStart();
//		fromActivityTag=getIntent().getIntExtra(Constant.ENTRY_TAG, Constant.INIT_HOME_ENTRY_TAG);
//		getPassword(fromActivityTag);
		
//		DeladyProcess.stopDelayThread();
//		DeladyProcess.doDelay();
//		DeladyProcess.getInstance(new Handler(), 15000, this);		
	}
	private void getPassword(int intExtra) {
		switch (intExtra) {
		//如果是从InitHomeActivity跳过来的
		case Constant.INIT_HOME_ENTRY_TAG:
			//TODO 暂时把开门密码设置在shardpreferce,考虑放到数据库中
			password=mSharedPreferences.getString(Constant.DOOR_PANER_PASSWORD, "0000");
			break;

		default:
			break;
		}
		
	}
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.password_layout:
//			Intent intetnIntent=new Intent(PasswordActivity.this,CheckPasswordStartActivity.class);
//			startActivity(intetnIntent);
			break;

		default:
			break;
		}
		
	}
	
	class password1watch implements TextWatcher{
	
		@Override
		public void afterTextChanged(Editable s) {
			Logger.e(TAG, "====password1=======afterTextChanged");
				password1=s.toString();	
				
				password2EditText.requestFocus();
//				Message message=new Message();
//				message.what=1;
//				mHandler.sendMessage(message);
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
			Logger.e(TAG, "====password1=======beforeTextChanged");
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			Logger.e(TAG, "====password1=======onTextChanged");
		}
	}
	class password2watch implements TextWatcher{
		
		@Override
		public void afterTextChanged(Editable s) {
				Log.i("zg", "afterTextChanged");
				
				Logger.e(TAG, "====password2watch=======afterTextChanged");
				if(!(password1==null||"".equals(password1))){
				password2=s.toString();
				password1EditText.setText("*");
				password3EditText.requestFocus();
//				Message message=new Message();
//				message.what=1;
//				mHandler.sendMessage(message);
				
				}else{
					Toast.makeText(CheckPasswordActivity.this, "Please input the first password", Toast.LENGTH_SHORT).show();
				}
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
			Logger.e(TAG, "====password2=======beforeTextChanged");
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			Logger.e(TAG, "====password2=======onTextChanged");
		}
	}
	
		class password3watch implements TextWatcher{
		
		@Override
		public void afterTextChanged(Editable s) {
				Log.i("zg", "afterTextChanged");
				if(!(password2==null||"".equals(password2))){
				password3=s.toString();
				password2EditText.setText("*");
				password4EditText.requestFocus();
//				Message message=new Message();
//				message.what=1;
//				mHandler.sendMessage(message);
				}else{
					Toast.makeText(CheckPasswordActivity.this, "Please input the secornd password", Toast.LENGTH_SHORT).show();
				}
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
		}
	}
		
		class password4watch implements TextWatcher{
			
			@Override
			public void afterTextChanged(Editable s) {
					Log.i("zg", "afterTextChanged");
					if(!(password3==null||"".equals(password3))){
					password4=s.toString();
					password3EditText.setText("*");
					Message message=new Message();
					message.what=1;
					mHandler.sendMessage(message);
					}else{
						Toast.makeText(CheckPasswordActivity.this, "Please input the third password", Toast.LENGTH_SHORT).show();
					}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}
		}
	
		@Override
		protected void onStop() {
			super.onStop();
//			DeladyProcess.stopDelayThread();
//		    passwordStringBuffer.delete(0, passwordStringBuffer.length()-1);
		}
		
		@Override
		protected void onDestroy() {
			super.onDestroy();
		}
		
		@Override
		public void finish() {
//			DeladyProcess.stopDelayThread();
			super.finish();
		}
		
		@Override
		public void delayProcess() {
//			startActivity(new Intent(PasswordActivity.this,LinphoneWelcomeActivity.class));			
		}
		
		@Override  
		public boolean dispatchKeyEvent(KeyEvent event) {  
		   // TODO Auto-generated method stub  
		   // 判断普通按键  
		   int keyCode = event.getKeyCode();
		   if(event.getAction() == KeyEvent.ACTION_DOWN){
			   
			   Log.d("Key Event", "Key code = "+keyCode);
			if(keyCode == 19){
				
				   return true;
				   
			   }else if(keyCode == 20){
				   
				   return true;
				   
			   }else if(keyCode == 21){
				 
				   return true;
				   
			   }else if(keyCode == 22){
				  
				   return true;
				   
			   }else if(keyCode == 66){
				  
				   return true;
				   
			   }else if(keyCode == 67){
				   if( passwordStringBuffer.length() != 0){
					   
					   switch (passwordStringBuffer.length()) {
						case 1:
							password1EditText.setText("");
							password1EditText.requestFocus();
							break;
						case 2:
							password2EditText.setText("");
							password2EditText.requestFocus();
							break;
						case 3:
							password3EditText.setText("");
							password3EditText.requestFocus();
							break;
						case 4:
							password4EditText.setText("");
							password4EditText.requestFocus();
							break;
						default:
							break;
					   }
					   passwordStringBuffer.deleteCharAt(passwordStringBuffer.length()-1);
					   downCount--;
					   
				   }else{
					   mHandler.removeMessages(TIME_OUT);
					   startActivity(new Intent(CheckPasswordActivity.this,SettingActivity.class));
				   }
				   return true;
				   
			   }else if(keyCode>=7 && keyCode<=16){
				   downCount++;
				   mHandler.removeMessages(TIME_OUT);
				   mHandler.sendEmptyMessageDelayed(TIME_OUT, 5000);
				   
				   String inputNumbetString = "";
				   
				   switch (keyCode) {
					case 7:
						inputNumbetString = "0";
						break;
					case 8:
						inputNumbetString = "1";
						break;
					case 9:
						inputNumbetString = "2";
						break;
					case 10:
						inputNumbetString = "3";
						break;
					case 11:
						inputNumbetString = "4";
						break;
					case 12:
						inputNumbetString = "5";
						break;
					case 13:
						inputNumbetString = "6";
						break;
					case 14:
						inputNumbetString = "7";
						break;
					case 15:
						inputNumbetString = "8";
						break;
					case 16:
						inputNumbetString = "9";
						break;
					default:
						inputNumbetString = "";
						break;
					}
				   
				   passwordStringBuffer.append(inputNumbetString);
				   switch (downCount) {
					case 1:
						password1EditText.setText("*");
						password2EditText.requestFocus();
						break;
					case 2:
						password2EditText.setText("*");
						password3EditText.requestFocus();
						break;
					case 3:
						password3EditText.setText("*");
						password4EditText.requestFocus();
						break;
					case 4:
						password4EditText.setText("*");
						mHandler.removeMessages(TIME_OUT);
						mHandler.sendEmptyMessage(COMPARE_PASSWORD);
						
//						password=mSharedPreferences.getString(Constant.DOOR_PANER_PASSWORD, "0000");
//							
//						Log.d("open door", "input password = "+passwordStringBuffer);
//						Intent intet=new Intent(PasswordActivity.this,CheckPasswordStartActivity.class);
//						Bundle bundle=new Bundle();
//						if(password.equals(passwordStringBuffer.toString())){
//									mI2cInterface.DoorRelease();
//						bundle.putBoolean(Constant.PASSWORD_CHECKED_RESULT, true);
//								
//						}else{
//								bundle.putBoolean(Constant.PASSWORD_CHECKED_RESULT, false);
//						}
//						bundle.putInt(Constant.ENTRY_TAG,fromActivityTag);
//						intet.putExtras(bundle);
//						startActivity(intet);
						
						break;
					default:
						break;
				   }
				   
				   return true;
			   }
		   }
		   return super.dispatchKeyEvent(event);
		   
		}
		
		@Override
		protected void onPause(){
			super.onPause();
			mHandler.removeMessages(TIME_OUT);
		}
		
		@Override 
		protected void onResume(){
			super.onResume();
			mHandler.sendEmptyMessageDelayed(TIME_OUT, 5000);
		}

}
