package com.wits.intercom;

import org.linphone.LinphoneManager;
import org.linphone.LinphoneService;

import com.wits.intercom.util.Logger;

import android.os.Handler;

public class DeladyProcess  {
	private final String TAG = DeladyProcess.class.getSimpleName();
	private static boolean doDelayFlag = true;
	private  static DeladyProcess instatnce;
	
	public static void stopDelayThread(){
		doDelayFlag=false;
	}
	public static void doDelay(){
		doDelayFlag=true;
	}
	
	public  static DeladyProcess getInstance(Handler handler,int delayTime,final IdelayMethodrd delayMethord){
		if(instatnce==null){
		  return  new DeladyProcess(handler,delayTime,delayMethord);
		}else{
		  return  instatnce;
		}
	}
	// override 此函数带参构造函数,构造器
	private DeladyProcess(Handler handler,int delayTime,final IdelayMethodrd delayMethord){
		handler.postDelayed(new Runnable() {
			@Override
		    public void run()
			{
				Logger.e(TAG, "=======doDelayFlag======="+doDelayFlag);
				if(doDelayFlag && !LinphoneService.isShowEditActivity && !LinphoneManager.isInSetting)
				{
					delayMethord.delayProcess();
					doDelayFlag=false;
				}
			}
		}, delayTime);
	}

	public interface IdelayMethodrd{
		void delayProcess();
	}
}
