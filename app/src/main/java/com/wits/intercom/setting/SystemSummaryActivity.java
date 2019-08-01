package com.wits.intercom.setting;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

import org.linphone.LinphoneManager;

import com.keep.lin.R;
import com.wits.intercom.Constance;
import com.wits.intercom.LinphoneWelcomeActivity;


import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.widget.TextView;

public class SystemSummaryActivity extends FragmentActivity {
	private TextView mTextView;
	private SharedPreferences mSharedPreferences;
	private String blockNo,roomNo,doorDuration;
	protected int CLEAR_INPUT_MESSAGE = 10;
	private Handler mHandle = new Handler() {
		public void dispatchMessage(android.os.Message msg) {
			Log.w(Constance.LOGTAG, "get message");

			if(msg.what == CLEAR_INPUT_MESSAGE ){
				LinphoneManager.isInSetting = false;
				startActivity(new Intent(SystemSummaryActivity.this,LinphoneWelcomeActivity.class));
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
		setContentView(R.layout.activity_system_summary);
		mTextView = (TextView)findViewById(R.id.device_info);
		mSharedPreferences=getSharedPreferences("deviceInfo", 0);
		blockNo = mSharedPreferences.getString("systemBlockNo", "1");
		roomNo = mSharedPreferences.getString("systemRoomNo", "1");
		doorDuration = mSharedPreferences.getString("doorDuration", "1");
		String ethernet =getResources().getString(R.string.no_about);
		if(LinphoneManager.getActiveNetwork(this) != null){
			ethernet = getResources().getString(R.string.yes_about);
		}
		
		String cpuSerialString = getCPUSerial();
		StringBuilder mStringBuilder = new StringBuilder();
		for(int i = 0;i<cpuSerialString.length();i++){
			mStringBuilder.append(cpuSerialString.charAt(i));
			if(i%4 == 0){
				mStringBuilder.append(" ");
			}
		}

		
		String contentString = getResources().getString(R.string.block_number_about)+blockNo + "\r\n"
				+getResources().getString(R.string.room_number_about)+roomNo+"\r\n"
				+getResources().getString(R.string.door_release_duration)+doorDuration+"\r\n"
				+getResources().getString(R.string.ethernet_about)+ethernet+"\r\n"
				+"APK version: "+" MULTITEK40-v4.1-20150617"+"\r\n"
				+"FW version: "+"DIP40_"+getKernalVersion()+"\r\n"
				+"CPU Serial:"+mStringBuilder.toString();
		mTextView.setText(contentString);
	}
	
	public static String getKernalVersion() {

		String str = "";

		try {

		Process pp = Runtime.getRuntime().exec("cat /proc/version");

		InputStreamReader ir = new InputStreamReader(pp.getInputStream());

		LineNumberReader input = new LineNumberReader(ir);
		str = input.readLine();
		if(str.indexOf("SMP PREEMPT")>-1){
			String[] subStrings = str.split("SMP PREEMPT");
			return subStrings[1].trim();
		}

		} catch (IOException ex) {

		//赋予默认值

		ex.printStackTrace();

		}

		return "";

		}


	@Override  
	public boolean dispatchKeyEvent(KeyEvent event) {  
	   // TODO Auto-generated method stub  
	   // 判断普通按键  
	   int keyCode = event.getKeyCode();
	   if(event.getAction() == KeyEvent.ACTION_DOWN){                //bit0 : blue, bit1 : green, bit2 : red
		   
		   Log.d("Key Event", "Key code = "+keyCode);
		   if(keyCode == 19){                       //left up  red
			  
			   return true;
			   
		   }else if(keyCode == 20){            //left down
			   finish();
			   return true;
			   
		   }else if(keyCode == 21){          //right up green
			  
			   return true;
			   
		   }else if(keyCode == 22){        //right down  blue
			   
			   return true;
			   
		   }
	   }
	   return super.dispatchKeyEvent(event);
	   
	} 
	
	/**

	* 获取CPU序列号

	* 

	* @return CPU序列号(16位)

	* 读取失败为"0000000000000000"

	*/

	public static String getCPUSerial() {

	String str = "", strCPU = "", cpuAddress = "0000000000000000";

	try {

	//读取CPU信息

	Process pp = Runtime.getRuntime().exec("cat /proc/cpuinfo");

	InputStreamReader ir = new InputStreamReader(pp.getInputStream());

	LineNumberReader input = new LineNumberReader(ir);

	//查找CPU序列号

	for (int i = 1; i < 100; i++) {

	str = input.readLine();

	if (str != null) {

	//查找到序列号所在行

	if (str.indexOf("Serial") > -1) {

	//提取序列号

	strCPU = str.substring(str.indexOf(":") + 1,

	str.length());

	//去空格

	cpuAddress = strCPU;

	break;

	}

	}else{

	//文件结尾

	break;

	}

	}

	} catch (IOException ex) {

	//赋予默认值

	ex.printStackTrace();

	}

	return cpuAddress;

	}


}
