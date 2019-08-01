package com.wits.intercom;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;

import org.linphone.LinphoneManager;
import org.linphone.ui.witsui.Constant;
import org.linphone.ui.witsui.ScreenOffActivity;

import com.keep.lin.R;
import com.wits.intercom.DeladyProcess.IdelayMethodrd;
import com.wits.intercom.dialing.EditToCallActivity;
import com.wits.intercom.setting.SettingActivity;
import com.wits.intercom.util.Logger;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.text.format.Time;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

public class MainFunctionsActivity extends FragmentActivity implements OnClickListener,IdelayMethodrd{
	private final String TAG = MainFunctionsActivity.class.getSimpleName();
	private Timer mTime;
	private int nine_down;
	private String devIpString;
	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(R.layout.activity_main_function);
		findViewAndSetListen();
		
		try {
			devIpString = getLocalIPAddress();
			Log.d("System", "dev ip = "+devIpString);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	protected void onStart() {	
		super.onStart();
	
    	DeladyProcess.stopDelayThread();
		DeladyProcess.doDelay();
		DeladyProcess.getInstance(new Handler(), 30000, this);				
	}
	@Override
	protected void onResume() {
		super.onResume();
	}
	private void findViewAndSetListen() {
		Log.i("zg", "============findViewAndSetListen=====================");		
			
		ImageView call_door_keep_button=(ImageView)findViewById(R.id.function_callDoorKeeper_button);
		call_door_keep_button.setOnClickListener(this);
		
		ImageView call_guard_button=(ImageView)findViewById(R.id.function_callGuard_button);
		call_guard_button.setOnClickListener(this);
		
		ImageView phone_book_imgButtonImageView=(ImageView)findViewById(R.id.function_phoneBook_button);
		phone_book_imgButtonImageView.setOnClickListener(this);
		
		ImageView open_door_with_password=(ImageView)findViewById(R.id.function_openDoorPassword_button);
		open_door_with_password.setOnClickListener(this);
	}
	
	@Override
	public void onClick(View v) {
		DeladyProcess.stopDelayThread();
		
		switch(v.getId()){
		case R.id.function_phoneBook_button:
			Log.i("zg", "===========phone_book_button==========");
			Intent phoneBookIntent=new Intent(MainFunctionsActivity.this,PhoneBookActivity.class);
			startActivity(phoneBookIntent);
			break;
		case R.id.function_openDoorPassword_button:
			Intent openDoorPasswordIntent=new Intent(MainFunctionsActivity.this,PasswordActivity.class);
			startActivity(openDoorPasswordIntent);
			break;
		case R.id.function_callDoorKeeper_button:
			Intent doorKeeperIntent=new Intent(MainFunctionsActivity.this,ConversationStatusActivity.class);
			doorKeeperIntent.putExtra(Constant.CALLER_STYLE, CallerStyle.DOORKEEPER);
			startActivity(doorKeeperIntent);
			break;
		case R.id.function_callGuard_button:
			Intent guardIntent=new Intent(MainFunctionsActivity.this,ConversationStatusActivity.class);
			guardIntent.putExtra(Constant.CALLER_STYLE, CallerStyle.GUARD);
			startActivity(guardIntent);
			break;
		 }		
	}

	@Override
	protected void onStop() {
		super.onStop();
//		DeladyProcess.stopDelayThread();
	}
	
	@Override
	protected void onDestroy() {
//		DeladyProcess.stopDelayThread();
		super.onDestroy();
	}
	
	@Override
	public void delayProcess() {
		startActivity(new Intent(MainFunctionsActivity.this,LinphoneWelcomeActivity.class));
//		MyApplication.getInstnce().back(1);
	}
	
	@Override  
	public boolean dispatchKeyEvent(KeyEvent event) {  
	   // TODO Auto-generated method stub  
	   // 判断普通按键  
	   int keyCode = event.getKeyCode();
	   if(event.getAction() == KeyEvent.ACTION_DOWN){
		   
		   Log.d("Key Event", "Key code = "+keyCode);
		if(keyCode == 19){
			  DeladyProcess.stopDelayThread();
			  nine_down = 0;
			   LinphoneManager.isShowingInputActivity = true;
			   String callSip = getCallSip("841");
			   Log.d("Conversation", "call sip =  "+callSip);
			   initCallInfor(1,callSip);
			   Intent mCallIntent = new Intent(this,CallPageActivity.class);
				
				mCallIntent.putExtra("roomNumber", "841");
				startActivity(mCallIntent);
			   return true;
			   
		   }else if(keyCode == 20){
			   DeladyProcess.stopDelayThread();
			   nine_down = 0;
			   LinphoneManager.isShowingInputActivity = true;
			   LinphoneManager.isShowingInputActivity = true;
			   Intent mPhoneBookIntent = new Intent(this, PhoneBookActivity.class);
			   startActivity(mPhoneBookIntent);
			   return true;
			   
		   }else if(keyCode == 21){
			   DeladyProcess.stopDelayThread();
			   nine_down = 0;
			   LinphoneManager.isShowingInputActivity = true;
			   initCallInfor(1,"10.99.26.1");
			   Intent mCallIntent = new Intent(this,CallPageActivity.class);
				
				mCallIntent.putExtra("roomNumber", "901");
				startActivity(mCallIntent);
			   return true;
			   
		   }else if(keyCode == 22){
			   DeladyProcess.stopDelayThread();
			   nine_down = 0;
			   LinphoneManager.isShowingInputActivity = true;
			   Intent mOpenDoorIntent = new Intent(this, PasswordActivity.class);
			   mOpenDoorIntent.putExtra("type", "door");
			   startActivity(mOpenDoorIntent);
			   return true;
			   
		   }else if(keyCode == 66 ){
//			   DeladyProcess.stopDelayThread();
//			   nine_down = 0;
//			   Intent intent = new Intent(this,
//						EditToCallActivity.class);
//				startActivity(intent);
				
			   return true;
			   
		   }else if(keyCode >= 7 && keyCode <= 16){
			   DeladyProcess.stopDelayThread();
			   nine_down = 0;
			   Intent intent = new Intent(this,
						EditToCallActivity.class);
			    intent.putExtra("keyCode", keyCode);
				startActivity(intent);
			   
			   return true;
		   }else if(keyCode == 67){
			   nine_down = 0;
//			   DeladyProcess.stopDelayThread();
//			   Intent intent = new Intent(this,
//						SettingActivity.class);
//				startActivity(intent);
				
			   return true;
			   
		   }
	   }
	   return super.dispatchKeyEvent(event);
	   
	} 
	
	private CallInfor initCallInfor(int role,String sipString){
		CallInfor callInfor=CallInfor.getInstance();
//		callInfor.sipip=sipString;
//		callInfor.role=role;
		CallInfor.setSipip(sipString);
		CallInfor.setRole(role);
		
		return callInfor; 
	}
	
	private String getCallSip(String inputRoomNumber) {
		// TODO Auto-generated method stub
		String ipString = "10";
		String thirdPartString = "1";
		String forthPartString = "1";
		if (inputRoomNumber == null && devIpString == null) {
			return null;
		} else {
			Log.d("parse call sip", "input room number= "+inputRoomNumber);
			
			int room = Integer.parseInt(inputRoomNumber);
			if (room >= 1 && room <= 254) {
				thirdPartString = "1";
				forthPartString = room + "";
			} else if (room >= 255 && room <= 508) {
				thirdPartString = "2";
				forthPartString = (room - 254) + "";
			} else if (room >= 509 && room <= 762) {
				thirdPartString = "3";
				forthPartString = (room - 254 * 2) + "";
			} else if (room >= 763 && room <= 799) {
				thirdPartString = "4";
				forthPartString = (room - 254 * 3) + "";
			}else if (room ==841 ) {
				thirdPartString = "23";
				forthPartString = "1";
			}
			
			String[] arr = devIpString.split("\\.");      //changed by jimmy at 2014.5.4
			if (arr != null) {
				for (int i = 1; i < arr.length; i++) {
					if (i == 1) {
						ipString +="."+arr[i];
					} else if((i == 2)){
						ipString += "."+thirdPartString;
					}else if((i == 3)){
						ipString += "."+forthPartString;
					}
				}
			}
			
			Log.d("get Indoor Ip", "this devive indoor ip is "+ipString);
			
			return ipString;
		}
	}
	
	private String getLocalIPAddress() throws SocketException{  
	    for(Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();en.hasMoreElements();){  
	        NetworkInterface intf = en.nextElement();  
	        for(Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();){  
	            InetAddress inetAddress = enumIpAddr.nextElement();  
	            if(!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address){  
	                return inetAddress.getHostAddress().toString();  
	            }  
	        }  
	    }  
	    return "null";  
	}
	
	
}
