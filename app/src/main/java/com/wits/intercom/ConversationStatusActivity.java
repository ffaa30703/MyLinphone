package com.wits.intercom;


import org.linphone.LinphoneManager;
import org.linphone.LinphoneService;
import org.linphone.LinphoneSimpleListener.LinphoneOnCallStateChangedListener;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCall.State;
import org.linphone.ui.witsui.Constant;

import com.keep.lin.R;
import com.wits.intercom.SandyClockFragment.IprocessConverSation;
import com.wits.intercom.Constance;

import android.R.integer;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.ImageView;
import android.widget.TextView;

public class ConversationStatusActivity extends FragmentActivity implements LinphoneOnCallStateChangedListener{
	private int conversationStatus = 2;
	private final static int CONVERSATION=1;
	private final static int RINGING=2;
	private final static int BUSY=3;
	private ImageView phoneImage,conversionStautsImg;
	private CallerStyle callStyle; 
	private String callIp;
	private String mRoomNumberString;
	private LinphoneCall mCall;
	
	private TextView mRoomNumber;
	
	private final int CHECK_BUSY_STATE = 1;
	private boolean isConnected = false;
	
	
	
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(final Message msg) {
			switch (msg.what) {
			case CHECK_BUSY_STATE:
				if(!isConnected){
					phoneImage.setImageResource(R.drawable.wits_phone_conversation_busy);
					conversionStautsImg.setImageResource(R.drawable.wits_conversation_status_busy);
				}
				break;

			default:
				break;
			}
		}
	};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		setContentView(R.layout.wits_activity_conversation_startus);
		setContentView(R.layout.wits_dailing_conversation_startus);
//    	SandyClockFragment.initArgsForSandyClockFragment(this, this);
		mRoomNumber = (TextView)findViewById(R.id.et_room_no_show);
		MyApplication.getInstnce().addActivity(this);
		findView();
		
		
		
	}
	
	
	//需要进行IP地址转换
	@Override
	protected void onStart() {
		super.onStart();
		//防止重复监听
		LinphoneManager.addListener(this);
		
		Intent mIntent = getIntent();
		mRoomNumberString =mIntent.getStringExtra("roomNumber");
		mRoomNumber.setText(mRoomNumberString);
		callIp = mIntent.getStringExtra("sipip");
		
		CallThread thread = new CallThread();
		thread.start();
		
		handler.sendEmptyMessageDelayed(CHECK_BUSY_STATE, 30000);
		
		getConversionAndShow();  //拿到具体接通状态
	}
	
	@Override
	protected void onStop() {
		LinphoneManager.removeListener(this);
//		LinphoneService.isShowEditActivity = false;
		super.onStop();
	}

	private void getConversionAndShow() {
		switch (conversationStatus) {
		case CONVERSATION:
			phoneImage.setImageResource(R.drawable.wits_phone_conversation_connect);
			conversionStautsImg.setImageResource(R.drawable.wits_conversation_status_connect);
			break;
		case RINGING:
			phoneImage.setImageResource(R.drawable.wits_phone_conversation_ringing);
			conversionStautsImg.setImageResource(R.drawable.wits_conversation_status_ringing);
			break;
		case BUSY:
			phoneImage.setImageResource(R.drawable.wits_phone_conversation_busy);
			conversionStautsImg.setImageResource(R.drawable.wits_conversation_status_busy);
			break;

		default:
			break;
		}
	}

	private void findView() { 
		
		phoneImage=(ImageView)findViewById(R.id.phone_conversion); //电话图片 根据实际接通情况进行判断
		conversionStautsImg=(ImageView)findViewById(R.id.conversion_status);//电话状态 根据实际接通情况进行判断
		
	}
	
	//通话状态发生变化
	@Override
	public void onCallStateChanged(LinphoneCall call, State state,String message) {
		
		Log.d("State Changed", state.toString());
		
		if(state == State.Connected){
			mCall = call;
			isConnected = true;
			phoneImage.setImageResource(R.drawable.wits_phone_conversation_connect);
			conversionStautsImg.setImageResource(R.drawable.wits_conversation_status_connect);
		}else if(state == State.CallEnd || state == State.Error){
			phoneImage.setImageResource(R.drawable.wits_phone_conversation_busy);
			conversionStautsImg.setImageResource(R.drawable.wits_conversation_status_busy);
			MyApplication.getInstnce().close();
		}
		
	}
	

	
	private class CallThread extends Thread {
		@Override
		public void run() {
			super.run();
			try {
				sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				LinphoneManager.getInstance().newOutgoingCallString(
						"sip:" + callIp, false);
			}

		}
	}

	private class stopCall extends Thread {
		public void run() {
			super.run();
			try {
				sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				stopCall();
			}

		}

	}

	private void stopCall() {

		Log.w(Constance.LOGTAG, "========stopCall =============");
		if (LinphoneManager.getLc().getCallsNb() > 1) {
			LinphoneManager.getLc().terminateCall(mCall);
		} else {
			LinphoneManager.getInstance().terminateCall();
		}
		finish();
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		
		// MyApplication.getInstnce().close();
//		new stopCall().start();
		LinphoneManager.isShowingInputActivity = false;
		
		if (LinphoneManager.getLc().getCallsNb() > 1) {
			LinphoneManager.getLc().terminateCall(mCall);
		} else {
			LinphoneManager.getInstance().terminateCall();
		}
		
		super.onDestroy();
		
	}
	
	@Override  
	public boolean dispatchKeyEvent(KeyEvent event) {  
	   // TODO Auto-generated method stub  
	   // 判断普通按键  
	   int keyCode = event.getKeyCode();
	   if(event.getAction() == KeyEvent.ACTION_DOWN){
		   
		   Log.d("Key Event", "Key code = "+keyCode);
		 if(keyCode == 67){
			 stopCall();
			 return true;
			   
		   }
	   }
	   return super.dispatchKeyEvent(event);
	   
	} 

}
