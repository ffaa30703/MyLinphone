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

import com.keep.lin.R;
import com.wits.intercom.Constance;
import com.wits.intercom.LinphoneWelcomeActivity;

import com.wits.linphone.LinphoneI2CInterface;

import android.os.Bundle;
import android.os.Handler;
import android.R.integer;
import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.TextureView;
import android.widget.TextView;

public class UserCheckActivity extends FragmentActivity {
	private String type;
	private String phoneNameString;
	private String roomNumberString;
	
	private final int BACK_TO_PREVIOUTS_PAGE = 10;

	private TextView mTitleTextureView;
	private int pressOkTimes = 0;
	
	protected LinphoneI2CInterface mI2cInterface;
	protected int CLEAR_INPUT_MESSAGE = 10;
	private Handler mHandle = new Handler() {
		public void dispatchMessage(android.os.Message msg) {
			Log.w(Constance.LOGTAG, "get message");

			if(msg.what == CLEAR_INPUT_MESSAGE ){
				LinphoneManager.isInSetting = false;
				startActivity(new Intent(UserCheckActivity.this,LinphoneWelcomeActivity.class));
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
		setContentView(R.layout.activity_user_check);
		mTitleTextureView = (TextView)findViewById(R.id.title);
		Log.d("ADD CARD", "User check creat");
		type = getIntent().getStringExtra("type");
		if(type != null){
			if(type.equals("ARE_YOU_SURE")){
				mTitleTextureView.setText(R.string.user_check);
			}else if(type.equals("RECORDED_ANOTHER_CARD")){
				roomNumberString = getIntent().getStringExtra("roomNumber");
				mTitleTextureView.setText(R.string.record_another_card);
			}else if(type.equals("ALL_CARDS_RECORDED")){
				mTitleTextureView.setText(R.string.all_card_recorded);
			}else if(type.equals("DELETE_PHONE_NAME")){
				phoneNameString = getIntent().getStringExtra("phoneName");
				mTitleTextureView.setText(R.string.user_check);
			}else if(type.equals("DELETE_PHONE_BOOK")){
				mTitleTextureView.setText(R.string.user_check);
			}else if(type.equals("DELETE_CARD")){
				mTitleTextureView.setText(R.string.all_cards_will_delete);
				roomNumberString = getIntent().getStringExtra("roomNumber");
			}else if(type.equals("DELETE_ALL_PASSWORD")){
				mTitleTextureView.setText(R.string.delete_all_password);

			}
		}
		
		mI2cInterface = new LinphoneI2CInterface();
		
	}

	@Override  
	public boolean dispatchKeyEvent(KeyEvent event) {  
	   // TODO Auto-generated method stub  
	   // 判断普通按键  
	   int keyCode = event.getKeyCode();
	   if(event.getAction() == KeyEvent.ACTION_DOWN){
		   
		   Log.d("Key Event", "Key code = "+keyCode);
		if(keyCode == 19){                       //left up
			   
			   return true;
			   
		   }else if(keyCode == 20){            //left down     cancel
			   if(type.equals("ARE_YOU_SURE")){
					
				}else if(type.equals("ALL_CARDS_RECORDED") || type.equals("DELETE_CARD")){
					
					Intent mPhoneBookEditIntent = new Intent(UserCheckActivity.this,RoomSetupActivity.class);
					startActivity(mPhoneBookEditIntent);
					
				}else if(type.equals("DELETE_PHONE_NAME") || type.equals("DELETE_PHONE_BOOK")){
					Intent mPhoneBookEditIntent = new Intent(UserCheckActivity.this,PhoneBookSetupActivity.class);
					startActivity(mPhoneBookEditIntent);
				}else if(type.equals("DELETE_ALL_PASSWORD")){
					finish();
				}else if(type.equals("RECORDED_ANOTHER_CARD")){
					mI2cInterface.SetProximity(0x02, 0x00, 0x00, 0x00, 0x00);
					Intent mPhoneBookEditIntent = new Intent(UserCheckActivity.this,RoomSetupActivity.class);
					startActivity(mPhoneBookEditIntent);
				}
			   return true;
			   
		   }else if(keyCode == 21){          //right up
			  
			   return true;
			   
		   }else if(keyCode == 22){        //right down   ok
			   if(type != null){
					if(type.equals("ARE_YOU_SURE")){
						
					}else if(type.equals("RECORDED_ANOTHER_CARD")){
						switch (pressOkTimes) {
						case 0:
//							mTitleTextureView.setText(R.string.record_another_card_room);
							Intent mUserShowMessageIntent = new Intent(UserCheckActivity.this,UserShowMessageActivity.class);
							mUserShowMessageIntent.putExtra("type", "SHOW_CARD");
							mUserShowMessageIntent.putExtra("roomNumber",roomNumberString);
							startActivity(mUserShowMessageIntent);
							Log.d("ADD CARD", "add another card.room number = "+roomNumberString);
							break;
						case 1:
//							Intent mUserShowMessageIntent = new Intent(UserCheckActivity.this,UserShowMessageActivity.class);
//							mUserShowMessageIntent.putExtra("type", "SHOW_CARD");
//							mUserShowMessageIntent.putExtra("roomNumber",roomNumberString);
//							
//							startActivity(mUserShowMessageIntent);
							break;
						default:
							break;
						}
						
					}else if(type.equals("ALL_CARDS_RECORDED")){
						finish();
					}else if(type.equals("DELETE_PHONE_NAME")){
						phoneNameString = getIntent().getStringExtra("phoneName");
						deletePhoneNameFromSdcard(phoneNameString);
						Intent mPhoneBookEditIntent = new Intent(UserCheckActivity.this,PhoneBookSetupActivity.class);
						startActivity(mPhoneBookEditIntent);
					}else if(type.equals("DELETE_PHONE_BOOK")){
						deletePhoneBook();
						Intent mPhoneBookEditIntent = new Intent(UserCheckActivity.this,PhoneBookSetupActivity.class);
						startActivity(mPhoneBookEditIntent);
					}else if(type.equals("DELETE_CARD")){
						Intent mUserShowMessageIntent = new Intent(UserCheckActivity.this,UserShowMessageActivity.class);
						mUserShowMessageIntent.putExtra("roomNumber", roomNumberString);
						mUserShowMessageIntent.putExtra("type", "ALL_CARD_DELETED");
						startActivity(mUserShowMessageIntent);
					}else if(type.equals("DELETE_ALL_PASSWORD")){
						int resultClearAllPassword = mI2cInterface.SetClearPassword();
						Log.d("SetClearAllPassword", "result = "+resultClearAllPassword);
						finish();
					}
				}
			   pressOkTimes++;
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
	
	private void deletePhoneBook(){
		File mDataFile = new File("/mnt/sdcard/DIP40_phonebook_en.txt");
		if(mDataFile.exists()){
			mDataFile.delete();
		}
		mDataFile = new File("/mnt/sdcard/DIP40_phonebook_tr.txt");
		if(mDataFile.exists()){
			mDataFile.delete();
		}
	}
	
	
}
