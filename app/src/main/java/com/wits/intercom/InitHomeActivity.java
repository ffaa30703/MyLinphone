package com.wits.intercom;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import org.linphone.LinphoneManager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.keep.lin.R;
import com.wits.intercom.DeladyProcess.IdelayMethodrd;
import com.wits.intercom.dialing.EditToCallActivity;
import com.wits.intercom.setting.SettingActivity;

/*实现了界面,功能未实现，接口调用有提出
 * 1.实现,无操作,3s后正常跳转
 * 2.物理按键Button未响应ImageView 事件处理
 * */
public class InitHomeActivity extends Activity implements OnClickListener,IdelayMethodrd{
	private  final String  TAG = InitHomeActivity.class.getSimpleName();
	
	private static final int talk_connection_busy=1;
	private static final int talk_connection_error=2;
	private static final int talk_connection_right=3;
	private int talk_connection_starts; //根据通话链接成功的信息，返回状态
	private int nine_down = 0;

	private String devIpString;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wits_activity_inithome);
		findViewAndSetListen();
		try {
			devIpString = getLocalIPAddress();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();
//		DeladyProcess.stopDelayThread();
		DeladyProcess.doDelay();
		DeladyProcess.getInstance(new Handler(), 2000, this);				
	}

	private void findViewAndSetListen() {
		Log.i(TAG, "============findViewAndSetListen=====================");		
		
		ImageView call_door_keep_button=(ImageView)findViewById(R.id.call_door_keep_button);
		call_door_keep_button.setOnClickListener(this);
		
		ImageView call_guard_button=(ImageView)findViewById(R.id.call_guard_button);
		call_guard_button.setOnClickListener(this);
		
		ImageView phone_book_imgButtonImageView=(ImageView)findViewById(R.id.phone_book_button);
		phone_book_imgButtonImageView.setOnClickListener(this);
		
		ImageView open_door_with_password=(ImageView)findViewById(R.id.open_door_withpassword_button);
		open_door_with_password.setOnClickListener(this);		
	    }
	
	//通过物理按键来选择触发
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.phone_book_button:
			Log.i(TAG, "===========phone_book_button==========");
			Intent jumpIntent=new Intent(InitHomeActivity.this,PhoneBookActivity.class);
			startActivity(jumpIntent);
			break;
		case R.id.open_door_withpassword_button:
			Log.i(TAG, "===========open_door_withpassword_button==========");
			Intent jumpIntent1=new Intent(InitHomeActivity.this,PasswordActivity.class);
			startActivity(jumpIntent1);
			break;
		case R.id.call_door_keep_button:
			//获取门卫的sip_id
			//调用通话接口
			//获取通话状态
			switch (talk_connection_starts) {
			 			case talk_connection_busy:
			 				//跳往用户正忙界面，延时一定时间后继续拨号，如是3次挂断或者用户主动挂断
			 				break;
			 			case talk_connection_error:
			 				//链接出现异常 给出异常提示
			 				break;
			 			case talk_connection_right:
			 				//调用通话接口
				
			 				break;
			}
			break;
			
		case R.id.call_guard_button:
			//获取安保的sip_id
			//调用通话接口
			//获取通话状态
			switch (talk_connection_starts) {
					case talk_connection_busy:
						//跳往用户正忙界面，延时一定时间后继续拨号，如是3次挂断或者用户主动挂断
						break;
					case talk_connection_error:
						//链接出现异常 给出异常提示
						break;
					case talk_connection_right:
						//调用通话接口
				break;
			}
			break;
		default:
			break;
		}
	}
	
	
	@Override
	protected void onStop(){
		super.onStop();
//		DeladyProcess.stopDelayThread();
	}

	@Override
	public void delayProcess() {
		startActivity(new Intent(this,MainFunctionsActivity.class));	
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
			  nine_down   = 0;
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
