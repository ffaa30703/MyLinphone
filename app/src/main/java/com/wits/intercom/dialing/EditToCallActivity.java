package com.wits.intercom.dialing;


import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import org.linphone.LinphoneManager;
import org.linphone.LinphoneService;
import org.linphone.LinphoneSimpleListener.LinphoneOnCallStateChangedListener;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCall.State;
import org.linphone.ui.witsui.Constant;

import com.keep.lin.R;
import com.wits.intercom.CallerStyle;
import com.wits.intercom.Constance;
import com.wits.intercom.ConversationStatusActivity;
import com.wits.intercom.MainFunctionsActivity;
import com.wits.intercom.PasswordActivity;
import com.wits.intercom.PhoneBookActivity;
import com.wits.intercom.DeladyProcess.IdelayMethodrd;
import com.wits.intercom.MyApplication;
import com.wits.intercom.util.AnalysisIPAddrFromNum;
import com.wits.intercom.util.Logger;
import com.wits.intercom.CallInfor;
import com.wits.intercom.CallPageActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.text.InputFilter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
/**
 * 输入编辑框
 * 然后呼叫
 * 触摸 物理数字按键 弹出此界面
 * 对输入的 数字进行 IP转码
 * 按Dial键进行拔号
 * @author Administrator
 *
 */
public class EditToCallActivity extends FragmentActivity {
	private final String TAG = EditToCallActivity.class.getSimpleName();
	private EditText etDailNum;
	private ImageView imgDialBtn;
	private String dialSIP;
	private String validNum;
	private LinphoneCall mCall;
	private CallerStyle callStyle; 
	private String devIpString;
	
	private String firstInputNumberString;
	
	private final int CLEAR_INPUT_MESSAGE = 10;
	private final int BACK_TO_S1P1 = 11;
	private boolean isCongestionRinging = false;
	private Handler mHandle = new Handler() {
		public void dispatchMessage(Message msg) {
			Log.w(Constance.LOGTAG, "get message");

			if(msg.what == CLEAR_INPUT_MESSAGE){
				LinphoneManager.stopRingForCongestion();
//				etDailNum.setText("");
//				etDailNum.setTextSize(60);
				finish();
			}else if(msg.what == BACK_TO_S1P1){
				LinphoneManager.stopRingForCongestion();
				finish();
			}
			
		};
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wits_dailing_edit_to_call);
		
		int keyCode = getIntent().getIntExtra("keyCode", 0);
		switch (keyCode) {
		case 7:
			firstInputNumberString = "0";
			break;
		case 8:
			firstInputNumberString = "1";
			break;
		case 9:
			firstInputNumberString = "2";
			break;
		case 10:
			firstInputNumberString = "3";
			break;
		case 11:
			firstInputNumberString = "4";
			break;
		case 12:
			firstInputNumberString = "5";
			break;
		case 13:
			firstInputNumberString = "6";
			break;
		case 14:
			firstInputNumberString = "7";
			break;
		case 15:
			firstInputNumberString = "8";
			break;
		case 16:
			firstInputNumberString = "9";
			break;
		default:
			firstInputNumberString = null;
			break;
		}
		Log.d(TAG,firstInputNumberString);
		setupView();
		try {
			devIpString = getLocalIPAddress();
			Log.d("System", "dev ip = "+devIpString);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		   
	}
	
	private void setupView(){
		etDailNum = (EditText) findViewById(R.id.et_DailNum);			
		imgDialBtn = (ImageView) findViewById(R.id.img_dailbtn);
		if(firstInputNumberString!=null){
			etDailNum.setText(firstInputNumberString);
//			etDailNum.setSelection(1);
			etDailNum.setSelection(1);
		}
		
	}
	
	public void doClick(View v){
		switch (v.getId()) {
		case R.id.img_dailbtn:
			//去进行拨号
			dialSIP = etDailNum.getText().toString().trim();
			Log.d(TAG, dialSIP);
			if (null == dialSIP || "".equals(dialSIP) || dialSIP.contains("#")|| dialSIP.contains("*"))
			{	//指向无效输入界面
				startActivity(new Intent(EditToCallActivity.this,InvalidRoomShowActivity.class));
			}
			else
			{
//				    validNum = AnalysisIPAddrFromNum.getProtoIP(dialSIP);//TODO YF
				    validNum = "192.168.1.10";
				    Logger.e(TAG, "ValidNum:"+validNum);
			    if(null == validNum ||"".equals(validNum))
			    { //輸入房間號,超出解析范圍
			      startActivity(new Intent(EditToCallActivity.this,InvalidRoomShowActivity.class));
			    }
			    else
			    {//转换IP后进行拔号,正常拔号
				    LinphoneManager.getInstance().newOutgoingCallString("sip:" + validNum, false);  //采用子线程
					startActivity(new Intent().setClass(EditToCallActivity.this, ConversationWindowShowActivity.class).putExtra("room_no", dialSIP));
//					startActivity(new Intent(EditToCallActivity.this,ConversationWindowShowActivity.class));
			    }
			}				
		   break;
		default:
			break;
		}
			//清空输入框
			etDailNum.setText(null);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		callStyle=(CallerStyle) getIntent().getSerializableExtra(Constant.CALLER_STYLE);
		if(callStyle!=null){
		switch (callStyle) {
		case DOORKEEPER: //门卫  01801
			dialSIP= "01009";
			break;
		case GUARD://保安   99901
			dialSIP="01009";
			break;
		case NEIGHBOR://邻居
			dialSIP="01009";
			break;
		default:
			break;
		    }
		}
		//callStyle==NULL 处理	
//		LinphoneManager.getInstance().newOutgoingCallString( "sip:" +AnalysisIPAddrFromNum.getProtoIP(dialSIP), false);		
//		startActivity(new Intent().setClass(EditToCallActivity.this, ConversationWindowShowActivity.class).putExtra("room_no", dialSIP));		

		
//		TutorialLaunchingThread thread = new TutorialLaunchingThread();
//		thread.start();		
	}
	
	@Override
	protected void onStop() {
		super.onStop();
//		LinphoneService.isShowEditActivity = false;
//		LinphoneManager.removeListener(this);
	}
	
	@Override
	protected void onDestroy(){
		super.onDestroy();
		LinphoneManager.isShowingInputActivity = false;
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
			}else if (room >= 841 && room <= 849) {
				thirdPartString = "23";
				forthPartString = (room - 840) + "";
			} else if (room >= 851 && room <= 859) {
				thirdPartString = "24";
				forthPartString = (room - 850) + "";
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
			
			if (room >= 901 && room <= 919) {
				thirdPartString = "26";
				forthPartString = (room - 900) + "";
				ipString = "10.99";
				if (arr != null) {
					for (int i = 1; i < arr.length; i++) {
						if((i == 2)){
							ipString += "."+thirdPartString;
						}else if((i == 3)){
							ipString += "."+forthPartString;
						}
					}
				}

			} else if (room >= 921 && room <= 959) {
				thirdPartString = "27";
				forthPartString = (room - 920) + "";
				ipString = "10.99";
				if (arr != null) {
					for (int i = 1; i < arr.length; i++) {
						if((i == 2)){
							ipString += "."+thirdPartString;
						}else if((i == 3)){
							ipString += "."+forthPartString;
						}
					}
				}

			}
						
			Log.d("get Ip", "call ip is "+ipString);
			
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
	
	private class TutorialLaunchingThread extends Thread {
		@Override
		public void run() {
			super.run();
			LinphoneManager.getInstance().newOutgoingCallString( "sip:" +AnalysisIPAddrFromNum.getProtoIP(dialSIP), false);		
		}
	}
	
	@Override  
	public boolean dispatchKeyEvent(KeyEvent event) {  
	   // TODO Auto-generated method stub  
	   // 判断普通按键  
	   int keyCode = event.getKeyCode();
	   if(event.getAction() == KeyEvent.ACTION_DOWN){
		   mHandle.removeMessages(CLEAR_INPUT_MESSAGE);
		   mHandle.sendEmptyMessageDelayed(CLEAR_INPUT_MESSAGE, 10000);
		   Log.d("Key Event", "Key code = "+keyCode);
		   if(keyCode == 66){
//			   Intent mCallIntent = new Intent();
//			   mCallIntent.putExtra("roomNumber", etDailNum.getText().toString());
//			   mCallIntent.putExtra("sipip", "10.1.23.1");
//			   mCallIntent.setClass(this, ConversationStatusActivity.class);
//			   startActivity(mCallIntent);
			   String inputRoomNumberString = etDailNum.getText().toString();
			   if(isCongestionRinging || inputRoomNumberString.equals("")){
				   return true;
			   }
			   if(inputRoomNumberString == ""){
				   etDailNum.setText(R.string.invalid_room_number);
				   mHandle.sendEmptyMessageDelayed(BACK_TO_S1P1, 3000);
				   return true;
			   }
			   
			   int room = Integer.parseInt(inputRoomNumberString);
			   
			   if(room>799 || room == 0 ){
				   if((room>=841 && room <= 849) || (room>=851 && room<= 859) 
						   || (room>=901 && room<=919) || (room>=921 && room <=959)){
					   String callSip = getCallSip(inputRoomNumberString);
					   Log.d("Conversation", "call sip =  "+callSip);
					   initCallInfor(1,callSip);
					   Intent mCallIntent = new Intent(this,CallPageActivity.class);
						
						mCallIntent.putExtra("roomNumber", inputRoomNumberString);
						startActivity(mCallIntent);
					   
					   return true;
				   }else{
					   etDailNum.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
					   etDailNum.setTextSize(35);
					   etDailNum.setText(R.string.invalid_room_number);
					   
					   LinphoneManager.startRingForCongestion();
					   mHandle.sendEmptyMessageDelayed(BACK_TO_S1P1, 3000);
					   isCongestionRinging = true;
					   return true;
				   }
				   
			   }
			   
			   String callSip = getCallSip(inputRoomNumberString);
			   Log.d("Conversation", "call sip =  "+callSip);
			   initCallInfor(1,callSip);
			   Intent mCallIntent = new Intent(this,CallPageActivity.class);
				
				mCallIntent.putExtra("roomNumber", inputRoomNumberString);
				startActivity(mCallIntent);
			   
			   return true;
		   }else if(keyCode == 67){
//			   etDailNum.setText("");
//			   return true;
			   String inputRoomNumberString = etDailNum.getText().toString();
			   if(inputRoomNumberString.equals("")){
				   finish();
			   }
		   }else if(keyCode == 19){
				   LinphoneManager.isShowingInputActivity = true;
				   String callSip = getCallSip("841");
				   Log.d("Conversation", "call sip =  "+callSip);
				   initCallInfor(1,callSip);
				   Intent mCallIntent = new Intent(this,CallPageActivity.class);
					
					mCallIntent.putExtra("roomNumber", "841");
					startActivity(mCallIntent);
				   return true;
				   
			   }else if(keyCode == 20){
				   LinphoneManager.isShowingInputActivity = true;
				   LinphoneManager.isShowingInputActivity = true;
				   Intent mPhoneBookIntent = new Intent(this, PhoneBookActivity.class);
				   startActivity(mPhoneBookIntent);
				   return true;
				   
			   }else if(keyCode == 21){
				   LinphoneManager.isShowingInputActivity = true;
				   initCallInfor(1,"10.99.26.1");
				   Intent mCallIntent = new Intent(this,CallPageActivity.class);
					
					mCallIntent.putExtra("roomNumber", "901");
					startActivity(mCallIntent);
				   return true;
				   
			   }else if(keyCode == 22){
				   LinphoneManager.isShowingInputActivity = true;
				   Intent mOpenDoorIntent = new Intent(this, PasswordActivity.class);
				   mOpenDoorIntent.putExtra("type", "door");
				   startActivity(mOpenDoorIntent);
				   return true;
				   
			   }
	   }
	   
	   if(isCongestionRinging){
		   return true;
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

}
