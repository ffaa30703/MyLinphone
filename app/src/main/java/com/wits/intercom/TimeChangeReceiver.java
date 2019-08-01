package com.wits.intercom;

import org.linphone.LinphoneManager;

import com.wits.linphone.LinphoneI2CInterface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.format.Time;
import android.util.Log;

public class TimeChangeReceiver extends BroadcastReceiver{
		private static LinphoneI2CInterface mLinphoneI2CInterface;
	
		private int mYear,mMonth,mDay,mHour,mMinunt;
		
		public TimeChangeReceiver() {
			// TODO Auto-generated constructor stub
			mLinphoneI2CInterface = new LinphoneI2CInterface();
		}
		
		// TODO Auto-generated method stub
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d("time change", intent.getAction());
			// TODO Auto-generated method stub
			if(intent.getAction().equals(Intent.ACTION_TIME_CHANGED)){
				if(!LinphoneManager.getTimeChangeFlag()){
					
					LinphoneManager.setTimeChangeFlag(false);
					
					Time t=new Time();
					t.setToNow();
					mYear=t.year;
					mMonth=t.month;                  //note: month is 0--11
					mDay=t.monthDay;
					mHour=t.hour;
					mMinunt=t.minute;
					
					mMonth+=1;                     //note: month is 0--11
					
					String mYearForString = String.valueOf(mYear);
					String temYearString = mYearForString.substring(2, mYearForString.length());
					mYear = Integer.parseInt(temYearString);
					
					Log.d("Time", "it is time:"+mYear+"/"+mMonth+"/"+mDay+"/"+mHour+"/"+mMinunt);
					
					int errNumber = 1;
//					int setAskResult = mLinphoneI2CInterface.setAskTime();
//					Log.d("set ask time from i2c","set ask time result=="+setAskResult+"try times="+errNumber);
//					while(setAskResult == -1 && errNumber < 10){
//						setAskResult = mLinphoneI2CInterface.setAskTime();
//						errNumber++;
//						Log.d("set ask time from i2c","set ask time result=="+setAskResult+"try times="+errNumber);
//					}
					
					int resultSet = mLinphoneI2CInterface.SetTime(mHour,mMinunt, mDay, mMonth, mYear);
					Log.d("set time from i2c","set time result=="+resultSet);
					while(resultSet == -1 && errNumber < 10){
						resultSet = mLinphoneI2CInterface.SetTime(mHour,mMinunt, mDay, mMonth, mYear);
						errNumber++;
						Log.d("set time from i2c","set time result=="+resultSet+"try times="+errNumber);
						
					}          //just set time when time change
					
	//				errNumber = 1;
	//				int resultGetTime = mLinphoneI2CInterface.getTime(dataArry);
	//				Log.d("get time from i2c","get time result=="+resultGetTime);
	//				
	//				while(resultGetTime == -1 && errNumber < 10){
	//					resultGetTime = mLinphoneI2CInterface.getTime(dataArry);
	//					errNumber++;
	//					Log.d("get time from i2c","get time result=="+resultGetTime+"try times="+errNumber);
	//				}
				}else{
					LinphoneManager.setTimeChangeFlag(false);
				}
			}else if(intent.getAction().equals(Intent.ACTION_DATE_CHANGED)){
				if(!LinphoneManager.getTimeChangeFlag()){
					
					LinphoneManager.setTimeChangeFlag(false);
					
					Time t=new Time();
					t.setToNow();
					mYear=t.year;
					mMonth=t.month;
					mDay=t.monthDay;
					mHour=t.hour;
					mMinunt=t.minute;
					
					mMonth+=1;                     //note: month is 0--11
					
					Log.d("Time", "it is time:"+mYear+"/"+mMonth+"/"+mDay+"/"+mHour+"/"+mMinunt);
					
					String mYearForString = String.valueOf(mYear);
					String temYearString = mYearForString.substring(2, mYearForString.length());
					mYear = Integer.parseInt(temYearString);
					
					int errNumber = 1;
//					int setAskResult = mLinphoneI2CInterface.setAskTime();
//					Log.d("set ask time from i2c","set ask time result=="+setAskResult+"try times="+errNumber);
//					while(setAskResult == -1 && errNumber < 10){
//						setAskResult = mLinphoneI2CInterface.setAskTime();
//						errNumber++;
//						Log.d("set ask time from i2c","set ask time result=="+setAskResult+"try times="+errNumber);
//					}
					
					int resultSet = mLinphoneI2CInterface.SetTime(mHour,mMinunt, mDay, mMonth, mYear);
					Log.d("set time from i2c","set time result=="+resultSet);
					while(resultSet == -1 && errNumber < 10){
						resultSet = mLinphoneI2CInterface.SetTime(mHour,mMinunt, mDay, mMonth, mYear);
						errNumber++;
						Log.d("set time from i2c","set time result=="+resultSet+"try times="+errNumber);
						
					}
					
	//				errNumber = 1;
	//				int resultGetTime = mLinphoneI2CInterface.getTime(dataArry);
	//				Log.d("get time from i2c","get time result=="+resultGetTime);
	//				
	//				while(resultGetTime == -1 && errNumber < 10){
	//					resultGetTime = mLinphoneI2CInterface.getTime(dataArry);
	//					errNumber++;
	//					Log.d("get time from i2c","get time result=="+resultGetTime+"try times="+errNumber);
	//				}
				}
			}
		}

}
