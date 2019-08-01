package com.wits.intercom.dialing;

import org.linphone.LinphoneManager;
import org.linphone.LinphoneSimpleListener.LinphoneOnCallStateChangedListener;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCall.State;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.keep.lin.R;
import com.wits.intercom.DeladyProcess;
import com.wits.intercom.DeladyProcess.IdelayMethodrd;
import com.wits.intercom.SandyClockFragment.IprocessConverSation;
import com.wits.intercom.CallerStyle;
import com.wits.intercom.LinphoneWelcomeActivity;
import com.wits.intercom.MainFunctionsActivity;

import com.wits.intercom.SandyClockFragment;
import com.wits.intercom.util.Logger;

public class ConversationWindowShowActivity extends FragmentActivity implements IdelayMethodrd,LinphoneOnCallStateChangedListener{
	private final String TAG = ConversationWindowShowActivity.class.getSimpleName();

	private ImageView phoneImage,conversionStautsImg;
	private TextView etRoomNOShow;
	public  int WiatTime = 30;
    private TextView sandyClockText ; 
	Message message_01,message_02;
	
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(final Message msg) {
			Log.i("zg", "=======ConversationStatusActivity============state===>"+msg.getData().getString("state"));
			Log.i("zg", "=======ConversationStatusActivity============message===>"+msg.getData().getString("msg"));
			String state=msg.getData().getString("state");

			if(State.IncomingReceived.toString().equals(state)|| State.IncomingReceived.toString()==state){
				//如果是接听电话
				Log.i("zg", "ConversationStatusActivity====IncomingReceived===");
			}else if(State.OutgoingInit.toString().equals(state)|| State.OutgoingInit.toString()==state){
				//如果是打电话			
				WiatTime = 28;
			}else if (State.OutgoingRinging.toString().equals(state)|| State.OutgoingRinging.toString()==state) {
				//响铃状态
				phoneImage.setImageResource(R.drawable.wits_phone_conversation_ringing);
				conversionStautsImg.setImageResource(R.drawable.wits_conversation_status_ringing);
				WiatTime = 28;
	
			}else if(State.Connected.toString().equals(state)|| State.Connected.toString()==state){
				//通话正常连接
				phoneImage.setImageResource(R.drawable.wits_phone_conversation_connect);
				conversionStautsImg.setImageResource(R.drawable.wits_conversation_status_connect);
				
				 WiatTime = 0;
				 msg01handler.removeCallbacks(msg01runnable);
        		 msg02handler.postDelayed(msg02runnable, 1000);
			}else if("Error".equals(state)||"Error".equals(state)){
				//对方挂断	
				phoneImage.setImageResource(R.drawable.wits_phone_conversation_busy);
				conversionStautsImg.setImageResource(R.drawable.wits_conversation_status_busy);
				WiatTime = 3;
			}else if ("CallEnd".equals(state)||"CallEnd".equals(state)) {
				//结束通话
				Log.i("zg", "ConversationStatusActivity====CallEnd===");			
				processOfTimeEnd();
			}
			super.handleMessage(msg);
		}
	};
	
    Handler msg01handler = new Handler();  
    Runnable msg01runnable = new Runnable() {  
        @Override  
        public void run() {       	
			if(WiatTime>0){			
				WiatTime--;
				 sandyClockText.setText(String.valueOf(WiatTime));
				 msg01handler.postDelayed(msg01runnable, 1000);  
			}else{
				processOfTimeEnd();
			}
        }  
    };  
	
    Handler msg02handler = new Handler();  
    Runnable msg02runnable = new Runnable() {  
        @Override  
        public void run() {       	        	         	
				WiatTime++;
				sandyClockText.setText(String.valueOf(WiatTime));
				msg02handler.postDelayed(msg02runnable, 1000);  					 
        }  
    };  
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wits_dailing_conversation_startus);		
		findView();
	}
	
	private void findView() {
		etRoomNOShow = (TextView) this.findViewById(R.id.et_room_no_show);
		etRoomNOShow.setText(getIntent().getStringExtra("room_no"));
		
		phoneImage=(ImageView)findViewById(R.id.phone_conversion); //电话图片 根据实际接通情况进行判断
		conversionStautsImg=(ImageView)findViewById(R.id.conversion_status);//电话状态 根据实际接通情况进行判断
		
		LinearLayout layout = (LinearLayout) findViewById(R.id.conversion_time_layout);
        sandyClockText =  (TextView) layout.findViewById(R.id.conversion_time);      
	}
	
	
	@Override
	protected void onStart() {	
		super.onStart();
		//防止重复监听
		LinphoneManager.removeListener(this);
		LinphoneManager.addListener(this);	
//		DeladyProcess.doDelay();
//		DeladyProcess.getInstance(new Handler(), 1000, this);				
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Logger.w(TAG, "===================onResume==========================");
	    msg01handler.postDelayed(msg01runnable, 1000);    
	}
	
	
	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		if(msg01handler!=null){
		msg01handler.removeCallbacks(msg01runnable);
//		msg01handler = null;
	}
	
	if(msg02handler!=null){
		msg02handler.removeCallbacks(msg02runnable);
//		msg02handler = null;
	}
	}
	
@Override
protected void onDestroy() {
	super.onDestroy();
	
//	if(msg01handler!=null){
//		msg01handler.removeCallbacks(msg01runnable);
//		msg01handler = null;
//	}
//	
//	if(msg02handler!=null){
//		msg02handler.removeCallbacks(msg02runnable);
//		msg02handler = null;
//	}
}

	@Override
	public void delayProcess() {
		finish();
	}


	public void processOfTimeEnd() {
	     startActivity(new Intent(ConversationWindowShowActivity.this,MainFunctionsActivity.class));
	     finish();	
	}

	//通话状态发生变化
	@Override
	public void onCallStateChanged(LinphoneCall call, State state,String message) {
		Logger.e(TAG, "=================onCallStateChanged====================");	
		Message msg = new Message();
        Bundle b = new Bundle();
		b.putString("state", state.toString());
		b.putString("msg", message.toString());
		msg.setData(b);
		this.handler.sendMessage(msg);
	}
	
}
