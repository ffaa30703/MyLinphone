package com.wits.intercom;

import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCall.State;

import com.keep.lin.R;
import com.wits.intercom.dialing.ConversationWindowShowActivity;
import com.wits.intercom.util.Logger;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class SandyClockFragment extends Fragment{
	private final String TAG = SandyClockFragment.class.getSimpleName();

	private static IprocessConverSation mProcess;
	private static Context mContext;
	private TextView sandyClockText;
	public  static State mState;
    private int WiatTime = 0;
	final Handler handler=new Handler(){
		public void handleMessage(Message msg) {
			switch(msg.what){
			case 1:			
				if(WiatTime>0){
				 WiatTime--;
				 sandyClockText.setText(String.valueOf(WiatTime));
				 Message message = handler.obtainMessage(1);  
			     handler.sendMessageDelayed(message, 1000);
				}else{
					mProcess.processOfTimeEnd();
				}
				break;
				
			case 2:			
				if(WiatTime>0){
				 WiatTime--;
				 sandyClockText.setText(String.valueOf(WiatTime));
				 Message message = handler.obtainMessage(2);  
			     handler.sendMessageDelayed(message, 1000);
				}
				break;		
			}
		};
	};
	
	public static void initArgsForSandyClockFragment(Context context,IprocessConverSation process){
		mProcess=process;
		mContext=context;
	}
   
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		 View view=inflater.inflate(R.layout.wits_sandyclockfragmet, container, false);
		
		 sandyClockText=(TextView) view.findViewById(R.id.conversion_time);
		 Message message = handler.obtainMessage(1);     // Message  
		 handler.sendMessageDelayed(message, 1000);     
		
		 return view;
	}
	
	public interface IprocessConverSation{
		public int processOfGetMessage();
		public void processOfTimeEnd();
	}

}
