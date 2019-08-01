package com.wits.intercom;

import static android.media.AudioManager.STREAM_RING;

import java.io.IOException;

import org.linphone.LinphoneActivity;
import org.linphone.LinphoneManager;
import org.linphone.LinphoneSimpleListener.LinphoneOnCallStateChangedListener;
import org.linphone.LinphoneSimpleListener.LinphoneOnMessageReceivedListener;
import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCallStats;
import org.linphone.core.LinphoneChatMessage;
import org.linphone.core.LinphoneChatMessage.StateListener;
import org.linphone.core.LinphoneChatRoom;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCall.State;

import android.R.integer;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;

import com.keep.lin.R;
import com.wits.intercom.CallInfor;
import com.wits.intercom.Constance;
import com.wits.intercom.MyApplication;

public class CallPageActivity extends FragmentActivity implements
		LinphoneOnCallStateChangedListener, OnClickListener, StateListener {
	
	private TextView statusDescribtView;
	private ImageView callRoleImg, videoImg, closeImg;
	private LinphoneChatRoom chatRoom;
	private int role;
	private String sipip;
	private boolean returnTag = false;
	private LinphoneCall mCall;
	private Intent intent;
	private String whichSide;// 是呼入电话还是呼出电话
	private boolean isCall;
	private boolean connected;
	private String message;
	private String number;
	private String dialNumber;
	private String sipToString;
	private String sipFromString;
	private MediaPlayer mMediaPlayer;
	private AudioManager mAudioManager;
	private int mSaveAudioMode;
	
	private boolean startRinging = false;
	private VideoRequestReceiver receiver;
	private VideoRequestOkReceiver receiverOk;
	private FollowMeReceiver followMeReceiver;
	private StartAudioReceiver startAudioReceiver;
	private BusyStateReceiver busyStateReceiver;
	
	private LinphoneCore lc;
	
	private static boolean isSetFollowMeFromCalled = false;
	
	private boolean dialogIsShown = true;
	
	private ImageView phoneImage,conversionStautsImg;
	private TextView mRoomNumber;
	private TextView mTimeTextView;
	private String mRoomNumberString;
	
	private final int UPDATE_TIME = 10;
	private final int GO_TO_S2P1 = 11;
	private final int WAIT_CALLED_RESPONSE = 12;
	private final int WAIT_OUT_TIME = 13;
	private final int START_AUDIO = 14;
	private final int BUSY_STATE = 15;
	private final int CLOSE_BY_BUSY = 16;
	private int  conversationTime = 30;
	private String curCallStats;
	
	private boolean startAudio = false;
	private boolean busyState = false;
	
	private int maxCallTime = 0;
	
	private Handler mHandle = new Handler() {
		public void dispatchMessage(Message msg) {
			Log.w(Constance.LOGTAG, "get message");

			if (msg.what == 1) {
				String state = msg.getData().getString("state");
				Log.w(Constance.LOGTAG, "=======get message=======" + state);
				if (state.equals("Connected")) {
//					phoneImage.setImageResource(R.drawable.wits_phone_conversation_connect);
//					conversionStautsImg.setImageResource(R.drawable.wits_conversation_status_connect);
				} else if (state.equals("CallEnd")) {
//					statusDescribtView.setText("CallEnd");
					
				} else if (state.equals("Error")) {
					phoneImage.setImageResource(R.drawable.wits_phone_conversation_busy);
					conversionStautsImg.setImageResource(R.drawable.wits_conversation_status_busy);
					mHandle.sendEmptyMessageDelayed(GO_TO_S2P1, 3000);
				}
			}else if(msg.what == UPDATE_TIME){
				conversationTime--;
				if(conversationTime>=0){
					mTimeTextView.setText(""+conversationTime);
					mHandle.sendEmptyMessageDelayed(UPDATE_TIME, 1000);
				}
				Log.d("update time", "curent time = "+conversationTime);
				
				if(conversationTime < 0){
					if(startAudio){
						conversationTime = 0;
						mHandle.sendEmptyMessage(GO_TO_S2P1);
					}else{
//						phoneImage.setImageResource(R.drawable.wits_phone_conversation_busy);
//						conversionStautsImg.setImageResource(R.drawable.wits_conversation_status_busy);
//						LinphoneManager.startRingForBusy();
//						mHandle.sendEmptyMessageDelayed(GO_TO_S2P1, 3000);
						conversationTime = 0;
						startActivity(new Intent(CallPageActivity.this,LinphoneWelcomeActivity.class));
					}
				}
			}else if(msg.what == GO_TO_S2P1){
				mHandle.removeMessages(UPDATE_TIME);
				LinphoneManager.stopRingForCongestion();
				LinphoneManager.stopRingForBusy();
				
				if (LinphoneManager.getLc().getCallsNb() > 1) {
					LinphoneManager.getLc().terminateCall(mCall);
				} else {
					LinphoneManager.getInstance().terminateCall();
				}
				
				Intent mIntent = new Intent(CallPageActivity.this,MainFunctionsActivity.class);
				startActivity(mIntent);
				
			}else if(msg.what == WAIT_CALLED_RESPONSE){
				if(curCallStats.equals("OutgoingProgress")){
					if(mMediaPlayer != null){
						mMediaPlayer.pause();  //add by jimmy at 2014.5.16
						mMediaPlayer.stop();
						mMediaPlayer.release();
						startRinging = false;
						mMediaPlayer = null;
					}
					phoneImage.setImageResource(R.drawable.wits_phone_conversation_off);
					conversionStautsImg.setImageResource(R.drawable.wits_conversation_status_off);
					Log.d("Linphone", "will play congestion tone");
					LinphoneManager.startRingForCongestion();
					mHandle.sendEmptyMessageDelayed(GO_TO_S2P1,3000);
				}
			}else if(msg.what == START_AUDIO){
				startAudio = true;
				phoneImage.setImageResource(R.drawable.wits_phone_conversation_connect);
				conversionStautsImg.setImageResource(R.drawable.wits_conversation_status_connect);
				conversationTime = 60;
				maxCallTime = 0;
				if(mMediaPlayer != null){
					mMediaPlayer.pause();  //add by jimmy at 2014.5.6
					mMediaPlayer.stop();
					mMediaPlayer.release();
					startRinging = false;
					mMediaPlayer = null;
				}
				
			}else if(msg.what == BUSY_STATE){
				phoneImage.setImageResource(R.drawable.wits_phone_conversation_busy);
				conversionStautsImg.setImageResource(R.drawable.wits_conversation_status_busy);
				if(mMediaPlayer != null){
					mMediaPlayer.pause();  //add by jimmy at 2014.5.6
					mMediaPlayer.stop();
					mMediaPlayer.release();
					startRinging = false;
					mMediaPlayer = null;
				}
				if (LinphoneManager.getLc().getCallsNb() > 1) {
					LinphoneManager.getLc().terminateCall(mCall);
				} else {
					LinphoneManager.getInstance().terminateCall();
				}
				LinphoneManager.startRingForBusy();
				mHandle.sendEmptyMessageDelayed(CLOSE_BY_BUSY, 3000);
			}else if(msg.what == CLOSE_BY_BUSY){
				mHandle.removeMessages(UPDATE_TIME);
				LinphoneManager.stopRingForCongestion();
				LinphoneManager.stopRingForBusy();
				busyState = false;
				Intent mIntent = new Intent(CallPageActivity.this,MainFunctionsActivity.class);
				startActivity(mIntent);
				
			}
			
		};
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.w(Constance.LOGTAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wits_dailing_conversation_startus);
		findViewAndProcess();
		MyApplication.getInstnce().addActivity(this);
		mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		conversationTime = 30;
	}
	
	@Override
	protected void onRestart() {
		Log.w(Constance.LOGTAG, "on restart");
		returnTag = true;
		super.onRestart();
	}

	private void setBusyByRole() {
//		switch (role) {
//		case CallInfor.DOOR_KEEPER:
//			statusDescribtView.setText(R.string.wits_doorkeeper_busy);
//			break;
//		case CallInfor.GUARD:
//			statusDescribtView.setText(R.string.wits_guard_busy);
//			break;
//
//		case CallInfor.NEIGHBOER:
//			statusDescribtView.setText(R.string.wits_guard_busy);
//			break;
//		// Add by wuhua
//		case CallInfor.BLOCKGUARD:
//			statusDescribtView.setText(R.string.wits_blockguard_busy);
//			break;
//		case CallInfor.SITEMANAGER:
//			statusDescribtView.setText(R.string.wits_sitemanager_busy);
//			break;
//		case CallInfor.SITESERVICE:
//			statusDescribtView.setText(R.string.wits_siteservice_busy);
//			break;
//		case CallInfor.ELEVATOR:
//			statusDescribtView.setText(R.string.wits_elevator_busy);
//			break;
//		case CallInfor.DOORPANNEL:
//			statusDescribtView.setText("DOOR PANEL BUSY");
//			break;
//		case CallInfor.IPCAMERA:
//			statusDescribtView.setText("IP CAMERA BUSY");
//			break;
//		default:
//			break;
//		}
	}

//	private void setConversationByRole() {
//		Log.e("callpageTag",
//				"---------------setConversationByRole----------------" + role);
//		int imgId = 0;
//		switch (role) {
//		case CallInfor.DOOR_KEEPER:
//			statusDescribtView.setText(R.string.wits_doorkeeper_conversation);
//			imgId = R.drawable.keeper;
//			break;
//		case CallInfor.GUARD:
//			statusDescribtView.setText(R.string.wits_guard_conversation);
//			imgId = R.drawable.keeper;
//			break;
//
//		case CallInfor.NEIGHBOER:
//			statusDescribtView.setText(R.string.wits_neighbor_conversation);
//			imgId = R.drawable.wits_incomingcall_neighbor;
//			break;
//		case CallInfor.BLOCKGUARD:
//			statusDescribtView.setText(R.string.wits_blockguard_conversation);
//			imgId = R.drawable.keeper;
//			break;
//		case CallInfor.SITEMANAGER:
//			statusDescribtView.setText(R.string.wits_sitemanager_conversation);
//			imgId = R.drawable.wits_call_sitemanager;
//			break;
//		case CallInfor.SITESERVICE:
//			statusDescribtView.setText(R.string.wits_siteservice_conversation);
//			imgId = R.drawable.wits_call_siteservice;
//			break;
//		case CallInfor.ELEVATOR:
//			statusDescribtView.setText(R.string.wits_elevator_conversation);
//			imgId = R.drawable.keeper;
//			break;
//		case CallInfor.DOORPANNEL:
//			statusDescribtView.setText("DOOR PANEL CONVERSATION");
//			imgId = R.drawable.keeper;
//			break;
//		case CallInfor.IPCAMERA:
//			statusDescribtView.setText("IP CAMERA CONVERSATION");
//			imgId = R.drawable.keeper;
//			break;
//		default:
//			imgId = R.drawable.keeper;
//			break;
//		}
//		callRoleImg.setBackgroundResource(imgId);
//		
//	}

	private void findViewAndProcess() {
//		try {
//			// 对方响铃之后，拨打电话方响起铃音
//			mMediaPlayer = new MediaPlayer();
//			// mMediaPlayer.setAudioStreamType(STREAM_RING);
//			String baseFilePathString = getFilesDir().getAbsolutePath();
//			mMediaPlayer
//					.setDataSource(baseFilePathString+"/playback.mp3");
//			mMediaPlayer.setLooping(true);
//			mMediaPlayer.prepare();
////			mMediaPlayer.setVolume(1f, 1f);
//		} catch (IllegalArgumentException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (SecurityException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IllegalStateException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		intent = getIntent();
		whichSide = intent.getStringExtra("whichside");
		connected = intent.getBooleanExtra("connected", false);
		message = intent.getStringExtra("message");
		number = intent.getStringExtra("number");
		mRoomNumberString =intent.getStringExtra("roomNumber");
		if (whichSide == null || whichSide.equals("")) {
			isCall = true;// 需要呼出电话
			dialNumber = intent.getStringExtra("dailNumber");//拨打的号码
		} else {
			isCall = false;// 不需要呼出电话
			if ("NEIGHBOR".equals(message)) {
				role = CallInfor.NEIGHBOER;
			} else if ("JANITOR".equals(message)) {
				role = CallInfor.GUARD;
			} else if ("GUARD".equals(message)) {
				role = CallInfor.GUARD;
			} else if ("SITE SERVICE".equals(message)) {
				role = CallInfor.SITESERVICE;
			} else if ("DOORKEEPER".equals(message)) {
				role = CallInfor.DOOR_KEEPER;
			} else if ("SITE GUARD".equals(message)) {
				role = CallInfor.SITEMANAGER;
			} else if ("BLOCK GUARD".equals(message)) {
				role = CallInfor.BLOCKGUARD;
			}else if("ELEVATOR".equals(message)){
				role = CallInfor.ELEVATOR;
			}else if("DOORPANEL".equals(message)){
				role = CallInfor.DOORPANNEL;
			}
		}

		mRoomNumber = (TextView)findViewById(R.id.et_room_no_show);
		mRoomNumber.setText(mRoomNumberString);
		mTimeTextView = (TextView)findViewById(R.id.conversion_time);
		
		phoneImage=(ImageView)findViewById(R.id.phone_conversion); //电话图片 根据实际接通情况进行判断
		conversionStautsImg=(ImageView)findViewById(R.id.conversion_status);//电话状态 根据实际接通情况进行判断
		

		sipFromString = getIntent().getStringExtra("sipip");
		createChatRoom();

		// 创建广播接收器
		receiver = new VideoRequestReceiver();
		registerReceiver(receiver, new IntentFilter(
				"com.intercom70.conversation.videorequest"));

		// 创建视频请求接受之后的广播接收器
		receiverOk = new VideoRequestOkReceiver();
		registerReceiver(receiverOk, new IntentFilter(
				"com.intercom70.conversation.acceptvideorequest"));
		
		followMeReceiver = new FollowMeReceiver();
		registerReceiver(followMeReceiver, new IntentFilter("com.intercom40.conversation.isSetFollowMe"));
		
		startAudioReceiver = new StartAudioReceiver();
		registerReceiver(startAudioReceiver, new IntentFilter("com.intercom40.conversation.startAudio"));
		
		busyStateReceiver = new BusyStateReceiver();
		  
		registerReceiver(busyStateReceiver, new IntentFilter("com.intercom40.conversation.busy"));
		
	}

	/**
	 * 创建聊天室，用来发送视频通话请求
	 */
	private void createChatRoom() {
		// 创建聊天室
		lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
		CallInfor callInfo = CallInfor.getInstance();
		sipToString = callInfo.sipip;
		
		Log.d("chatRoom message", "CallInfo sipip= "+sipToString);
		
		if (lc != null) {
			if (isCall) {
				chatRoom = lc.createChatRoom("sip:" + sipip);
			} else {
				chatRoom = lc.createChatRoom("sip:" + sipFromString);
			}
			// chatRoom = lc.createChatRoom("sip:10.1.3.35");
			Log.e("createchatroom", "创建聊天室 " + (chatRoom == null ? "失败" : "成功"));
			Log.e("createchatroom", "创建聊天室 地址--->" + callInfo.sipip);
			Log.e("createchatroom", "是呼出电话--->" + isCall + "----" + whichSide);
			Log.e("createchatroom", "呼入电话地址--->" + sipFromString);
			Log.e("createchatroom", "呼出电话地址--->" + sipToString);
		}
	}

	@Override
	protected void onStart() {
		Log.w(Constance.LOGTAG, "on onStart");
		super.onStart();
		startRinging = false;
		LinphoneManager.addListener(this); //changed by jimmy
		if ((!returnTag) && isCall) {
			pressCall();
		}
		Log.e("createchatroom", "onstart00000--->" + connected);
		if (connected) {
			Log.e("createchatroom", "onstart11111--->" + connected);
//			setConversationByRole();
		}
		
		
		
	}
	
	@Override
	protected void onStop(){
		super.onStop();
		conversationTime = 30;
		LinphoneManager.removeListener(this); 
		
		mHandle.removeMessages(UPDATE_TIME);
		 mHandle.removeMessages(GO_TO_S2P1 );
		 mHandle.removeMessages(WAIT_CALLED_RESPONSE);
		 mHandle.removeMessages(WAIT_OUT_TIME);
		 mHandle.removeMessages(START_AUDIO);
		 if(mMediaPlayer != null){
				mMediaPlayer.pause();  //add by jimmy at 2014.5.16
				mMediaPlayer.stop();
				mMediaPlayer.release();
				startRinging = false;
				mMediaPlayer = null;
		 }
		 LinphoneManager.stopRingForBusy();
		 LinphoneManager.stopRingForCongestion();
		 LinphoneManager.isShowingInputActivity = false;
		dialogIsShown = false;
		
		if (LinphoneManager.getLc().getCallsNb() > 1) {
			LinphoneManager.getLc().terminateCall(mCall);
		} else {
			LinphoneManager.getInstance().terminateCall();
		}
		
		unregisterReceiver(receiver);
		unregisterReceiver(receiverOk);
		unregisterReceiver(followMeReceiver);
		unregisterReceiver(startAudioReceiver);
		
	}

	private void pressCall() {

		CallInfor callInfor = CallInfor.getInstance();

//		role = callInfor.role;
		role = CallInfor.getRole();
		
		if (role != 0) {
//			setCallRoolImgByRole();
		} else {
			Log.w(Constance.LOGTAG, "the callinfor no role");
		}

//		sipip = callInfor.sipip;
		sipip = CallInfor.getSipip();

		if (null == sipip || "".equals(sipip)) {
			Log.w(Constance.LOGTAG, "the callinfor no sipip");
		} else {
			new CallThread().start();
		}
	}

//	private void setCallRoolImgByRole() {
//		int imgId;
//		int statusDescribt;
//		switch (role) {
//		case CallInfor.DOOR_KEEPER:
//			imgId = R.drawable.keeper;
//			statusDescribt = R.string.wits_doorkeeper_running;
//			break;
//		case CallInfor.GUARD:
//			imgId = R.drawable.keeper;
//			statusDescribt = R.string.wits_guard_running;
//			break;
//		case CallInfor.NEIGHBOER:
//			imgId = R.drawable.wits_incomingcall_neighbor;
//			statusDescribt = R.string.wits_neighbor_running;
//			break;
//		case CallInfor.SITEMANAGER:
//			imgId = R.drawable.wits_call_sitemanager;
//			statusDescribt = R.string.wits_sitemanager_running;
//			break;
//		case CallInfor.SITESERVICE:
//			imgId = R.drawable.wits_call_siteservice;
//			statusDescribt = R.string.wits_siteservice_running;
//			break;
//		case CallInfor.ELEVATOR:
//			imgId = R.drawable.keeper;
//			statusDescribt = R.string.wits_elevator_running;
//			break;
//		case CallInfor.BLOCKGUARD:
//			imgId = R.drawable.keeper;
//			statusDescribt = R.string.wits_blockguard_running;
//			break;
//		case CallInfor.DOORPANNEL:
//			imgId = R.drawable.keeper;
//			statusDescribt = R.string.wits_doorpanel_running;
//			break;
//		case CallInfor.IPCAMERA:
//			imgId = R.drawable.keeper;
//			statusDescribt = R.string.wits_ipcamera_running;
//			break;
//		default:
//			imgId = R.drawable.keeper;
//			statusDescribt = R.string.wits_doorkeeper_running;
//			break;
//		}
//		callRoleImg.setBackgroundResource(imgId);
//		statusDescribtView.setText(statusDescribt);
//	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.video:
			Log.w(Constance.LOGTAG, "press video");
			// Intent intent = new Intent(CallPageActivity.this,
			// ConversationActivity.class);
			// intent.putExtra("message", message + " " + number);
			// startActivity(intent);
			
			if (connected) {
				if (isCall) {
					sendTextMessage("Accepted Video?");
				} else {
					sendTextMessage("Accepted Video?");
				}
			}
			break;
		case R.id.close:
//			MyApplication.getInstnce().close();
//			new stopCall().start();
//			finish();          //changed by jimmy at 2014.4.28

			if(mMediaPlayer != null){
				mMediaPlayer.pause();  //add by jimmy at 2014.5.16
				mMediaPlayer.stop();
				mMediaPlayer.release();
				startRinging = false;
				mMediaPlayer = null;
			}
			
//			MyApplication.getInstnce().close();        //changed by jimmy at 2014.5.5

			break;
		default:
			break;
		}

	}

	@Override
	public void onCallStateChanged(LinphoneCall call, State state,
			String message) {
		Log.w(Constance.LOGTAG, "onCallStateChanged() state==>" + state);
		curCallStats = state.toString();
		if (State.Connected == call.getState()  || state.OutgoingInit == call.getState()) {
			mCall = call;
		}
		
		Message mes = new Message(); 
		mes.what = 1;
		Bundle b = new Bundle();
		b.putString("state", state.toString());
		mes.setData(b);
		mHandle.sendMessage(mes);
		
		if (!startRinging) {
			Log.e("CallPage", "CallPageStart--->" + state);
			if(mMediaPlayer == null){
				mMediaPlayer = new MediaPlayer();
				try {
					mMediaPlayer
							.setDataSource("/mnt/sdcard/Ringtones"+"/ringback_tone.mp3");
					
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SecurityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalStateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				mMediaPlayer.setLooping(true);
				try {
					mMediaPlayer.prepare();
				} catch (IllegalStateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				mMediaPlayer.start();
				startRinging = true;
			}
		}
//		if (state != LinphoneCall.State.OutgoingRinging && startRinging) {
//			if(mMediaPlayer != null){
//				Log.e("CallPage", "CallPageStop--->" + state);
//					if(mMediaPlayer != null){
//					mAudioManager.setMode(mSaveAudioMode);
//					mMediaPlayer.pause();  //add by jimmy at 2014.5.6
//					mMediaPlayer.stop();
//					mMediaPlayer.release();
//					startRinging = false;
//					mMediaPlayer = null;
//				}
//			}
//		}
		if (state == State.CallEnd) {
			// 呼叫失败、呼叫结束、呼叫释放这三种状态出现的时候关闭当前的Activity
			Log.e("CallPage", "CallPageEnd0000--->" + state);
			if(mMediaPlayer != null){
				mMediaPlayer.pause();  //add by jimmy at 2014.5.16
				mMediaPlayer.stop();
				mMediaPlayer.release();
				startRinging = false;
				mMediaPlayer = null;
			}
			if(isSetFollowMeFromCalled || busyState){
				
			}else{
				startActivity(new Intent(CallPageActivity.this,LinphoneWelcomeActivity.class));
			}
				
		}
		if (state == State.Connected) {
			connected = true;
		}

	}

	private class CallThread extends Thread {
		@Override
		public void run() {
			super.run();
			try {
				sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				
				isSetFollowMeFromCalled = false;
				mHandle.sendEmptyMessage(UPDATE_TIME);
//				mHandle.sendEmptyMessageDelayed(WAIT_CALLED_RESPONSE, 13000);
				LinphoneManager.getInstance().newOutgoingCallStringOnlyVideo(
						"sip:" + sipip, false,true);
				
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

	// @Override
	// public void finish() {
	// Log.w(Constance.LOGTAG, "========CallPageActivity finish=============");
	// if(mCall!=null){
	// stopCall();
	// }
	// super.finish();
	//
	//
	// }
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
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
			 mHandle.removeMessages(UPDATE_TIME);
			 mHandle.removeMessages(GO_TO_S2P1 );
			 mHandle.removeMessages(WAIT_CALLED_RESPONSE);
			 mHandle.removeMessages(WAIT_OUT_TIME);
			 mHandle.removeMessages(START_AUDIO);
			 if(mMediaPlayer != null){
					mMediaPlayer.pause();  //add by jimmy at 2014.5.16
					mMediaPlayer.stop();
					mMediaPlayer.release();
//					startRinging = false;
					mMediaPlayer = null;
				}
			 LinphoneManager.stopRingForBusy();
			 LinphoneManager.stopRingForCongestion();
			 startActivity(new Intent(CallPageActivity.this,LinphoneWelcomeActivity.class));
			 return true;
			   
		   }
	   }
	   return super.dispatchKeyEvent(event);
	   
	} 


	/**
	 * 发送视频通话请求消息
	 */
	private void sendTextMessage(String message) {
//		LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
//		boolean isNetworkReachable = lc == null ? false : lc
//				.isNetworkReachable();        //need not to check net status here,and this has some bug when use Ethernet
		
		createChatRoom();
		
		boolean isNetworkReachable = true;
		if (chatRoom != null && isNetworkReachable) {
			
			Log.d("chatRoom message", "chat room sip= "+chatRoom.getPeerAddress().asString()+" message="+message);
			
			LinphoneChatMessage chatMessage = chatRoom
					.createLinphoneChatMessage(message);
			chatRoom.sendMessage(chatMessage, this);
			
		} else if (!isNetworkReachable) {
			LinphoneActivity.instance().displayCustomToast(
					getString(R.string.error_network_unreachable),
					Toast.LENGTH_LONG);
		}
		Log.e("createchatroom", "开始发送聊天信息---->6");
	}

	@Override
	public void onLinphoneChatMessageStateChanged(LinphoneChatMessage msg,
			LinphoneChatMessage.State state) {
		// TODO Auto-generated method stub
		Log.e("createchatroom", "聊天信息状态---->" + msg.getText() + "---" + state);
	}

	/**
	 * 建立视频通话
	 */
//	private void goToEstablishVideo() {
//		Intent intentTem = new Intent(CallPageActivity.this,
//				ConversationActivity.class);
//		if (isCall&&dialNumber!=null) {
//			intentTem.putExtra("message", CallInfor.getStringByRole(CallInfor.getInstance().role) + " " +"CONVERSATION");
//		}else {
//			intentTem.putExtra("message", message+ " " + "CONVERSATION");
//			intentTem.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //add by jimmy at 2014.5.29
//		}
//		startActivity(intentTem);
//	}

	// 等待5秒钟之后关闭视频请求对话框的线程
	class DismissThread extends Thread {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				Thread.sleep(10000);
				
				if(dialogIsShown){
					dismissDialog(1);
				}
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}
	
//	protected Dialog onCreateDialog(int id) {
//		  switch (id) {
//		  case 1:{
//			  LayoutInflater inflater = getLayoutInflater();
//			  View layout = inflater.inflate(R.layout.answer_video_request_dialog,
//			  (ViewGroup) findViewById(R.id.answer_video_request));
//			  
//			  TextView title = (TextView)layout.findViewById(R.id.answer_video_title);
//			  TextView acceptVideo = (TextView)layout.findViewById(R.id.answer_video_yes);
//			  TextView refuseVideo = (TextView)layout.findViewById(R.id.answer_video_no);
//			  acceptVideo.setOnClickListener(new OnClickListener() {
//						
//						@Override
//						public void onClick(View v) {
//							// TODO Auto-generated method stub
//							sendTextMessage("YES");
//							goToEstablishVideo();
//							dismissDialog(1);
//							dialogIsShown = false;
//						}
//					});
//			  refuseVideo.setOnClickListener(new OnClickListener() {
//					
//					@Override
//					public void onClick(View v) {
//						// TODO Auto-generated method stub
//						dismissDialog(1);
//						dialogIsShown = false;
//					}
//				});
//			  
//			  Dialog mDialog = new Dialog(this,R.style.FullHeightDialog);
//			  mDialog.setContentView(layout);
//			  
//			  Window win = mDialog.getWindow();
//			  WindowManager.LayoutParams params = win.getAttributes();
//			  params.x = 0;//设置x坐标
//			  params.y = 0;//设置y坐标
//			  
//			  WindowManager windowManager = getWindowManager();
//			  Display display = windowManager.getDefaultDisplay();
//
//			  
//			  params.width = 510;
//			  params.height = 190;
//			  params.alpha = (float) 0.9;
//			  
//			  
//			  win.setAttributes(params);
//			  return mDialog;
//		  }
//		  default:
//			  return null;
//		  }
//		
//		  
//	}
	

	// 接收视频请求的广播接收器
	class VideoRequestReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
//			AlertDialog.Builder builder = new AlertDialog.Builder(
//					CallPageActivity.this);
//			builder.setMessage("VIDEO ACCEPTED?")
//					.setTitle("Tips")
//					.setCancelable(false)
//					.setPositiveButton("YES",
//							new DialogInterface.OnClickListener() {
//								public void onClick(DialogInterface dialog,
//										int id) {
//									sendTextMessage("YES");
//									goToEstablishVideo();
//								}
//							})
//					.setNegativeButton("NO",
//							new DialogInterface.OnClickListener() {
//								public void onClick(DialogInterface dialog,
//										int id) {
//									dialog.cancel();
//								}
//							});
//			AlertDialog dialog = builder.create();
//			dialog.show();
//			new DismissThread(dialog).start();
			
			
			showDialog(1);
			dialogIsShown = true;
			 new DismissThread().start();
			
			
		}

	}

	// 视频请求被接受之后的广播接收器
	class VideoRequestOkReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
//			goToEstablishVideo();
		}

	}
	
	class FollowMeReceiver extends BroadcastReceiver{      //add by jimmy at 2014.5.21

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			isSetFollowMeFromCalled = true;
			
			if (LinphoneManager.getLc().getCallsNb() > 1) {         //close original call
				LinphoneManager.getLc().terminateCall(mCall);
			} else {
				LinphoneManager.getInstance().terminateCall();
			}
			
			sipip = intent.getStringExtra("followMeNumber");              //establish a new call 
			
			Log.d("Linphone", "received follow number = "+sipip);
			
			conversationTime = 30;
			
			 mHandle.removeMessages(UPDATE_TIME);
			 mHandle.removeMessages(GO_TO_S2P1 );
			 mHandle.removeMessages(WAIT_CALLED_RESPONSE);
			 mHandle.removeMessages(WAIT_OUT_TIME);
			 mHandle.removeMessages(START_AUDIO);
			new CallThread().start();
			
			
		}
		
	}
	
	class StartAudioReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			mHandle.sendEmptyMessage(START_AUDIO);
		}
		
	}
	
	class BusyStateReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			mHandle.sendEmptyMessage(BUSY_STATE);
			busyState = true;
		}
		
	}
	
}
