package com.wits.linphone;

import android.R.integer;


public class LinphoneI2CInterface {
	
	
	static{
		System.loadLibrary("LinPhone");		
	}
	 
	 /*
	  * get the result after set a proximity
	  */
	 public native int GetProximityStatue(int[] type); 
	 
	 /*
	  * get result after input door password
	  */
	 public native int GetDoorPassword(int[] result);
	 
	 /*
	  * sense a visitor coming 
	  */
	 public native int GetWakeup();   
	 
	 /*
	  * enter setting page
	  */
	 public native int GetSetupDoorpanel();      
	 
	 /*
	  * get temperature from base
	  */
	 public native int GetTemperature(int[] data);
	 
	 /*
	  * get time from base
	  */
	 public native int GetTime(int[] data);
	 
	 /*
	  * set proximity
	  * type :
	  * 0x01 Save  proximity card(s) to a room
	  * 0x02 Cancel proximty card(s) read 
	  * 0x03 Delete all proximty card(s) for a room
	  * 0x04 Room number that proximity card will be added/deleted S4-P4
	  */
	 public native int SetProximity(int type,int data1,int data2,int data3,int data4);
	 
	 /*
	  * set LED color 
	  * bit0 : blue, bit1 : green, bit2 : red
	  * type:
	  * 0x11 key
	  * 0x12 board 
	  */
	 
	 public native int SetLedRGB(int type,int color);
	 
	 /*
	  * activate the door opener
	  */
	 public native int SetDoorRelease();
	 
	 /*
	  * set the door open delay time
	  * duration in miliseconds
	  */
	 public native int SetDoorOpener(int time);
	 
	 /*
	  * clear all door passwords
	  */
	 public native int SetClearPassword();
	 
	 /*
	  * set ask time
	  */
	 public native int SetAskTime();
	 
	 /*
	  * set time
	  * 
	  */
	 public native int SetTime(int hour,int minute,int day,int month,int year);
	 
	 
   }