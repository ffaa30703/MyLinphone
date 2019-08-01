package com.wits.intercom;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Locale;

import org.linphone.LinphoneManager;

import com.keep.lin.R;
import com.mdeal.data.Media;
import com.mdeal.data.MediaDao;
import com.wits.intercom.dialing.EditToCallActivity;
import com.wits.intercom.util.Logger;

import android.R.integer;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ListView;

public class PhoneBookActivity extends FragmentActivity{
	private final String TAG = PhoneBookActivity.class.getSimpleName();
	
	private ListView mListView;
	private ArrayList<PhoneBook> phoneBooks;
	private int iFirstOrLastItemSelected ;
	
	private MediaDao mMediaDao;
	private ArrayList<String> mMediaList;
	private int currentPosition = 0;
//	private ArrayList<String> mCurrentList;
	private int selector = 0;
	private int page = 0;
	private LanguageAdapter mAdapter;
	private String devIpString;

	protected int CLEAR_INPUT_MESSAGE = 10;
	
	private Handler mHandle = new Handler() {
		public void dispatchMessage(android.os.Message msg) {
			Log.w(Constance.LOGTAG, "get message");

			if(msg.what == CLEAR_INPUT_MESSAGE ){
				 finish();
			}
			
		};
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wits_activity_phonebook);
		try {
			devIpString = getLocalIPAddress();
			Log.d("System", "dev ip = "+devIpString);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		mCurrentList = new ArrayList<String>();
//		mMediaDao =  new MediaDao(this);
		mMediaList = new ArrayList<String>();
		getPhoneBooksFromSdcard();
		mListView=(ListView)findViewById(R.id.phone_book_listview);
		mAdapter = new LanguageAdapter(this, mMediaList);
		mListView.setAdapter(mAdapter);
		
		//监听Window中的选中(高亮)的item  
    	mListView.setOnItemSelectedListener(itemSelectedListener);  
    	mListView.setOnItemClickListener(clickListener);
    	mListView.setSelectionFromTop(selector, 0);
    	
	}
	
	private void nextPage(){
		 page += 6;
		 if(page <(mMediaList.size()-1)){
			   mListView.setSelectionFromTop(page,0);
			   selector = page;
		   }else{
			   page = 0;
			   mListView.setSelectionFromTop(page,0);
			   selector = page;
		   }
	}
	
	private void upPage(){
		page -= 6;
		if( page >= 0){
			  mListView.setSelectionFromTop(page,0); 
			  selector = page;
		  }else{
			  page = ((mMediaList.size()-1)/6)*6;
			  mListView.setSelectionFromTop(page,0); 
			  selector = page;
		  }
		  
	}
	

   public 	OnItemClickListener clickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
			//在此可实现响应item被点击后的功能
			  String dataString = mMediaList.get(position);
			  String[] sepDateStrings = dataString.split(" ", 2);
			  String sipip = getCallSip(sepDateStrings[0]);
			  
			  String name = sepDateStrings[1];
			  Log.d("phone book", "sipip = "+sipip);
			  initCallInfor(1,sipip);
			  Intent mCallIntent = new Intent(PhoneBookActivity.this,CallPageActivity.class);
				
			  mCallIntent.putExtra("roomNumber", name);
			  startActivity(mCallIntent);
			  
		}
	};
	
   public  OnItemSelectedListener itemSelectedListener = new OnItemSelectedListener() {  
		@Override
		public void onItemSelected(AdapterView<?> arg0, View view, int position,
				long id) {
			mAdapter.setCurrentItem(position);
			mAdapter.notifySetDataChange();
		}
		@Override
		public void onNothingSelected(AdapterView<?> arg0) {	
			  Log.d("itemselected", "nothing selected ");
		}   
    };  
	
	private CallInfor initCallInfor(int role,String sipString){
		CallInfor callInfor=CallInfor.getInstance();
//		callInfor.sipip=sipString;
//		callInfor.role=role;
		CallInfor.setSipip(sipString);
		CallInfor.setRole(role);
		
		return callInfor;
	}
	
	public boolean dispatchKeyEvent(KeyEvent event) {  
		   // TODO Auto-generated method stub  
		   // 判断普通按键  
		   int keyCode = event.getKeyCode();
		   if(event.getAction() == KeyEvent.ACTION_DOWN){
			   mHandle.removeMessages(CLEAR_INPUT_MESSAGE);
			   mHandle.sendEmptyMessageDelayed(CLEAR_INPUT_MESSAGE, 10000);
			   Log.d("Key Event", "Key code = "+keyCode);
			if(keyCode == 19){             //left up
				   selector--;
					if(selector>=0){
						mListView.setSelectionFromTop(selector, 100);
						page = (selector/6)*6;
					}else{
						selector = mMediaList.size() - 1;
						mListView.setSelectionFromTop(selector, 100);
						page = (selector/6)*6;
					}
				   return true;
			   }else if(keyCode == 20){   //left down
				   selector++;
					if(selector <= mMediaList.size()-1){
						mListView.setSelectionFromTop(selector, 100);
						page = (selector/6)*6;
					}else{
						selector = 0;
						mListView.setSelectionFromTop(selector, 100);
						page = (selector/6)*6;
					}
				  return true;
			   }else if(keyCode == 21){   //right up
				   upPage();
				   return true;
				   
			   }else if(keyCode == 22){   //right down
				   nextPage();
				   return true;
				   
			   }else if(keyCode == 67){
				   finish();
				   return true;
			   }
		   }
		   return super.dispatchKeyEvent(event);
		   
		} 
	
	private void getPhoneBooksFromSdcard(){
		
		String defaultLocal = Locale.getDefault().toString();
		Log.d("language", "default local = "+defaultLocal);
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
	            Log.d(TAG, line.toString());
	            mMediaList.add(line);
	        }
	        
	        reader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
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
			
			int room = Integer.parseInt(inputRoomNumber.replaceAll("\\D+","").replaceAll("\r", "").replaceAll("\n", "").trim(),10);
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
			}else if (room ==801 ) {
				thirdPartString = "21";
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
	protected void onPause(){
		super.onPause();
		mHandle.removeMessages(CLEAR_INPUT_MESSAGE);
	}
	
	@Override 
	protected void onResume(){
		super.onResume();
		page = 0;
		selector = 0;
		mListView.setSelectionFromTop(0, 0);
		mHandle.sendEmptyMessageDelayed(CLEAR_INPUT_MESSAGE, 10000);
	}
	
}
