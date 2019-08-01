package com.wits.intercom;


import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import org.linphone.LinphoneManager;
import org.linphone.ui.witsui.Constant;

import com.keep.lin.R;
import com.wits.intercom.DeladyProcess.IdelayMethodrd;
import com.wits.intercom.dialing.EditToCallActivity;
import com.wits.intercom.setting.SettingActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CheckPasswordStartActivity extends Activity implements OnClickListener,IdelayMethodrd{
	private final String TAG = CheckPasswordStartActivity.class.getSimpleName();
	
	private int keyStart=1;
	private TextView passwordStar;
	private ImageView lockImageView,startImageView;
	private final static int PASSWORD_RIGHT=1;
	private final static int PASSWORD_WRONG=2;
	private boolean passwordIsRight=false;
	private int comeFromActivityTag;

	private int nine_down = 0;

	private String devIpString;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wits_activity_password_checked);
		try {
			devIpString = getLocalIPAddress();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		findView();	
	}
	
	private void getKeyValueAndShowStart() {
		//进行输入密码验证
		Bundle bundle=getIntent().getExtras();
		passwordIsRight=bundle.getBoolean(Constant.PASSWORD_CHECKED_RESULT);
		comeFromActivityTag=bundle.getInt(Constant.ENTRY_TAG);
		
		if(passwordIsRight){
			passwordStar.setText(getResources().getString(R.string.password_is_right));
			lockImageView.setImageResource(R.drawable.wits_lock_open);
			startImageView.setImageResource(R.drawable.wits_password_right);
		}else{
			passwordStar.setText(getResources().getString(R.string.password_is_wrong));
			lockImageView.setImageResource(R.drawable.wits_lock_lock);
			startImageView.setImageResource(R.drawable.wits_password_wrong);
		}
		
	}
	private void findView() {
		passwordStar=(TextView)findViewById(R.id.checked_password_result);
		
		lockImageView=(ImageView)findViewById(R.id.lock_start);
		
		startImageView=(ImageView)findViewById(R.id.lock_start2);
		LinearLayout linearLayout=(LinearLayout)findViewById(R.id.password_checked_layout);
		linearLayout.setOnClickListener(this);	
	}
	@Override
	protected void onStart() {
		super.onStart();
		getKeyValueAndShowStart();
		processByFromTag(comeFromActivityTag);
		
	}
	private void processByFromTag(int comeFromActivityTag2) {
		switch (comeFromActivityTag2) {
		case Constant.INIT_HOME_ENTRY_TAG:
			DeladyProcess.doDelay();
			DeladyProcess.getInstance(new Handler(), 3000, this);
			break;

		default:
			break;
		}
	}
	@Override
	public void finish() {
		DeladyProcess.stopDelayThread();
		super.finish();
	}
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.password_checked_layout:
			Intent intent=new Intent(CheckPasswordStartActivity.this,EditToCallActivity.class);
			startActivity(intent);		
			break;
		default:
			break;
		}	
	}
	@Override
	public void delayProcess() {
		Intent intent;
		if(passwordIsRight){
			//TODO 调用I2C的方法打开门		
			Log.e(TAG,"==========用户输入开门密码正确,通过I2C硬件开门============");
			//回到主页面
			 intent=new Intent(CheckPasswordStartActivity.this,LinphoneWelcomeActivity.class);
		}else{
//			 回到MainFunctionsActivity 页面
			Log.e(TAG,"==========用户密码输入ERROR!==回到FunctionsActivity页======");
//		    intent=new Intent(CheckPasswordStartActivity.this,PasswordActivity.class);
			intent=new Intent(CheckPasswordStartActivity.this,MainFunctionsActivity.class);
			intent.putExtra(Constant.ENTRY_TAG, Constant.INIT_HOME_ENTRY_TAG);
		}
		startActivity(intent);
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
			  nine_down  = 0;
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
			   
		   }else if(keyCode == 16){
			   DeladyProcess.stopDelayThread();
			   nine_down++;
			   if(nine_down == 2){
				   nine_down = 0;
				   
			   }
			   return true;
			   
		   }else if(keyCode == 66 ){
			   DeladyProcess.stopDelayThread();
			   nine_down = 0;
			   Intent intent = new Intent(this,
						EditToCallActivity.class);
				startActivity(intent);
				
			   return true;
			   
		   }else if(keyCode >= 7 && keyCode <= 15){
			   DeladyProcess.stopDelayThread();
			   nine_down = 0;
			   Intent intent = new Intent(this,
						EditToCallActivity.class);
			    intent.putExtra("keyCode", keyCode); 
				startActivity(intent);
			   
			   return true;
		   }else if(keyCode == 67){
			   nine_down = 0;
			   DeladyProcess.stopDelayThread();
			   Intent intent = new Intent(this,
						LinphoneWelcomeActivity.class);
				startActivity(intent);
				
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
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
}
