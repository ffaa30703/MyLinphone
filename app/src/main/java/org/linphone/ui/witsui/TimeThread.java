package org.linphone.ui.witsui;


import com.wits.intercom.Constance;

import android.os.Handler;
import android.os.Message;
import android.text.format.Time;
import android.util.Log;

public class TimeThread extends Thread {
	private Handler mHandler;
	private int mHour,mMinunt;
	private static boolean key=true;
	public TimeThread(Handler handler){
		mHandler=handler;
	}
	public static synchronized void stopthread()
	{
		key=false; 
	}
	public static synchronized boolean getrunflag()
	{
		return key; 
	}
	@Override
	public void run() {
		super.run();
		key=true;
		while (getrunflag()) {
			Time t=new Time();
			t.setToNow();
			mHour=t.hour;
			mMinunt=t.minute;
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			ClientTime cTime=ClientTime.getInstance();
			cTime.hour=mHour;
			cTime.minunt=mMinunt;
			
			Message messageMessage=new Message();
			messageMessage.what=Constance.GET_TEMPRETURE;
			messageMessage.obj=cTime;
			mHandler.sendMessage(messageMessage);
			
		} ;
		
	}

	
}
