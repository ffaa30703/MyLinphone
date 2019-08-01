package com.wits.intercom.setting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Locale;

import org.linphone.LinphoneManager;
import org.linphone.ui.witsui.Constant;

import com.keep.lin.R;
import com.wits.intercom.CallPageActivity;
import com.wits.intercom.Constance;
import com.wits.intercom.LinphoneWelcomeActivity;
import com.wits.intercom.MyApplication;
import com.wits.intercom.PasswordActivity;
import com.wits.intercom.PhoneBookActivity;

import com.wits.intercom.dialing.EditToCallActivity;
import com.wits.linphone.LinphoneI2CInterface;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.R.integer;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.TextView;

public class UserInputActivity extends FragmentActivity implements OnTouchListener {
	
	private Handler mHandler;
	private final int RESET_KEY_DOWN  = 10;
	
	private int seriousDownTimes = 0;
	private  StringBuffer inputStringBuffer;
	private String temPasswordString = "";
	private int lastKeyCode = 0;
	private TextView mInpuTextView;
	private ImageView mOkImageView,mCancelImageView;
	private String typeString;
	private int pressOkTimes = 0;
	private SharedPreferences mSharedPreferences;
	private Editor mEditor;
	
	protected LinphoneI2CInterface mI2cInterface;
	
	private boolean onlyNumber = false;
	private boolean inputIsFinished = false;
	protected int CLEAR_INPUT_MESSAGE = 10;
	private Handler mHandle = new Handler() {
		public void dispatchMessage(android.os.Message msg) {
			Log.w(Constance.LOGTAG, "get message");

			if(msg.what == CLEAR_INPUT_MESSAGE ){
				LinphoneManager.isInSetting = false;
				startActivity(new Intent(UserInputActivity.this,LinphoneWelcomeActivity.class));
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
		setContentView(R.layout.activity_user_input);
		mInpuTextView = (TextView)findViewById(R.id.input_view);
		mOkImageView = (ImageView)findViewById(R.id.dialog_save);
		mCancelImageView = (ImageView)findViewById(R.id.dialog_cancel);
		mOkImageView.setOnTouchListener(this);
		mCancelImageView.setOnTouchListener(this);
		
		mSharedPreferences=getSharedPreferences("deviceInfo", 0);
		mEditor = mSharedPreferences.edit();
		
		mI2cInterface = new LinphoneI2CInterface();
		
		typeString = getIntent().getStringExtra("type");
		if(typeString != null){
			if(typeString.equals("ADD_CARD")){
				onlyNumber = true;
				mInpuTextView.setText(getResources().getString(R.string.enter_room_number_dialog));
				
			}else if(typeString.equals("DELETE_CARD")){
				onlyNumber = true;
				mInpuTextView.setText(getResources().getString(R.string.enter_room_number_delete));
			}else if(typeString.equals("EDIT_NAME")){
				mInpuTextView.setText(getResources().getString(R.string.enter_room_number_dialog));
				onlyNumber = true;
			}else if(typeString.equals("DELETE_PHONE_NAME")){
				onlyNumber = true;
				mInpuTextView.setText(getResources().getString(R.string.enter_room_number_dialog));
			}else if(typeString.equals("SET_APARTMENT_NAME")){
				mInpuTextView.setText(getResources().getString(R.string.enter_apartment_number));
			}else if(typeString.equals("SET_BLOCK_NAME")){
				mInpuTextView.setText(getResources().getString(R.string.enter_door_block_number));
			}else if(typeString.equals("SET_BLOCK_PASSWORD") || typeString.equals("SET_SYSTEM_PASSWORD")){
				onlyNumber = true;
				mInpuTextView.setText(getResources().getString(R.string.enter_old_password));
			}else if(typeString.equals("SET_DOOR_DURATION")){
				onlyNumber = true;
				String releaseDurationString = mSharedPreferences.getString("doorDuration", "1");
				mInpuTextView.setText(String.format(getResources().getString(R.string.set_door_release_duration), releaseDurationString));
			}else if(typeString.equals("SET_IP")){
				onlyNumber = true;
				mInpuTextView.setText(getResources().getString(R.string.enter_system_block_number));
			}
		}
		
		inputStringBuffer = new StringBuffer();
		mHandler = new Handler(){
			public void dispatchMessage(android.os.Message msg) {
				switch (msg.what) {
				case RESET_KEY_DOWN:
					seriousDownTimes = 0;
					
					break;

				default:
					break;
				}
			}

		};
		
	}
	
	
	@Override  
	public boolean dispatchKeyEvent(KeyEvent event) {  
	   // TODO Auto-generated method stub  
	   // 判断普通按键  
	   int keyCode = event.getKeyCode();
	   String inputString ;
	   if(event.getAction() == KeyEvent.ACTION_DOWN){
		   
		   Log.d("Key Event", "Key code = "+keyCode);
		   if(keyCode == 7 && !inputIsFinished){
			   mHandler.removeMessages(RESET_KEY_DOWN);
			   mHandler.sendEmptyMessageDelayed(RESET_KEY_DOWN,2000);
			   
			   if(lastKeyCode != 7 || onlyNumber){
				   seriousDownTimes = 0;
			   }
			   lastKeyCode = keyCode;
			   
			   switch (seriousDownTimes) {
				case 0:
					seriousDownTimes++;
					inputString = "0";
					inputStringBuffer.append(inputString);
					mInpuTextView.append(inputString);
					mInpuTextView.setText(inputStringBuffer);
					break;
				case 1:
					seriousDownTimes = 0;
					inputString = ".";
					inputStringBuffer.deleteCharAt(inputStringBuffer.length()-1);
					inputStringBuffer.append(inputString);
					mInpuTextView.setText(inputStringBuffer);
					break;
				default:
					break;
				}
			   return true;
			   
		   }else if(keyCode == 8 && !inputIsFinished){
			   mHandler.removeMessages(RESET_KEY_DOWN);
			   mHandler.sendEmptyMessageDelayed(RESET_KEY_DOWN,2000);
			   
			   if(lastKeyCode != 8 || onlyNumber){
				   seriousDownTimes = 0;
			   }
			   lastKeyCode = keyCode;
			   switch (seriousDownTimes) {
				case 0:
					seriousDownTimes++;
					inputString = "1";
					inputStringBuffer.append(inputString);
					mInpuTextView.setText(inputStringBuffer);
					break;
				case 1:
					seriousDownTimes = 0;
					inputString = " ";
					inputStringBuffer.deleteCharAt(inputStringBuffer.length()-1);
					inputStringBuffer.append(inputString);
					mInpuTextView.setText(inputStringBuffer);
					break;
				default:
					break;
				}
			   return true;
			   
		   }else if(keyCode == 9 && !inputIsFinished){
			   mHandler.removeMessages(RESET_KEY_DOWN);
			   mHandler.sendEmptyMessageDelayed(RESET_KEY_DOWN,2000);
			   
			   if(lastKeyCode != 9 || onlyNumber){
				   seriousDownTimes = 0;
			   }
			   lastKeyCode = keyCode;
			   switch (seriousDownTimes) {
				case 0:
					seriousDownTimes++;
					inputString = "2";
					inputStringBuffer.append(inputString);
					mInpuTextView.setText(inputStringBuffer);
					break;
				case 1:
					seriousDownTimes++;
					inputString = "A";
					inputStringBuffer.deleteCharAt(inputStringBuffer.length()-1);
					inputStringBuffer.append(inputString);
					mInpuTextView.setText(inputStringBuffer);
					
					break;
				case 2:
					seriousDownTimes++;
					inputString = "B";
					inputStringBuffer.deleteCharAt(inputStringBuffer.length()-1);
					inputStringBuffer.append(inputString);
					mInpuTextView.setText(inputStringBuffer);
					break;
				case 3:
					seriousDownTimes = 0;
					inputString = "C";
					inputStringBuffer.deleteCharAt(inputStringBuffer.length()-1);
					inputStringBuffer.append(inputString);
					mInpuTextView.setText(inputStringBuffer);
					break;
				default:
					break;
				}
			   return true;
			   
		   }else if(keyCode == 10 && !inputIsFinished){
			   mHandler.removeMessages(RESET_KEY_DOWN);
			   mHandler.sendEmptyMessageDelayed(RESET_KEY_DOWN,2000);
			   
			   if(lastKeyCode != 10 || onlyNumber){
				   seriousDownTimes = 0;
			   }
			   lastKeyCode = keyCode;
			   switch (seriousDownTimes) {
				case 0:
					seriousDownTimes++;
					inputString = "3";
					inputStringBuffer.append(inputString);
					mInpuTextView.setText(inputStringBuffer);
					break;
				case 1:
					seriousDownTimes++;
					inputString = "D";
					inputStringBuffer.deleteCharAt(inputStringBuffer.length()-1);
					inputStringBuffer.append(inputString);
					mInpuTextView.setText(inputStringBuffer);
					
					break;
				case 2:
					seriousDownTimes++;
					inputString = "E";
					inputStringBuffer.deleteCharAt(inputStringBuffer.length()-1);
					inputStringBuffer.append(inputString);
					mInpuTextView.setText(inputStringBuffer);
					break;
				case 3:
					seriousDownTimes = 0;
					inputString = "F";
					inputStringBuffer.deleteCharAt(inputStringBuffer.length()-1);
					inputStringBuffer.append(inputString);
					mInpuTextView.setText(inputStringBuffer);
					break;
				default:
					break;
				}
			   return true;
			   
		   }else if(keyCode == 11 && !inputIsFinished){
			   mHandler.removeMessages(RESET_KEY_DOWN);
			   mHandler.sendEmptyMessageDelayed(RESET_KEY_DOWN,2000);
			   
			   if(lastKeyCode != 11 || onlyNumber){
				   seriousDownTimes = 0;
			   }
			   lastKeyCode = keyCode;
			   switch (seriousDownTimes) {
				case 0:
					seriousDownTimes++;
					inputString = "4";
					inputStringBuffer.append(inputString);
					mInpuTextView.setText(inputStringBuffer);
					break;
				case 1:
					seriousDownTimes++;
					inputString = "G";
					inputStringBuffer.deleteCharAt(inputStringBuffer.length()-1);
					inputStringBuffer.append(inputString);
					mInpuTextView.setText(inputStringBuffer);
					
					break;
				case 2:
					seriousDownTimes++;
					inputString = "H";
					inputStringBuffer.deleteCharAt(inputStringBuffer.length()-1);
					inputStringBuffer.append(inputString);
					mInpuTextView.setText(inputStringBuffer);
					break;
				case 3:
					seriousDownTimes = 0;
					inputString = "I";
					inputStringBuffer.deleteCharAt(inputStringBuffer.length()-1);
					inputStringBuffer.append(inputString);
					mInpuTextView.setText(inputStringBuffer);
					break;
				default:
					break;
				}
			   return true;
			   
		   }else if(keyCode == 12 && !inputIsFinished){
			   mHandler.removeMessages(RESET_KEY_DOWN);
			   mHandler.sendEmptyMessageDelayed(RESET_KEY_DOWN,2000);
			   
			   if(lastKeyCode != 12 || onlyNumber){
				   seriousDownTimes = 0;
			   }
			   lastKeyCode = keyCode;
			   switch (seriousDownTimes) {
				case 0:
					seriousDownTimes++;
					inputString = "5";
					inputStringBuffer.append(inputString);
					mInpuTextView.setText(inputStringBuffer);
					break;
				case 1:
					seriousDownTimes++;
					inputString = "J";
					inputStringBuffer.deleteCharAt(inputStringBuffer.length()-1);
					inputStringBuffer.append(inputString);
					mInpuTextView.setText(inputStringBuffer);
					
					break;
				case 2:
					seriousDownTimes++;
					inputString = "K";
					inputStringBuffer.deleteCharAt(inputStringBuffer.length()-1);
					inputStringBuffer.append(inputString);
					mInpuTextView.setText(inputStringBuffer);
					break;
				case 3:
					seriousDownTimes = 0;
					inputString = "L";
					inputStringBuffer.deleteCharAt(inputStringBuffer.length()-1);
					inputStringBuffer.append(inputString);
					mInpuTextView.setText(inputStringBuffer);
					break;
				default:
					break;
				}
			   return true;
			   
		   }else if(keyCode == 13 && !inputIsFinished ){
			   mHandler.removeMessages(RESET_KEY_DOWN);
			   mHandler.sendEmptyMessageDelayed(RESET_KEY_DOWN,2000);
			   
			   if(lastKeyCode != 13 || onlyNumber){
				   seriousDownTimes = 0;
			   }
			   lastKeyCode = keyCode;
			   switch (seriousDownTimes) {
				case 0:
					seriousDownTimes++;
					inputString = "6";
					inputStringBuffer.append(inputString);
					mInpuTextView.setText(inputStringBuffer);
					break;
				case 1:
					seriousDownTimes++;
					inputString = "M";
					inputStringBuffer.deleteCharAt(inputStringBuffer.length()-1);
					inputStringBuffer.append(inputString);
					mInpuTextView.setText(inputStringBuffer);
					
					break;
				case 2:
					seriousDownTimes++;
					inputString = "N";
					inputStringBuffer.deleteCharAt(inputStringBuffer.length()-1);
					inputStringBuffer.append(inputString);
					mInpuTextView.setText(inputStringBuffer);
					break;
				case 3:
					seriousDownTimes = 0;
					inputString = "O";
					inputStringBuffer.deleteCharAt(inputStringBuffer.length()-1);
					inputStringBuffer.append(inputString);
					mInpuTextView.setText(inputStringBuffer);
					break;
				default:
					break;
				}
			   return true;
			   
		   }else if(keyCode == 14 && !inputIsFinished){
			   mHandler.removeMessages(RESET_KEY_DOWN);
			   mHandler.sendEmptyMessageDelayed(RESET_KEY_DOWN,2000);
			   
			   if(lastKeyCode != 14 || onlyNumber){
				   seriousDownTimes = 0;
			   }
			   lastKeyCode = keyCode;
			   switch (seriousDownTimes) {
				case 0:
					seriousDownTimes++;
					inputString = "7";
					inputStringBuffer.append(inputString);
					mInpuTextView.setText(inputStringBuffer);
					break;
				case 1:
					seriousDownTimes++;
					inputString = "P";
					inputStringBuffer.deleteCharAt(inputStringBuffer.length()-1);
					inputStringBuffer.append(inputString);
					mInpuTextView.setText(inputStringBuffer);
					
					break;
				case 2:
					seriousDownTimes++;
					inputString = "Q";
					inputStringBuffer.deleteCharAt(inputStringBuffer.length()-1);
					inputStringBuffer.append(inputString);
					mInpuTextView.setText(inputStringBuffer);
					break;
				case 3:
					seriousDownTimes++;
					inputString = "R";
					inputStringBuffer.deleteCharAt(inputStringBuffer.length()-1);
					inputStringBuffer.append(inputString);
					mInpuTextView.setText(inputStringBuffer);
					break;
				case 4:
					seriousDownTimes = 0;
					inputString = "S";
					inputStringBuffer.deleteCharAt(inputStringBuffer.length()-1);
					inputStringBuffer.append(inputString);
					mInpuTextView.setText(inputStringBuffer);
					break;
				default:
					break;
				}
			   return true;
			   
		   }else if(keyCode == 15 && !inputIsFinished){
			   mHandler.removeMessages(RESET_KEY_DOWN);
			   mHandler.sendEmptyMessageDelayed(RESET_KEY_DOWN,2000);
			   
			   if(lastKeyCode != 15 || onlyNumber){
				   seriousDownTimes = 0;
			   }
			   lastKeyCode = keyCode;
			   switch (seriousDownTimes) {
				case 0:
					seriousDownTimes++;
					inputString = "8";
					inputStringBuffer.append(inputString);
					mInpuTextView.setText(inputStringBuffer);
					break;
				case 1:
					seriousDownTimes++;
					inputString = "T";
					inputStringBuffer.deleteCharAt(inputStringBuffer.length()-1);
					inputStringBuffer.append(inputString);
					mInpuTextView.setText(inputStringBuffer);
					
					break;
				case 2:
					seriousDownTimes++;
					inputString = "U";
					inputStringBuffer.deleteCharAt(inputStringBuffer.length()-1);
					inputStringBuffer.append(inputString);
					mInpuTextView.setText(inputStringBuffer);
					break;
				case 3:
					seriousDownTimes = 0;
					inputString = "V";
					inputStringBuffer.deleteCharAt(inputStringBuffer.length()-1);
					inputStringBuffer.append(inputString);
					mInpuTextView.setText(inputStringBuffer);
					break;
				default:
					break;
				}
			   return true;
			   
		   }else if(keyCode == 16 && !inputIsFinished){
			   mHandler.removeMessages(RESET_KEY_DOWN);
			   mHandler.sendEmptyMessageDelayed(RESET_KEY_DOWN,2000);
			   
			   if(lastKeyCode != 16 || onlyNumber){
				   seriousDownTimes = 0;
			   }
			   lastKeyCode = keyCode;
			   switch (seriousDownTimes) {
				case 0:
					seriousDownTimes++;
					inputString = "9";
					inputStringBuffer.append(inputString);
					mInpuTextView.setText(inputStringBuffer);
					break;
				case 1:
					seriousDownTimes++;
					inputString = "W";
					inputStringBuffer.deleteCharAt(inputStringBuffer.length()-1);
					inputStringBuffer.append(inputString);
					mInpuTextView.setText(inputStringBuffer);
					
					break;
				case 2:
					seriousDownTimes++;
					inputString = "X";
					inputStringBuffer.deleteCharAt(inputStringBuffer.length()-1);
					inputStringBuffer.append(inputString);
					mInpuTextView.setText(inputStringBuffer);
					break;
				case 3:
					seriousDownTimes++;
					inputString = "Y";
					inputStringBuffer.deleteCharAt(inputStringBuffer.length()-1);
					inputStringBuffer.append(inputString);
					mInpuTextView.setText(inputStringBuffer);
					break;
				case 4:
					seriousDownTimes = 0;
					inputString = "Z";
					inputStringBuffer.deleteCharAt(inputStringBuffer.length()-1);
					inputStringBuffer.append(inputString);
					mInpuTextView.setText(inputStringBuffer);
					break;
				default:
					break;
				}
			   return true;
			   
		   }else if(keyCode == 66 ){          //right ok key
			   
			   if(typeString.equals("ADD_CARD")){
				   switch (pressOkTimes) {
					case 0:
						String roomNumberString = inputStringBuffer.toString();
						   int roomNumber = Integer.parseInt(roomNumberString);
						   if((roomNumber>=1 && roomNumber <= 799) && inputStringBuffer.length() == 3){
							Intent mUserShowMessageIntent = new Intent(UserInputActivity.this,UserShowMessageActivity.class);
							mUserShowMessageIntent.putExtra("type", "SHOW_CARD");
							mUserShowMessageIntent.putExtra("roomNumber", inputStringBuffer.toString());
							startActivity(mUserShowMessageIntent);
						   }else{
							   mInpuTextView.setText(R.string.invalid_number);
							   inputStringBuffer.delete(0, inputStringBuffer.length());
							   return true;
						   }
						break;
					case 1:
						
						break;
					case 2:
						
						break;
						
					default:
						break;
					}
			   }else if(typeString.equals("DELETE_CARD")){
				   String roomNumberString = inputStringBuffer.toString();
				   int roomNumber = Integer.parseInt(roomNumberString);
				   if((roomNumber>=1 && roomNumber <= 800) && inputStringBuffer.length() == 3){
					   Intent mUserCheckIntent = new Intent(UserInputActivity.this,UserCheckActivity.class);
					   mUserCheckIntent.putExtra("type", "DELETE_CARD");
					   mUserCheckIntent.putExtra("roomNumber", roomNumberString);
					   startActivity(mUserCheckIntent);
				   }else{
					   mInpuTextView.setText(R.string.invalid_number);
					   inputStringBuffer.delete(0, inputStringBuffer.length());
					   return true;
				   }
				   
				}else if(typeString.equals("EDIT_NAME")){
				   switch (pressOkTimes) {
					case 0:
						mEditor.putString("roomNum", inputStringBuffer.toString());
						String phoneNameString = checkPhoneNameFromSdcard(inputStringBuffer.toString());
						
						if(phoneNameString != null){
							String[] sepDateStrings = phoneNameString.split(" ", 2);
							
							mInpuTextView.setText(sepDateStrings[1]);
							inputStringBuffer.delete(0, inputStringBuffer.length());
							inputStringBuffer.append(sepDateStrings[1]);
							onlyNumber = false;
						}else{
							mInpuTextView.setText(R.string.no_this_contactor);
							inputStringBuffer.delete(0, inputStringBuffer.length());
							return true;
						}
						
						break;
					case 1:
						mInpuTextView.setText(getResources().getString(R.string.touch_save_button));
						break;
					default:
						break;
					}
			   }else if(typeString.equals("DELETE_PHONE_NAME")){
				   String phoneNameString = null;
				   switch (pressOkTimes) {
					case 0:
						phoneNameString = checkPhoneNameFromSdcard(inputStringBuffer.toString());
						if(phoneNameString != null){
							mInpuTextView.setText(phoneNameString);
						}else{
							mInpuTextView.setText(R.string.no_this_contactor);
							inputStringBuffer.delete(0, inputStringBuffer.length());
							return true;
						}
						break;
					case 1:
//						Intent mUserCheckIntent = new Intent(UserInputActivity.this,UserCheckActivity.class);
//						mUserCheckIntent.putExtra("type", "DELETE_PHONE_NAME");
//						mUserCheckIntent.putExtra("phoneName", inputStringBuffer.toString());
//						startActivity(mUserCheckIntent);
						break;
					default:
						break;
					}
			   }else if(typeString.equals("SET_APARTMENT_NAME")){
				   switch (pressOkTimes) {
				   	case 0:
						mEditor.putString("apartment_number", inputStringBuffer.toString());
						if(inputStringBuffer.length() <= 3){
						inputStringBuffer.delete(0, inputStringBuffer.length());
						mInpuTextView.setText(getResources().getString(R.string.enter_apartment_name));
						}else{
							mInpuTextView.setText(R.string.invalid_number);
							inputStringBuffer.delete(0, inputStringBuffer.length());
							return true;
						}
						break;
				   	case 1:
				   		if(inputStringBuffer.length() <= 16){
				   			mEditor.putString("apartment_name", inputStringBuffer.toString());
				   		}else{
				   			mInpuTextView.setText(R.string.invalid_number);
							inputStringBuffer.delete(0, inputStringBuffer.length());
							return true;
				   		}
				   		break;
					default:
						break;
					}
			   }else if(typeString.equals("SET_BLOCK_NAME")){
				   if(inputStringBuffer.length() <= 3){
						mInpuTextView.setText(inputStringBuffer.toString());
						mEditor.putString("blockNo", inputStringBuffer.toString());
				   }else{
					   mInpuTextView.setText(R.string.invalid_number);
					   inputStringBuffer.delete(0, inputStringBuffer.length());
					   return true;
				   }
					
				}else if(typeString.equals("SET_BLOCK_PASSWORD") || typeString.equals("SET_SYSTEM_PASSWORD")){
					
					switch (pressOkTimes) {
					case 0:
						
						String oldPasswordString = "";
						
						if(typeString.equals("SET_BLOCK_PASSWORD")){
							oldPasswordString = mSharedPreferences.getString(Constant.BLOCK_PASSWORD, "0000");
						}else if(typeString.equals("SET_SYSTEM_PASSWORD")){
							oldPasswordString = mSharedPreferences.getString(Constant.SYSTEM_PASSWORD, "0000");
						}
						
						if(oldPasswordString.equals(inputStringBuffer.toString())){
							inputStringBuffer.delete(0, inputStringBuffer.length());

							mInpuTextView.setText(R.string.enter_new_password);
						}else {
							inputStringBuffer.delete(0, inputStringBuffer.length());

							mInpuTextView.setText(R.string.enter_old_password);
							return true;
						}
						break;
					case 1:
						if(inputStringBuffer.length() == 4 ){
							if(isValidPassword(inputStringBuffer)){
								temPasswordString = inputStringBuffer.toString();
								inputStringBuffer.delete(0, inputStringBuffer.length());
								mInpuTextView.setText(R.string.reenter_new_password);
							}else{
								inputStringBuffer.delete(0, inputStringBuffer.length());
								mInpuTextView.setText(R.string.invalid_number);
								return true;
							}
						}else{
							inputStringBuffer.delete(0, inputStringBuffer.length());
							mInpuTextView.setText(R.string.invalid_number);
							return true;
						}
						break;
					case 2:
						mInpuTextView.setText(R.string.touch_save_button);
						if(temPasswordString.equals(inputStringBuffer.toString())){
							if(typeString.equals("SET_BLOCK_PASSWORD")){
								mEditor.putString(Constant.BLOCK_PASSWORD, inputStringBuffer.toString());
							}else if(typeString.equals("SET_SYSTEM_PASSWORD")){
								mEditor.putString(Constant.SYSTEM_PASSWORD, inputStringBuffer.toString());
							}
						}else{
							inputStringBuffer.delete(0, inputStringBuffer.length());
							mInpuTextView.setText(R.string.enter_new_password);
							pressOkTimes = 1;
							return true;
						}
						break;
					default:
						break;
					}
					
				}else if(typeString.equals("SET_DOOR_DURATION")){
					int duration = Integer.parseInt(inputStringBuffer.toString());
					if(duration >=1 && duration <= 99){
					mEditor.putString("doorDuration", inputStringBuffer.toString());
					mInpuTextView.setText(String.format(getResources().getString(R.string.touch_save_button), inputStringBuffer.toString()));
					inputIsFinished  = true;
					}else{
						mInpuTextView.setText(R.string.invalid_number);
						inputStringBuffer.delete(0, inputStringBuffer.length());
						return true;
					}
				}else if(typeString.equals("SET_IP")){
					switch (pressOkTimes) {
					case 0:
						int blockNum = Integer.parseInt(inputStringBuffer.toString());
						if(blockNum >=1 && blockNum <= 99){
							mEditor.putString("systemBlockNo", inputStringBuffer.toString());
							inputStringBuffer.delete(0, inputStringBuffer.length());
							mInpuTextView.setText(R.string.enter_system_door_number);
						}else{
							mInpuTextView.setText(R.string.out_of_limits);
							inputStringBuffer.delete(0, inputStringBuffer.length());
							return true;
						}
						break;
					case 1:
						int roomNum = Integer.parseInt(inputStringBuffer.toString());
						if(roomNum >=1 && roomNum <= 19){
						mEditor.putString("systemRoomNo", inputStringBuffer.toString());
						mInpuTextView.setText(R.string.touch_save_button);
						}else{
							mInpuTextView.setText(R.string.out_of_limits);
							inputStringBuffer.delete(0, inputStringBuffer.length());
							return true;
						}
						break;
					default:
						break;
					}
				}
				
			   pressOkTimes++;
			   Log.d("UserInput","ok down number = "+pressOkTimes);
			   return true;
			   
		   }else if(keyCode == 67){            //left  delete character
			   inputStringBuffer.deleteCharAt(inputStringBuffer.length()-1);
			   mInpuTextView.setText(inputStringBuffer);
			   return true;
			   
		   }if(keyCode == 19){                       //left up   ok
			   if(typeString.equals("SET_APARTMENT_NAME") 
					   || typeString.equals("SET_BLOCK_NAME") 
					   || typeString.equals("SET_BLOCK_PASSWORD") || typeString.equals("SET_SYSTEM_PASSWORD")){
					mEditor.commit();
					finish();
				}else if(typeString.equals("SET_DOOR_DURATION")){
					mEditor.commit();
					int resultSetDuration = mI2cInterface.SetDoorOpener(Integer.parseInt(inputStringBuffer.toString()));
					Log.d("SetDoorDuration", " result = "+resultSetDuration);
					finish();
				}else if(typeString.equals("SET_IP")){
					mEditor.commit();
					getIPAndMacAddress(mSharedPreferences.getString("systemBlockNo", "01"),mSharedPreferences.getString("systemRoomNo", "01"));
				}else if(typeString.equals("EDIT_NAME")){
					mEditor.commit();
					String roomNumberString = mSharedPreferences.getString("roomNum", "");
					editPhoneName(roomNumberString,inputStringBuffer.toString());
					finish();
					
				}else if(typeString.equals("DELETE_PHONE_NAME")){
//					deletePhoneNameFromSdcard(inputStringBuffer.toString());
//					Intent mPhoneBookEditIntent = new Intent(UserInputActivity.this,PhoneBookSetupActivity.class);
//					startActivity(mPhoneBookEditIntent);
					Intent mUserCheckIntent = new Intent(UserInputActivity.this,UserCheckActivity.class);
					mUserCheckIntent.putExtra("type", "DELETE_PHONE_NAME");
					mUserCheckIntent.putExtra("phoneName", inputStringBuffer.toString());
					startActivity(mUserCheckIntent);
				}
			   return true;
			   
		   }else if(keyCode == 20){            //left down   cancel
			   finish();
			   return true;
			   
		   }
		   
	   }
	   return super.dispatchKeyEvent(event);
	   
	}
	
	private void deletePhoneNameFromSdcard(String roomNumber){
		String defaultLocal = Locale.getDefault().toString();
		File mDataFile;
		if(defaultLocal.startsWith("tr")){
			mDataFile = new File("/mnt/sdcard/DIP40_phonebook_tr.txt");
		}else{
			mDataFile = new File("/mnt/sdcard/DIP40_phonebook_en.txt");
		}
		InputStreamReader is;
		String line = null;
		StringBuffer data = new StringBuffer();
		try {
			is = new InputStreamReader(new FileInputStream(mDataFile));
			BufferedReader reader = new BufferedReader(is);
			while ((line = reader.readLine()) != null) {
	            if(line.startsWith(roomNumber)){
	            	continue;
	            }
	            data.append(line+"\r\n");
	        }
	        reader.close();
	        
	        FileOutputStream mFileOutputStream = new FileOutputStream(mDataFile);
	        OutputStreamWriter mOutputStreamWriter = new OutputStreamWriter(mFileOutputStream);
            mOutputStreamWriter.write(data.toString());
            mOutputStreamWriter.flush();
            mOutputStreamWriter.close();
	        
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
	}
	private boolean isValidPassword(StringBuffer inputData) {
		// TODO Auto-generated method stub
		String numString1 = inputData.substring(0, 1);
		String numString2 = inputData.substring(1, 2);
		String numString3 = inputData.substring(2, 3);
		String numString4 = inputData.substring(3, 4);
		int num1 = Integer.parseInt(numString1);
		int num2 = Integer.parseInt(numString2);
		int num3 = Integer.parseInt(numString3);
		int num4 = Integer.parseInt(numString4);
		if(((num1-num2) == 0 && (num2-num3) == 0 && (num3-num4) == 0)
				|| ((num1-num2) == 1 && (num2-num3) == 1 && (num3-num4) == 1)
				|| ((num1-num2) == -1 && (num2-num3) == -1 && (num3-num4) == -1)){
					return false;
				}
		return true;
	}


	private void getIPAndMacAddress(String blockNum,String roomNum) {
		// TODO Auto-generated method stub
		String IPAddress = "10.";
		String thirdPartString = "";
		String forthPartString = "";
		int block = Integer.parseInt(blockNum);
		int room = Integer.parseInt(roomNum);
		IPAddress += block;
		if (block < 99) {
			if (room >= 1 && room <= 19) {
					thirdPartString = "21";
					forthPartString = room + "";
			}
		} else if (block == 99) {
			if (room >= 1 && room <= 9) {
				thirdPartString = "29";
				forthPartString = room + ""; 
			} 
		}
		Log.i("ip", IPAddress + "." + thirdPartString + "." + forthPartString);
		
		String followMeNumberString  = IPAddress + "." + thirdPartString + "." + forthPartString;
		
		int thirdByte = Integer.parseInt(thirdPartString); 
		int forthByte = Integer.parseInt(forthPartString);
		String macAddressString = "";
		String blockHex = Integer.toHexString(block);
		String thirdPartHex = Integer.toHexString(thirdByte);      
		String forthPartHex = Integer.toHexString(forthByte);   
		if (blockHex.trim().length() != 2) {
			blockHex = "0" + blockHex;
		}
		if (thirdPartHex.trim().length() != 2) {
			thirdPartHex = "0" + thirdPartHex;
		}
		if (forthPartHex.trim().length() != 2) {
			forthPartHex = "0" + forthPartHex;
		}
		Log.i("ip", macAddressString + "-" + blockHex + "-" + thirdPartHex
				+ "-" + forthPartHex);
		IPAddress = IPAddress + "." + thirdPartString + "." + forthPartString
				+ "\r\n";
		macAddressString = macAddressString + ":" + blockHex + ":"
				+ thirdPartHex + ":" + forthPartHex;
		try {
			File ipAddressAndMacAddressFile = new File(
					"/mnt/sdcard/Address.txt");
			if (!ipAddressAndMacAddressFile.exists()) {
				ipAddressAndMacAddressFile.createNewFile();
			}
			FileOutputStream fos = new FileOutputStream(
					ipAddressAndMacAddressFile);
			fos.write((IPAddress + macAddressString).getBytes());
			fos.write("\r\n".getBytes("gbk"));
			Log.e("info", "=====================" + IPAddress);
			
			Intent intentSetIP = new Intent();
			intentSetIP.setAction("android.intent.action.GOTOSETIP");
			intentSetIP.putExtra("ip", IPAddress);
			sendBroadcast(intentSetIP);
			
			macAddressString = "02:A5:5A" + macAddressString;
			File ethmacFile = new File("/data/ethmac.info");
			FileOutputStream fStream = new FileOutputStream(ethmacFile);
			fStream.write(macAddressString.getBytes()); 

			sendBroadcast(new Intent("android.intent.action.GOTOREBOOT"));
//			MyApplication.getInstnce().close();
//			System.exit(0);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	};

	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.ok:
			if(typeString.equals("EDIT_NAME")){
				mEditor.commit();
				finish();
			}
			
			break;
		case R.id.cancel:
			finish();
			break;
		default:
			break;
		}
		return true;
	} 
	
	private String checkPhoneNameFromSdcard(String roomNumber){
		String defaultLocal = Locale.getDefault().toString();
		File mDataFile;
		if(defaultLocal.startsWith("tr")){
			mDataFile = new File("/mnt/sdcard/DIP40_phonebook_tr.txt");
		}else{
			mDataFile = new File("/mnt/sdcard/DIP40_phonebook_en.txt");
		}
		InputStreamReader is;
		String line = null;
		try {
			is = new InputStreamReader(new FileInputStream(mDataFile));
			BufferedReader reader = new BufferedReader(is);
			while ((line = reader.readLine()) != null) {
				String temString = line;
				String[] sepDateStrings = temString.split(" ", 2);
				int room = 0;
				if(sepDateStrings[0] != ""){
					room = Integer.parseInt(sepDateStrings[0].replaceAll("\\D+","").replaceAll("\r", "").replaceAll("\n", "").trim(),10);
				}
				Log.d("Read file", "room number = "+roomNumber+" line = "+line);
	            if(room == Integer.parseInt(roomNumber)){
	            	reader.close();
	            	return line;
	            }
	        }
	        
	        reader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
        
	}
	
	private void editPhoneName(String roomNumber,String name){
		String defaultLocal = Locale.getDefault().toString();
		File mDataFile;
		if(defaultLocal.startsWith("tr")){
			mDataFile = new File("/mnt/sdcard/DIP40_phonebook_tr.txt");
		}else{
			mDataFile = new File("/mnt/sdcard/DIP40_phonebook_en.txt");
		}
		Log.d("phone book", "get data from file "+mDataFile.getPath());
		InputStreamReader is;
		String line = null;
		StringBuffer data = new StringBuffer();
		try {
			is = new InputStreamReader(new FileInputStream(mDataFile));
			BufferedReader reader = new BufferedReader(is);
			while ((line = reader.readLine()) != null) {
				String temString = line;
				String[] sepDateStrings = temString.split(" ", 2);
				Log.d("phone book", "edit name room number = "+sepDateStrings[0]);
				int room = 0;
				if(sepDateStrings[0] != ""){
					room = Integer.parseInt(sepDateStrings[0].replaceAll("\\D+","").replaceAll("\r", "").replaceAll("\n", "").trim(),10);
				}
	            if(room == Integer.parseInt(roomNumber)){
	            	Log.d("phone book", "edit name = "+name);
	            	data.append(roomNumber+" "+name+"\r\n");
	            	continue;
	            }
	            data.append(line+"\r\n");
	        }
	        reader.close();
	        
	        FileOutputStream mFileOutputStream = new FileOutputStream(mDataFile);
	        OutputStreamWriter mOutputStreamWriter = new OutputStreamWriter(mFileOutputStream);
            mOutputStreamWriter.write(data.toString());
            mOutputStreamWriter.flush();
            mOutputStreamWriter.close();
            Log.d("phone book", "rewrite success");
	        
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	protected void onDestroy(){
		super.onDestroy();
//		sendBroadcast(new Intent("android.intent.action.GOTOREBOOT"));
//		System.exit(0);
	}

}
