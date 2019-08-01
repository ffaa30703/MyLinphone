/*
LinphoneService.java
Copyright (C) 2010  Belledonne Communications, Grenoble, France

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.linphone;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.linphone.LinphoneManager.NewOutgoingCallUiListener;
import org.linphone.LinphoneSimpleListener.LinphoneServiceListener;
import org.linphone.core.CallDirection;
import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneChatMessage;
import org.linphone.core.LinphoneChatRoom;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCall.State;
import org.linphone.core.LinphoneChatMessage.StateListener;
import org.linphone.core.LinphoneCallParams;
import org.linphone.core.LinphoneCore.GlobalState;
import org.linphone.core.LinphoneCore.RegistrationState;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactoryImpl;
import org.linphone.core.OnlineStatus;
import org.linphone.mediastream.Log;
import org.linphone.mediastream.Version;

import android.R.anim;
import android.R.integer;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;

import com.keep.lin.R;
import com.wits.intercom.CallInfor;
import com.wits.intercom.Constance;

import com.wits.intercom.TimeChangeReceiver;
import com.wits.linphone.LinphoneI2CInterface;

import de.timroes.axmlrpc.Call;

/**
 * 
 * Linphone service, reacting to Incoming calls, ...<br />
 * 
 * Roles include:
 * <ul>
 * <li>Initializing LinphoneManager</li>
 * <li>Starting C libLinphone through LinphoneManager</li>
 * <li>Reacting to LinphoneManager state changes</li>
 * <li>Delegating GUI state change actions to GUI listener</li>
 * 
 * 
 * @author Guillaume Beraudo
 * 
 */

@SuppressLint("Wakelock")
public final class LinphoneService extends Service implements
		LinphoneServiceListener {
	/*
	 * Listener needs to be implemented in the Service as it calls
	 * setLatestEventInfo and startActivity() which needs a context.
	 */
	private static int INDOOR_RING = 9;
	public Handler mHandler = new Handler(){
		

		public void dispatchMessage(android.os.Message msg) {
			Log.w(Constance.LOGTAG, "get message");

			if (msg.what == INDOOR_RING) {
//				showAlertWindow();
			}
		}
	};
	private static LinphoneService instance;

	// private boolean mTestDelayElapsed; // add a timer for testing
	private boolean mTestDelayElapsed = true; // no timer
	private WifiManager mWifiManager;
	private WifiLock mWifiLock;
	// Add by wuhua
	private LinphoneCall mCall;
	private PowerManager pm ;
	private AudioManager mAudioManager;
	private WakeLock cpu_wakeLock; 
	private WakeLock screen_wakeLock;
	private int maxVolume ;

	public static boolean isReady() {
		return instance != null && instance.mTestDelayElapsed;
	}

	/**
	 * @throws RuntimeException
	 *             service not instantiated
	 * 
	 */
	public static LinphoneService instance() {
		if (isReady())
			return instance;

		throw new RuntimeException("LinphoneService not instantiated yet");
	}

	private final static int NOTIF_ID = 1;
	private final static int INCALL_NOTIF_ID = 2;
	private final static int MESSAGE_NOTIF_ID = 3;
	private final static int CUSTOM_NOTIF_ID = 4;

	private static final int IC_LEVEL_ORANGE = 0;
	/*
	 * private static final int IC_LEVEL_GREEN=1; private static final int
	 * IC_LEVEL_RED=2;
	 */
	private static final int IC_LEVEL_OFFLINE = 3;
	
	private static LinphoneI2CInterface mLinphoneI2CInterface;

	@Override
	public void onCreate() {
		android.util.Log.w("zg", "LinphoneService oncreate");
		super.onCreate();


		// In case restart after a crash. Main in LinphoneActivity
		// 得到LinphonePreferenceManager对象
		LinphonePreferenceManager.getInstance(this);

		// Set default preferences
		// 设置初始值
		PreferenceManager.setDefaultValues(this, R.xml.preferences, true);

		// Dump some debugging information to the logs
		Log.i(START_LINPHONE_LOGS);
		dumpDeviceInformation();
		dumpInstalledLinphoneInformation();

		Intent notifIntent = new Intent(this, incomingReceivedActivity);
		notifIntent.putExtra("Notification", true);

		// 创建并启动LinphoneManager
		LinphoneManager.createAndStart(this, this);
		// wifi设置
		mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		mWifiLock = mWifiManager.createWifiLock(
				WifiManager.WIFI_MODE_FULL_HIGH_PERF, this.getPackageName()
						+ "-wifi-call-lock");
		mWifiLock.setReferenceCounted(false);
		instance = this; // instance is ready once linphone manager has been
							// created

		// Retrieve methods to publish notification and keep Android
		// from killing us and keep the audio quality high.
		if (Version.sdkStrictlyBelow(Version.API05_ECLAIR_20)) {
			try {
				mSetForeground = getClass().getMethod("setForeground",
						mSetFgSign);
			} catch (NoSuchMethodException e) {
				Log.e(e, "Couldn't find foreground method");
			}
		} else {
			try {
				mStartForeground = getClass().getMethod("startForeground",
						mStartFgSign);
				mStopForeground = getClass().getMethod("stopForeground",
						mStopFgSign);
			} catch (NoSuchMethodException e) {
				Log.e(e, "Couldn't find startGoreground or stopForeground");
			}
		}

		// 设置PresenceInfo 参数0：马上开始，OnlineStatus.Online设置状态是在线状态
		LinphoneManager.getLc().setPresenceInfo(0, "", OnlineStatus.Online);
		
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);  
	    cpu_wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"My Tag" ); 
	    cpu_wakeLock.acquire();
	    
	    BroadcastReceiver mTimeChangeReceiver = new TimeChangeReceiver();
	    IntentFilter mTimeFilter = new IntentFilter();
	    mTimeFilter.addAction(Intent.ACTION_DATE_CHANGED);
	    mTimeFilter.addAction(Intent.ACTION_TIME_CHANGED);
	    registerReceiver(mTimeChangeReceiver, mTimeFilter);
	    
	    mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
	    
//	    BroadcastReceiver mScreenStateReceiver = new ScreenStateReceiver();
//	    IntentFilter mScreenFilter = new IntentFilter();
//	    mScreenFilter.addAction(Intent.ACTION_SCREEN_OFF);
//	    mScreenFilter.addAction(Intent.ACTION_SCREEN_ON);
//	    registerReceiver(mScreenStateReceiver, mScreenFilter);
//	    
//	    BroadcastReceiver mTimeChangeReceiver = new TimeChangeReceiver();
//	    IntentFilter mTimeFilter = new IntentFilter();
//	    mTimeFilter.addAction(Intent.ACTION_DATE_CHANGED);
//	    mTimeFilter.addAction(Intent.ACTION_TIME_CHANGED);
//	    registerReceiver(mTimeChangeReceiver, mTimeFilter);
	    mLinphoneI2CInterface = new LinphoneI2CInterface();
//	    Thread detectIndoorThread = new detectIndoorRingThread();
//	    detectIndoorThread.start();
//	    showAlertWindow();
	    
	    maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
		Log.d("AudioManager . Call Stream max volume = "+maxVolume);
		sp = getSharedPreferences("deviceInfo", 0);
		
	}

	private enum IncallIconState {
		INCALL, PAUSE, VIDEO, IDLE
	}

	private IncallIconState mCurrentIncallIconState = IncallIconState.IDLE;

	private synchronized void setIncallIcon(IncallIconState state) {

		Log.i("zg", "LinphoneService setIncallIcon");
		if (state == mCurrentIncallIconState)
			return;
		mCurrentIncallIconState = state;

		int notificationTextId = 0;
		int inconId = 0;

		switch (state) {
		case IDLE:

			return;
		case INCALL:
			inconId = R.drawable.conf_unhook;
			notificationTextId = R.string.incall_notif_active;
			break;
		case PAUSE:
			inconId = R.drawable.conf_status_paused;
			notificationTextId = R.string.incall_notif_paused;
			break;
		case VIDEO:
			inconId = R.drawable.conf_video;
			notificationTextId = R.string.incall_notif_video;
			break;
		default:
			throw new IllegalArgumentException("Unknown state " + state);
		}

		if (LinphoneManager.getLc().getCallsNb() == 0) {
			return;
		}

		LinphoneCall call = LinphoneManager.getLc().getCalls()[0];
		String userName = call.getRemoteAddress().getUserName();
		String domain = call.getRemoteAddress().getDomain();
		String displayName = call.getRemoteAddress().getDisplayName();
		LinphoneAddress address = LinphoneCoreFactoryImpl.instance()
				.createLinphoneAddress("sip:" + userName + "@" + domain);
		address.setDisplayName(displayName);

		Uri pictureUri = LinphoneUtils
				.findUriPictureOfContactAndSetDisplayName(address,
						getContentResolver());
		Bitmap bm = null;
		try {
			bm = MediaStore.Images.Media.getBitmap(getContentResolver(),
					pictureUri);
		} catch (Exception e) {
			bm = BitmapFactory.decodeResource(getResources(),
					R.drawable.unknown_small);
		}
		String name = address.getDisplayName() == null ? address.getUserName()
				: address.getDisplayName();

	}

	private static final Class<?>[] mSetFgSign = new Class[] { boolean.class };
	private static final Class<?>[] mStartFgSign = new Class[] { int.class,
			Notification.class };
	private static final Class<?>[] mStopFgSign = new Class[] { boolean.class };

	private Method mSetForeground;
	private Method mStartForeground;
	private Method mStopForeground;
	private Object[] mSetForegroundArgs = new Object[1];
	private Object[] mStartForegroundArgs = new Object[2];
	private Object[] mStopForegroundArgs = new Object[1];
	private Class<? extends Activity> incomingReceivedActivity = LinphoneActivity.class;
	private SharedPreferences sp;

	void invokeMethod(Method method, Object[] args) {

		Log.i("zg", "invokeMethod");

		try {
			method.invoke(this, args);
		} catch (InvocationTargetException e) {
			// Should not happen.
			Log.w(e, "Unable to invoke method");
		} catch (IllegalAccessException e) {
			// Should not happen.
			Log.w(e, "Unable to invoke method");
		}
	}

	public static final String START_LINPHONE_LOGS = " ==== Phone information dump ====";
	public static final boolean isShowEditActivity = false;

	// 得到一些设备信息
	private void dumpDeviceInformation() {
		Log.i("zg", "LinphoneService dumpDeviceInformation");

		StringBuilder sb = new StringBuilder();
		sb.append("DEVICE=").append(Build.DEVICE).append("\n");
		sb.append("MODEL=").append(Build.MODEL).append("\n");
		// MANUFACTURER doesn't exist in android 1.5.
		// sb.append("MANUFACTURER=").append(Build.MANUFACTURER).append("\n");
		sb.append("SDK=").append(Build.VERSION.SDK_INT);
		Log.i(sb.toString());
	}

	// 得到安装的软件的信息
	private void dumpInstalledLinphoneInformation() {

		Log.i("zg", "LinphoneService dumpInstalledLinphoneInformation");

		PackageInfo info = null;
		try {
			info = getPackageManager().getPackageInfo(getPackageName(), 0);
		} catch (NameNotFoundException nnfe) {
		}

		if (info != null) {
			Log.i("Linphone version is ", info.versionName + " ("
					+ info.versionCode + ")");
		} else {
			Log.i("Linphone version is unknown");
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public synchronized void onDestroy() {
		LinphoneManager.getLc().setPresenceInfo(0, "", OnlineStatus.Offline);
		instance = null;
		LinphoneManager.destroy();

		mWifiLock.release();
		cpu_wakeLock.release();
		super.onDestroy();
	}

	private static final LinphoneGuiListener guiListener() {
		return null;
	}

	public void onDisplayStatus(final String message) {
		mHandler.post(new Runnable() {
			public void run() {
				if (guiListener() != null)
					guiListener().onDisplayStatus(message);
			}
		});
	}

	public void onGlobalStateChanged(final GlobalState state,
			final String message) {
		if (state == GlobalState.GlobalOn) {

			// Slightly delay the propagation of the state change.
			// This is to let the linphonecore finish to be created
			// in the java part.
			mHandler.postDelayed(new Runnable() {
				public void run() {
					if (guiListener() != null)
						guiListener().onGlobalStateChangedToOn(message);
				}
			}, 50);
		}
	}

	public void onRegistrationStateChanged(final RegistrationState state,
			final String message) {
		// if (instance == null) {
		// Log.i("Service not ready, discarding registration state change to ",state.toString());
		// return;
		// }
		Log.i("zg", "LinphoneService onRegistrationStateChanged");

		if (state == RegistrationState.RegistrationOk
				&& LinphoneManager.getLc().getDefaultProxyConfig() != null
				&& LinphoneManager.getLc().getDefaultProxyConfig()
						.isRegistered()) {

		}

		if ((state == RegistrationState.RegistrationFailed || state == RegistrationState.RegistrationCleared)
				&& (LinphoneManager.getLc().getDefaultProxyConfig() == null || !LinphoneManager
						.getLc().getDefaultProxyConfig().isRegistered())) {

		}
		if (state == RegistrationState.RegistrationNone) {

		}

		mHandler.post(new Runnable() {
			public void run() {
				if (LinphoneActivity.isInstanciated()) {
					LinphoneActivity.instance().onRegistrationStateChanged(
							state);
				}
			}
		});
	}

	public void setActivityToLaunchOnIncomingReceived(
			Class<? extends Activity> activity) {
		Log.i("zg", "LinphoneService setActivityToLaunchOnIncomingReceived");

		incomingReceivedActivity = activity;

	}

	/**
	 * 接电话时的处理
	 * 
	 * @param state
	 */
	protected void onIncomingReceived(State state) {

		Log.i("zg", "LinphoneService onIncomingReceived");
		// wakeup linphone
		// startActivity(new Intent()
		// .setClass(this, incomingReceivedActivity)
		// .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
		// ZG
		// Add by wuhua
		if(mCall.getRemoteParams().getCustomHeader(LinphoneManager.HEARDER_IS_ONLYVIDEO).equals(LinphoneManager.VALUE_TRUE)){
			if(!mAudioManager.isMicrophoneMute()){
				Log.d("Incoming call ","only video set stream mute to true");
				mAudioManager.setMicrophoneMute(true);
			}
		}else{
//			mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVolume, 0);
			if(mAudioManager.isMicrophoneMute()){
				Log.d("Incoming call ","normal set stream mute to false");
				mAudioManager.setMicrophoneMute(false);
			}
		}
		answer();
		
//		if (LinphoneManager.VALUE_TRUE.equalsIgnoreCase(mCall.getRemoteParams()
//				.getCustomHeader(LinphoneManager.HEARDER_IS_MONITORING))) {
//			
//			if(mCall.getRemoteParams().getCustomHeader(LinphoneManager.HEARDER_IS_ONLYVIDEO).equals(LinphoneManager.VALUE_TRUE)){
//				if(!mAudioManager.isMicrophoneMute()){
//					Log.d("Incoming call ","only video set stream mute to true");
//					mAudioManager.setMicrophoneMute(true);
//				}
//			}else{
//				mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVolume, 0);
//				if(mAudioManager.isMicrophoneMute()){
//					Log.d("Incoming call ","normal set stream mute to false");
//					mAudioManager.setMicrophoneMute(false);
//				}
//			}
//			
//			answer();
//		} else {
//			
//			if(mAudioManager.isMicrophoneMute()){
//				Log.d("Incoming call ","normal set stream mute to false");
//				mAudioManager.setStreamMute(AudioManager.STREAM_VOICE_CALL, false);
//				mAudioManager.setMicrophoneMute(false);
//			}
//			mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVolume, 0);
//			
//			Log.d("Incoming call sipip = "+mCall.getRemoteAddress().getDomain());
//			String inComingIP = mCall.getRemoteAddress().getDomain();
//			String[] arrIP = (inComingIP == null) ? null : inComingIP
//					.split("\\.");
//			String doorMessage = "DOOR";// 来电界面上显示的设备类型
//			String number = arrIP[arrIP.length - 1];// 来电界面上显示的设备序号（IP地址中的最后一个字节）
//			int which = Integer.parseInt(arrIP[2]);
//			int num = Integer.parseInt(arrIP[arrIP.length-1]);
//			
//			
////			 arrIP[2] = "21";//很多IP没有办法测试,这里修改IP进行模拟测试,  just for test
//			 
//			 CallInfor temcInfor = CallInfor.getInstance();
//			 temcInfor.sipip = inComingIP;
//			 
////			if (inComingIP != null) {
////				// 根据呼进电话的IP地址决定显示响铃界面
////				if (arrIP != null) {
////					if ("22".equals(arrIP[2])){
////						doorMessage = "ELEVATOR";
////						Intent intent = new Intent(this,
////								InComingCallsDifferentActivity.class);
////						intent.putExtra("sipip", inComingIP);
////						intent.putExtra("message", doorMessage);
////						intent.putExtra("number", ((which-1)*254+num)+"");
////						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
////						startActivity(intent);
////					}else if ("23".equals(arrIP[2])) {
////						// 显示S4-P3,25,28不能打电话
////						doorMessage = "DOORKEEPER";
////						Intent intent = new Intent(this,
////								InComingCallsDifferentActivity.class);
////						intent.putExtra("sipip", inComingIP);
////						intent.putExtra("message", doorMessage);
////						intent.putExtra("number", ((which-1)*254+num)+"");
////						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
////						startActivity(intent);
////					} else if ("24".equals(arrIP[2])) {
////						// 显示S4-P3,25,28不能打电话
////						doorMessage = "BLOCK GUARD";
////						Intent intent = new Intent(this,
////								InComingCallsDifferentActivity.class);
////						intent.putExtra("sipip", inComingIP);
////						intent.putExtra("message", doorMessage);
////						intent.putExtra("number", ((which-1)*254+num)+"");
////						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
////						startActivity(intent);
////					}else if("26".equals(arrIP[2])){
////						doorMessage = "SITE GUARD";
////						Intent intent = new Intent(this,
////								InComingCallsDifferentActivity.class);
////						intent.putExtra("sipip", inComingIP);
////						intent.putExtra("message", doorMessage);
////						intent.putExtra("number", ((which-1)*254+num)+"");
////						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
////						startActivity(intent);
////					}else if ("27".equals(arrIP[2])) {
////						// 显示S4-P3,25,28不能打电话
////						doorMessage = "SITE SERVICE";  
////						Intent intent = new Intent(this,
////								InComingCallsDifferentActivity.class);
////						intent.putExtra("sipip", inComingIP);
////						intent.putExtra("message", doorMessage);
////						intent.putExtra("number", ((which-1)*254+num)+"");
////						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
////						startActivity(intent);
////					}else if ("21".equals(arrIP[2])) {
////						// 显示S4-P1，传递需要显示的信息过去
////						answer();
////						doorMessage = "DOORPANEL";
////						Intent intent = new Intent(this, InComingCalls.class);
////						intent.putExtra("message", doorMessage);
////						intent.putExtra("sipip", inComingIP);
////						intent.putExtra("number", number);
////						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
////						startActivity(intent);
////						
////					} else if ("29".equals(arrIP[2])) {
////						// 显示S4-P1，传递需要显示的信息过去
////						answer();
////						doorMessage = "DOORPANEL";
////						Intent intent = new Intent(this, InComingCalls.class);
////						intent.putExtra("message", doorMessage);
////						intent.putExtra("sipip", inComingIP);
////						intent.putExtra("number", number);
////						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
////						startActivity(intent); 
////						
////					} else if("5".equals(arrIP[2]) || "6".equals(arrIP[2])
////							|| "7".equals(arrIP[2]) || "8".equals(arrIP[2])
////							){
////						answer();
////						doorMessage = "INDOOR CALL";
////						Intent intent = new Intent(this, InComingCalls.class);
////						intent.putExtra("message", doorMessage);
////						intent.putExtra("sipip", inComingIP);
////						intent.putExtra("number", number);
////						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
////						startActivity(intent); 
////						
////					}else if ("1".equals(arrIP[2]) || "2".equals(arrIP[2])
////							|| "3".equals(arrIP[2]) || "4".equals(arrIP[2])
////							|| "9".equals(arrIP[2]) || "10".equals(arrIP[2])
////							|| "11".equals(arrIP[2]) || "12".equals(arrIP[2])
////							|| "13".equals(arrIP[2]) || "14".equals(arrIP[2])
////							|| "15".equals(arrIP[2]) || "16".equals(arrIP[2])
////							|| "17".equals(arrIP[2]) || "18".equals(arrIP[2])
////							|| "19".equals(arrIP[2]) || "20".equals(arrIP[2])) {
////						// 显示S4-P4
////						doorMessage = "NEIGHBOR";
////						Intent intent = new Intent(this,
////								InComingCallsDifferentActivity.class);   
////						intent.putExtra("sipip", inComingIP);
////						intent.putExtra("message", doorMessage);
////						intent.putExtra("number", ((which-1)*254+num)+"");
////						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
////						startActivity(intent);
////					}
////				}
////			}
//
//		}

	}

	// Add by wuhua
	private void answer() {
		Log.d("answer===============================");
		LinphoneCallParams params = LinphoneManager.getLc()
				.createDefaultCallParameters();
		if (mCall != null && mCall.getRemoteParams() != null
				&& mCall.getRemoteParams().getVideoEnabled()
				&& LinphoneManager.isInstanciated()
				&& LinphoneManager.getInstance().isAutoAcceptCamera()) {
			params.setVideoEnabled(true);
		} else {
			params.setVideoEnabled(false);
		}

		boolean isLowBandwidthConnection = !LinphoneUtils
				.isHightBandwidthConnection(this);
		if (isLowBandwidthConnection) {
			params.enableLowBandwidth(true);
			Log.d("Low bandwidth enabled in call params");
		}

		if (!LinphoneManager.getInstance().acceptCallWithParams(mCall, params)) {
			// the above method takes care of Samsung Galaxy S
			Log.d("answer===============================couldnt_accept_call");
		} else {
			Log.d("answer===============================call answered");
		}
	}

	@SuppressWarnings("deprecation")
	@SuppressLint("Wakelock")
	public void onCallStateChanged(final LinphoneCall call, final State state,
			final String message) {
		
//		sendBroadcast(new Intent("android.intent.action.WAKE_UP_SCREEN"));
		
		Log.i("zg", "LinphoneService onCallStateChanged");
		Log.i("zg", "LinphoneService onCallStateChanged state==>" + state);
		Log.i("zg", "LinphoneService onCallStateChanged message==>" + message);
		Log.i("zg",
				"LinphoneService onCallStateChanged message==>"
						+ call.getRemoteAddress());
		
		Log.d("callStateChange","state =="+state.toString());
		
		if (instance == null) {
			Log.i("Service not ready, discarding call state change to ",
					state.toString());
			return;
		}
		// if (LinphoneManager.getLcIfManagerNotDestroyedOrNull() != null) {
		// List<LinphoneCall> calls = LinphoneUtils
		// .getLinphoneCalls(LinphoneManager.getLc());
		// for (LinphoneCall callItem : calls) {
		// if (State.IncomingReceived == callItem.getState()) {
		// Add by wuhua
		mCall = call;
		// }
		// }
		// }
		if(state == State.OutgoingInit){
			screen_wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK|PowerManager.ACQUIRE_CAUSES_WAKEUP,"My Tag" );  
			screen_wakeLock.acquire();
			
			//add by jimmy at 2014.5.8   open or close the audio stream mute
			
			if(LinphoneManager.ONLYVIDEO){
				Log.d("Conversation"," ONLY VIDEO");
				if(!mAudioManager.isMicrophoneMute()){
					Log.d("Audio Manager"," open audio stream mute");
					mAudioManager.setMicrophoneMute(true);

				}
			}else{
				Log.d("Conversation"," NORMAL");
				mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVolume, 0);
				if(mAudioManager.isMicrophoneMute()){
					Log.d("Audio Manager"," close audio stream mute");
					mAudioManager.setMicrophoneMute(false);
				}
			}
			
			
		}
		if (state == State.IncomingReceived) {
			if(!sp.getBoolean("isSetFollowMe", false)){   //default value should be false after debug
				screen_wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK|PowerManager.ACQUIRE_CAUSES_WAKEUP,"My Tag" );  
				screen_wakeLock.acquire();
				
				// Update by wuhua
				// 判断是否已经设置OnlyGuardCall，只允许保安打进电话
				
				boolean onlyGuardCall = sp.getBoolean("onlyGuardCall", false);
				String inComingIPTem = mCall.getRemoteAddress().getDomain();
				String[] arrIPTem = (inComingIPTem == null) ? null : inComingIPTem
						.split("\\.");
				int forthNum = Integer.parseInt(arrIPTem[3]);
				if (onlyGuardCall) {
					Log.e("onlyGuardCall", "onlyGuardCall-->"+onlyGuardCall+"&&"+inComingIPTem);
					if (arrIPTem[2].equals("24")
							&& (forthNum >= 1 && forthNum <= 9)) {
					} else if (inComingIPTem.startsWith("10.99.26.")
							&& (forthNum >= 1 && forthNum <= 19)) {
					} else {
						LinphoneManager.getInstance().terminateCall();
						return;
					}
				}
				onIncomingReceived(state);// CYY
			}else{
				String fromSipip = call.getRemoteAddress().getDomain();
				LinphoneChatRoom mChatRoom = LinphoneManager.getLcIfManagerNotDestroyedOrNull().createChatRoom("sip:"+fromSipip);
				String followMeNumber = sp.getString("followMeNumber", "");
				Log.d("get follow me number from sharedPreference.followMe number = "+followMeNumber);
				LinphoneChatMessage mMessage = mChatRoom.createLinphoneChatMessage("FOLLOW_ME:"+followMeNumber);
				mChatRoom.sendMessage(mMessage, new StateListener() {
					
					@Override
					public void onLinphoneChatMessageStateChanged(LinphoneChatMessage msg,
							LinphoneChatMessage.State state) {
						// TODO Auto-generated method stub
						
					}
				});
				
			}
			// 保存打进电话的IP地址,用做CallBack的数据来源,此数据如果不存在,就无法拨打回拨电话
			Editor editor = sp.edit();
			editor.putString("lastIncomingCall", mCall.getRemoteAddress()
					.getDomain());
			editor.commit();
			// if(LinphoneManager.getLc().isIncall())
			// LinphoneManager.getLc().terminateCall(call);
		}
		// Add by wuhua, establish the video for monitor conversation 
//		if (state == LinphoneCall.State.Connected) {                    
//			if (mCall.getDirection() == CallDirection.Outgoing
//					&& LinphoneManager.VALUE_TRUE.equalsIgnoreCase(mCall
//							.getRemoteParams().getCustomHeader(
//									LinphoneManager.HEARDER_IS_MONITORING))) {
//
//				Intent intent = new Intent(this, ConversationActivity.class);  //changed by jimmy at 2014.5.7
//				intent.putExtra("message",
//						IntercomActivity.sendConnectedMessage);
//				Log.i("IntercomActivity.sendConnectedMessage",
//						IntercomActivity.sendConnectedMessage);
//				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//				startActivity(intent);
//			}
//		}
		if (state == State.CallUpdatedByRemote) {
			// If the correspondent proposes video while audio call
			boolean remoteVideo = call.getRemoteParams().getVideoEnabled();
			boolean localVideo = call.getCurrentParamsCopy().getVideoEnabled();
			boolean autoAcceptCameraPolicy = LinphoneManager.getInstance()
					.isAutoAcceptCamera();
			if (remoteVideo && !localVideo && !autoAcceptCameraPolicy
					&& !LinphoneManager.getLc().isInConference()) {
				try {
					LinphoneManager.getLc().deferCallUpdate(call);
				} catch (LinphoneCoreException e) {
					e.printStackTrace();
				}
			}
		}
		
		

		if (state == State.StreamsRunning) {
			// Workaround bug current call seems to be updated after state
			// changed to streams running
			if (getResources().getBoolean(R.bool.enable_call_notification))
				mWifiLock.acquire();
				
		} else {
			if (getResources().getBoolean(R.bool.enable_call_notification)) {
			}
			if ((state == State.CallEnd || state == State.Error )       
					&& LinphoneManager.getLc().getCallsNb() < 1) {
				Log.i("zg", "==================callend123======================"+message);
				sendBroadcast(new Intent("com.wits.bc.CALLEND"));
				if(LinphoneManager.getScreenState() && screen_wakeLock != null){
					if(screen_wakeLock.isHeld())
					screen_wakeLock.release();           
				}
//				mWifiLock.release();                              // wifi keep work all time for wake up system by remote call 
				
				if(mAudioManager.isMicrophoneMute()){
					mAudioManager.setStreamMute(AudioManager.STREAM_VOICE_CALL, false);
					mAudioManager.setMicrophoneMute(false);
				}
				
				mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVolume, 0);
				
			}

			mHandler.post(new Runnable() {
				public void run() {
					if (guiListener() != null)
						guiListener().onCallStateChanged(call, state, message);
				}
			});
		}
		
//		if ((state == State.CallEnd || state == State.Error)
//				&& LinphoneManager.getLc().getCallsNb() < 1) {
//			Log.i("zg", "==================callend123======================"+message);
//			sendBroadcast(new Intent("com.wits.bc.CALLEND"));
//			screen_wakeLock.release();
////			mWifiLock.release();                            // wifi keep work all time for wake up system by remote call 
//		}
		
	}

	public interface LinphoneGuiListener extends NewOutgoingCallUiListener {
		void onDisplayStatus(String message);

		void onGlobalStateChangedToOn(String message);

		void onCallStateChanged(LinphoneCall call, State state, String message);
	}

	public void changeRingtone(String ringtone) {
		Editor editor = PreferenceManager
				.getDefaultSharedPreferences(this).edit();
		editor.putString(getString(R.string.pref_audio_ringtone), ringtone);
		editor.commit();
	}

	public void onRingerPlayerCreated(MediaPlayer mRingerPlayer) {
		String uriString = PreferenceManager.getDefaultSharedPreferences(this)
				.getString(
						getString(R.string.pref_audio_ringtone),
						android.provider.Settings.System.DEFAULT_RINGTONE_URI
								.toString());
		try {
			if (uriString.startsWith("content://")) {
				mRingerPlayer.setDataSource(this, Uri.parse(uriString));
			} else {
				FileInputStream fis = new FileInputStream(uriString);
				mRingerPlayer.setDataSource(fis.getFD());
				fis.close();
			}
		} catch (IOException e) {
			Log.e(e, "Cannot set ringtone");
		}
	}

	public void tryingNewOutgoingCallButAlreadyInCall() {
		mHandler.post(new Runnable() {
			public void run() {
				if (guiListener() != null)
					guiListener().onAlreadyInCall();
			}
		});
	}

	public void tryingNewOutgoingCallButCannotGetCallParameters() {
		mHandler.post(new Runnable() {
			public void run() {
				if (guiListener() != null)
					guiListener().onCannotGetCallParameters();
			}
		});
	}

	public void tryingNewOutgoingCallButWrongDestinationAddress() {
		mHandler.post(new Runnable() {
			public void run() {
				if (guiListener() != null)
					guiListener().onWrongDestinationAddress();
			}
		});
	}

	public void onCallEncryptionChanged(final LinphoneCall call,
			final boolean encrypted, final String authenticationToken) {
		// IncallActivity registers itself to this event and handle it.
	}

	/**
	 * Add by wuhua
	 */
	public LinphoneCall getCurrentCall() {
		return mCall;
	}
	
//	private void showAlertWindow() {
//        Context applicationContext = getApplicationContext();
//
//        WindowManager manager =
//                (WindowManager) applicationContext.getSystemService(Context.WINDOW_SERVICE);
//        LayoutParams params = new LayoutParams();
//        params.type = LayoutParams.TYPE_TOAST;
//        params.format = PixelFormat.RGBA_8888;
//        params.gravity = Gravity.CENTER;
//        params.x = 0;
//        params.y = 0;
//        params.width = LayoutParams.WRAP_CONTENT;
//        params.height = LayoutParams.WRAP_CONTENT;
//
//        manager.addView(new com.wits.intercom70.NotificationView(applicationContext), params);
//    }
	
//	private class detectIndoorRingThread extends Thread {
//		@Override
//		public void run() {
//			super.run();
//			while(true){
//				try {
//					sleep(1000);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				} finally {
//					int resultIndoorRing = mLinphoneI2CInterface.IndoorButton();
//					Log.d("indoor ring"," detect indoor ring result = "+resultIndoorRing);
//					if(resultIndoorRing == 0){
//						Log.d("indoor ring"," detect indoor ring start ring and show icon");
//						mHandler.sendEmptyMessage(INDOOR_RING);
//					}
//				}
//			}
//			
//
//		}
//	}
	
}
