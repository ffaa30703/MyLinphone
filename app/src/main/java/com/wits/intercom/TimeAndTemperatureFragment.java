package com.wits.intercom;

import org.linphone.LinphoneManager;
import org.linphone.ui.witsui.ISetCurrentTime;
import org.linphone.ui.witsui.TimeHandle;
import org.linphone.ui.witsui.TimeThread;
import org.linphone.ui.witsui.Tools;

import com.keep.lin.R;
import com.wits.linphone.LinphoneI2CInterface;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class TimeAndTemperatureFragment extends Fragment {
	private View mView;
	private TextView timeTextView,temperatureText;
	private TimeAndTemperatureFragment instance;
	
	private int mYear,mMonth,mDay,mHour,mMinunt;
	private int[] saveTemp = new int[1];
	private Handler mHandler;
	private LinphoneI2CInterface mLinphoneI2CInterface;
	
	@SuppressLint("HandlerLeak")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mView=inflater.inflate(R.layout.bottom_layout, container, false);
		timeTextView=(TextView)mView.findViewById(R.id.time_textview);
		temperatureText = (TextView)mView.findViewById(R.id.temperature);
		Time t=new Time();
		t.setToNow();
		mYear=t.year;
		mMonth=t.month+1;
		mDay=t.monthDay;
		mHour=t.hour;
		mMinunt=t.minute;
		
		timeTextView.setText(orderTime(mHour)+":"+orderTime(mMinunt));
		
		mLinphoneI2CInterface = new LinphoneI2CInterface();
		
		int[] dataTem = new int[1];
		dataTem = LinphoneManager.getTemperature();
		if (dataTem[0]>128) {
			int tem = dataTem[0]-128;
			temperatureText.setText("-"+(tem/16)+""+(tem%16)+"°C");
		}else if (dataTem[0]==128) {
			temperatureText.setText("0°C");
		}else {
			temperatureText.setText((dataTem[0]/16)+""+(dataTem[0]%16)+"°C");
		}
		
		mHandler=new Handler(){
			public void handleMessage(android.os.Message msg) {
				switch (msg.what) {
				case Constance.GET_TEMPRETURE:
//					Log.d("get tempreture ", "receive message for get tempreture ");
				
						int[] dataTem = new int[1];
						int resultTem = mLinphoneI2CInterface.GetTemperature(dataTem);
						int errNumber =  1;
						while(resultTem == -1 && errNumber < 10){
							resultTem = mLinphoneI2CInterface.GetTemperature(dataTem);
							errNumber++;
						}
						if(resultTem != -1){
							LinphoneManager.setTemperature(dataTem);
							if (dataTem[0]>128) {
								int tem = dataTem[0]-128;
								temperatureText.setText("-"+(tem/16)+""+(tem%16)+"°C");
							}else if (dataTem[0]==128) {
								temperatureText.setText("0°C");
							}else {
								temperatureText.setText((dataTem[0]/16)+""+(dataTem[0]%16)+"°C");
							}
						}else {
							dataTem = LinphoneManager.getTemperature();
							if (dataTem[0]>128) {
								int tem = dataTem[0]-128;
								temperatureText.setText("-"+(tem/16)+""+(tem%16)+"°C");
							}else if (dataTem[0]==128) {
								temperatureText.setText("0°C");
							}else {
								temperatureText.setText((dataTem[0]/16)+""+(dataTem[0]%16)+"°C");
							}
						}
						
						Time t=new Time();
						t.setToNow();
						mYear=t.year;
						mMonth=t.month+1;
						mDay=t.monthDay;
						mHour=t.hour;
						mMinunt=t.minute;
						
						timeTextView.setText(orderTime(mHour)+":"+orderTime(mMinunt));
//						mHandle.sendEmptyMessageDelayed(Constance.FLUSH_TIME, 5000);
						
						
					break;
					
				case Constance.FLUSH_TIME:
//					Time t=new Time();
//					t.setToNow();
//					mYear=t.year;
//					mMonth=t.month+1;
//					mDay=t.monthDay;
//					mHour=t.hour;
//					mMinunt=t.minute;
//					
//					dataText.setText(orderTime(mDay)+"."+orderTime(mMonth)+"."+orderTime(mYear));
//					timeText.setText(orderTime(mHour)+":"+orderTime(mMinunt));
//					mHandle.sendEmptyMessageDelayed(Constance.FLUSH_TIME, 5000);
					break;

				default:
					break;
				}
				
			}
		};
		mHandler.sendEmptyMessageDelayed(Constance.GET_TEMPRETURE, 1000);
		new TimeThread(mHandler).start();
		return mView;
	}
	
	private String orderTime(int time){
		String value=String.valueOf(time);
		if(time<10){
			return "0"+value;
		}else{
			return value;
		}
	}


	
	
	
}
