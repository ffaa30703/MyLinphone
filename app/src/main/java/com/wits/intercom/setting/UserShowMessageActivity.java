package com.wits.intercom.setting;


import com.keep.lin.R;
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
import android.view.Menu;
import android.widget.TextView;

public class UserShowMessageActivity extends FragmentActivity {
	private String type;
	private TextView mMessageTextView;
	
	private final int WAIT_PROXIMITY_RESPONSE = 12;
	private final int GET_RESPONSE_BY_THREAD = 13;
	private final int NO_CARD = 14;
	private final int CARD_RECORDED = 15;
	private final int BACK_TO_PRIVIOUS_PAGE = 16;
	private final int DETECT_RESPONSE  = 17;
	
	private boolean WAITING_FOR_ZONE_RESPONSE = false;
	private boolean responseDetecting = false;
	private boolean isAddCard = true;
	
	protected LinphoneI2CInterface mI2cInterface;
	
	private SharedPreferences mPreferences;
	private Editor mEditor;
	
	protected boolean isMoreThenFiveCard = false;
	private  String roomNumberString;
	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case WAIT_PROXIMITY_RESPONSE:
				mI2cInterface.SetProximity(0x02, 0x00, 0x00, 0x00, 0x00);
				mHandler.removeMessages(DETECT_RESPONSE);
				mHandler.sendEmptyMessageDelayed(NO_CARD, 3000);
				mMessageTextView.setText(getResources().getString(R.string.no_card));
				break;
			case GET_RESPONSE_BY_THREAD:
				if(isMoreThenFiveCard){
					mI2cInterface.SetProximity(0x02, 0x00, 0x00, 0x00, 0x00);
					mHandler.removeMessages(WAIT_PROXIMITY_RESPONSE);
					mHandler.sendEmptyMessageDelayed(BACK_TO_PRIVIOUS_PAGE, 3000);
					mMessageTextView.setText(getResources().getString(R.string.all_card_recorded));
				}else{
					mHandler.removeMessages(WAIT_PROXIMITY_RESPONSE);
					mHandler.sendEmptyMessageDelayed(CARD_RECORDED, 3000);
					mMessageTextView.setText(getResources().getString(R.string.card_recorded));
				}
				break;
			case CARD_RECORDED:
//				int cardNum = mPreferences.getInt("card_numbers", 0);
//				cardNum++;
//				mEditor.putInt("card_numbers", cardNum);
//				mEditor.commit();
				Intent mUserCheckIntent = new Intent(UserShowMessageActivity.this,UserCheckActivity.class);
				mUserCheckIntent.putExtra("type", "RECORDED_ANOTHER_CARD");
				mUserCheckIntent.putExtra("roomNumber", roomNumberString);
				startActivity(mUserCheckIntent);
				break;
			case NO_CARD:
				
				Intent mRoomSetupIntent = new Intent(UserShowMessageActivity.this, RoomSetupActivity.class);
				startActivity(mRoomSetupIntent);
				break;
			case BACK_TO_PRIVIOUS_PAGE:
				if(type.equals("ALL_CARD_DELETED") || type.equals("SHOW_CARD")){
					Intent mBackRoomSetupIntent = new Intent(UserShowMessageActivity.this, RoomSetupActivity.class);
					startActivity(mBackRoomSetupIntent);
				}
				break;
			case DETECT_RESPONSE:
				int[] type = new int[1];
				int resultResponse = mI2cInterface.GetProximityStatue(type);
				Log.d("Proximity", "GetProximityStatue.result = "+resultResponse);
				if(resultResponse == 0){
					responseDetecting = false;
					Log.d("ADD CARD", "received response.type = "+type[0]);
					if(type[0] == 0x01){
						isMoreThenFiveCard = false;
						mHandler.removeMessages(WAIT_PROXIMITY_RESPONSE);
						mHandler.sendEmptyMessage(GET_RESPONSE_BY_THREAD);
					}else if(type[0] == 0x02){
						isMoreThenFiveCard = true;
						mHandler.removeMessages(WAIT_PROXIMITY_RESPONSE);
						mHandler.sendEmptyMessage(GET_RESPONSE_BY_THREAD);
						
					}
				}else{
					mHandler.sendEmptyMessageDelayed(DETECT_RESPONSE, 1000);
				}
				break;
			default:
				break;
			}
		}
	};
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_user_show_message);
		Log.d("ADD CARD", "User show creat");
		mMessageTextView = (TextView)findViewById(R.id.user_input_message);
		
		mI2cInterface = new LinphoneI2CInterface();
		
		type = getIntent().getStringExtra("type");
		Log.d("UserInput", "type = "+type);
		if(type != null){
			if(type.equals("SHOW_CARD")){
				roomNumberString = getIntent().getStringExtra("roomNumber");
				
				int roomNumber = Integer.parseInt(roomNumberString);
				
				int digit3 = roomNumber/100;
				int digit2 = (roomNumber%100)/10;
				int digit1 = roomNumber%10;
				Log.d("Proximity", "add card.room number = "+digit1+digit2+digit3);
				int mAddCardRestult = mI2cInterface.SetProximity(0x04, 0x01, digit1,digit2,digit3);    // for test room 005,add card
				Log.d("Proximity", "add card result = "+mAddCardRestult);
				mMessageTextView.setText(getResources().getString(R.string.show_card));
				mHandler.sendEmptyMessageDelayed(WAIT_PROXIMITY_RESPONSE, 20000);
				responseDetecting = true;
//				detectResponseThread();
				mHandler.sendEmptyMessage(DETECT_RESPONSE);
				
			}else if(type.equals("ALL_CARD_DELETED")){
				String roomNumberString = getIntent().getStringExtra("roomNumber");
				
				int roomNumber = Integer.parseInt(roomNumberString);
				
				int digit3 = roomNumber/100;
				int digit2 = (roomNumber%100)/10;
				int digit1 = roomNumber%10;
				int mDeleteCardRestult = mI2cInterface.SetProximity(0x04, 0x00, digit1,digit2,digit3);    // for test room 005,add card
				Log.d("Proximity", "delete card result = "+mDeleteCardRestult);
				mMessageTextView.setText(getResources().getString(R.string.all_cards_deleted));
				mHandler.sendEmptyMessageDelayed(BACK_TO_PRIVIOUS_PAGE, 3000);
			}
		}
		
		mPreferences=getSharedPreferences("deviceInfo", 0);
		mEditor = mPreferences.edit();
		
	}
	
	private void detectResponseThread() {
		new Thread() {
			public void run() {
				while(responseDetecting){
				try {
					sleep(1000);
					int[] type = new int[1];
					int resultResponse = mI2cInterface.GetProximityStatue(type);
					Log.d("Proximity", "GetProximityStatue.result = "+resultResponse);
					if(resultResponse == 0){
						responseDetecting = false;
						Log.d("ADD CARD", "received response.type = "+type[0]);
						if(type[0] == 0x01){
							isMoreThenFiveCard = false;
							mHandler.removeMessages(WAIT_PROXIMITY_RESPONSE);
							mHandler.sendEmptyMessage(GET_RESPONSE_BY_THREAD);
						}else if(type[0] == 0x02){
							isMoreThenFiveCard = true;
							mHandler.removeMessages(WAIT_PROXIMITY_RESPONSE);
							mHandler.sendEmptyMessage(GET_RESPONSE_BY_THREAD);
							
						}
					}
					
					
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				}
			};
		}.start();
	}
	
	@Override
	protected void onStart(){
		super.onStart();
		Log.d("ADD CARD", "User show start");
	}
	
	@Override
	protected void onResume() {
		super.onResume();

		Log.d("ADD CARD", "User show resume");
	}
	
	@Override
	protected void onPause(){
		super.onPause();
		responseDetecting = false;
		mHandler.removeMessages(WAIT_PROXIMITY_RESPONSE);
		mHandler.removeMessages(GET_RESPONSE_BY_THREAD);
		mHandler.removeMessages(DETECT_RESPONSE);
	}
	
}
