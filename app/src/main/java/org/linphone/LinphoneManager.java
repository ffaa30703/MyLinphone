/*
LinphoneManager.java
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

import static android.media.AudioManager.STREAM_VOICE_CALL;
import static org.linphone.core.LinphoneCall.State.CallEnd;
import static org.linphone.core.LinphoneCall.State.Error;
import static org.linphone.core.LinphoneCall.State.IncomingReceived;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.linphone.LinphoneSimpleListener.ConnectivityChangedListener;
import org.linphone.LinphoneSimpleListener.LinphoneOnAudioChangedListener;
import org.linphone.LinphoneSimpleListener.LinphoneOnAudioChangedListener.AudioState;
import org.linphone.LinphoneSimpleListener.LinphoneOnDTMFReceivedListener;
import org.linphone.LinphoneSimpleListener.LinphoneOnMessageReceivedListener;
import org.linphone.LinphoneSimpleListener.LinphoneServiceListener;
import org.linphone.compatibility.Compatibility;
import org.linphone.core.CallDirection;
import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneAuthInfo;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCall.State;
import org.linphone.core.LinphoneCallParams;
import org.linphone.core.LinphoneCallStats;
import org.linphone.core.LinphoneChatMessage;
import org.linphone.core.LinphoneChatMessage.StateListener;
import org.linphone.core.LinphoneChatRoom;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCore.EcCalibratorStatus;
import org.linphone.core.LinphoneCore.FirewallPolicy;
import org.linphone.core.LinphoneCore.GlobalState;
import org.linphone.core.LinphoneCore.MediaEncryption;
import org.linphone.core.LinphoneCore.RegistrationState;
import org.linphone.core.LinphoneCore.Transports;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.core.LinphoneCoreListener;
import org.linphone.core.LinphoneFriend;
import org.linphone.core.LinphoneProxyConfig;
import org.linphone.core.PayloadType;
import org.linphone.mediastream.Log;
import org.linphone.mediastream.Version;
import org.linphone.mediastream.video.capture.AndroidVideoApi5JniWrapper;
import org.linphone.mediastream.video.capture.hwconf.AndroidCameraConfiguration;
import org.linphone.mediastream.video.capture.hwconf.AndroidCameraConfiguration.AndroidCamera;
import org.linphone.mediastream.video.capture.hwconf.Hacks;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;

import com.keep.lin.R;
import com.wits.intercom.Constance;
import com.wits.intercom.MyApplication;
import com.wits.intercom.ReadyToCallActivity;
import com.wits.intercom.dialing.EditToCallActivity;
import com.wits.intercom.setting.SettingActivity;
import com.wits.linphone.LinphoneI2CInterface;

/**
 * 
 * Manager of the low level LibLinphone stuff.<br />
 * Including:
 * <ul>
 * <li>Starting C liblinphone</li> 开始Cliblinphone
 * <li>Reacting to C liblinphone state changes</li> 给C liblinphone状态改变做出反应
 * <li>Calling Linphone android service listener methods</li>调用
 * <li>Interacting from Android GUI/service with low level SIP stuff/</li>
 * </ul>
 * 
 * Add Service Listener to react to Linphone state changes. 添加一个服务用来监听Linphone
 * state changes
 * 
 * @author Guillaume Beraudo监听
 * 
 */
public class LinphoneManager implements LinphoneCoreListener {

	// CYY
	public static final String HEARDER_IS_MONITORING = "isMonitoring";
	public static final String HEARDER_IS_ONLYVIDEO = "onlyVideo";
	public static final String HEARDER_IS_DIP40 = "fromDip40";

	public static final String VALUE_TRUE = "true";
	public static final String VALUE_FALSE = "false";

	public static boolean ONLYVIDEO = false;

	public static Camera curCamera;

	private static LinphoneManager instance;
	private Context mServiceContext;
	private static AudioManager mAudioManager;

	private ConnectivityManager mConnectivityManager;
	private static SharedPreferences mPref;
	private static SharedPreferences devInfoSp;
	private Resources mR;
	private LinphoneCore mLc;
	private static Transports initialTransports;
	private static LinphonePreferenceManager sLPref;
	private String lastLcStatusMessage;
	private String basePath;
	private static boolean sExited;

	private static boolean threadRunning;

	private String contactParams;

	private WakeLock mIncallWakeLock;

	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothHeadset mBluetoothHeadset;
	private BluetoothProfile.ServiceListener mProfileListener;
	private BroadcastReceiver bluetoothReiceiver = new BluetoothManager();
	public boolean isBluetoothScoConnected;
	public boolean isUsingBluetoothAudioRoute;

	public ChatStorage chatStorage;

	private static Context applicationContext;
	private static int INDOOR_RING = 5;
	private static int STOP_RING = 6;
	private static int ALARM_RING = 7;
	private static int STOP_ALARM_RING = 8;
	private static int ALARM_ICON_CHANGE = 9;
	private static int SHOW_NETWORK_CONNECTTVITY = 10;

	private static int mSaveAudioMode = 0;
	private LinphoneCall mCall;

	public static WakeLock screen_wakeLock;
	private static PowerManager mPowerManager;

	public static boolean securityAvailable = true;

	public static boolean alarmIsOccuring = false;

	private static MediaPlayer mIndoorRingerPlayer;
	private static MediaPlayer mAlarmRingerPlayer;
	private static MediaPlayer mSecurityRingerPlayer;

	private int retryTimesForCall = 0;

	public static boolean isShowingInputActivity = false;
	private static MediaPlayer mCongestionRingerPlayer;
	private static MediaPlayer mBusyRingerPlayer;
	public static boolean isCongestionRinging = false;
	private boolean willCall = false;

	public static synchronized void startRingForIndoor() { // add by jimmy at
															// 2014.5.12

		// CYY
		// if (Hacks.needGalaxySAudioHack()) {
		// mSaveAudioMode = mAudioManager.getMode();
		if (mSaveAudioMode == AudioManager.MODE_NORMAL) {
			Log.d("Audio Manager.current mode = NORMAL");
		} else if (mSaveAudioMode == AudioManager.MODE_RINGTONE) {
			Log.d("Audio Manager.current mode = RING");
		} else if (mSaveAudioMode == AudioManager.MODE_IN_CALL) {
			Log.d("Audio Manager.current mode = CALL");
		} else if (mSaveAudioMode == AudioManager.MODE_IN_COMMUNICATION) {
			Log.d("Audio Manager.current mode = COMMUNICATION");
		} else if (mSaveAudioMode == AudioManager.MODE_INVALID) {
			Log.d("Audio Manager.current mode = INVALID");
		}

		// mAudioManager.setMode(MODE_RINGTONE);
		// }
		try {
			if ((mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE
					|| mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) && mVibrator != null) {
				long[] patern = { 0, 1000, 1000 };
				mVibrator.vibrate(patern, 1);
			}
			if (mIndoorRingerPlayer == null) {
				mIndoorRingerPlayer = new MediaPlayer();
				// mRingerPlayer.setVolume(5, 5);
				// mRingerPlayer.setAudioStreamType(STREAM_RING);

				// mListenerDispatcher.onRingerPlayerCreated(mRingerPlayer);

				String indoorRingPath = devInfoSp.getString("indoorLevel", "2");
				switch (Integer.parseInt(indoorRingPath)) {
				case 1:
					mIndoorRingerPlayer.setDataSource(mRing_1);
					break;
				case 2:
					mIndoorRingerPlayer.setDataSource(mRing_2);
					break;
				case 3:
					mIndoorRingerPlayer.setDataSource(mRing_3);
					break;
				case 4:
					mRingerPlayer.setDataSource(mRing_4);
					break;
				case 5:
					mIndoorRingerPlayer.setDataSource(mRing_5);
					break;
				case 6:
					mIndoorRingerPlayer.setDataSource(mRingbackSoundFile);
					break;
				case 7:
					mIndoorRingerPlayer.setDataSource(mRing_7);
					break;
				case 8:
					mIndoorRingerPlayer.setDataSource(mRing_8);
					break;
				case 9:
					mIndoorRingerPlayer.setDataSource(mRing_9);
					break;

				default:
					mIndoorRingerPlayer.setDataSource(mRing_2);
					break;
				}

				mIndoorRingerPlayer.prepare();
				mIndoorRingerPlayer.setLooping(true);
				mIndoorRingerPlayer.start();
				if (devInfoSp.getBoolean("DISTURBE", false)) {
					Thread.sleep(2000);
					stopRingForIndoor();
				}

			} else {
				Log.w("already ringing");
			}
		} catch (Exception e) {
			Log.e(e, "cannot handle incoming call");
		}

	}

	public static synchronized void stopRingForIndoor() {
		if (mRingerPlayer != null) {
			mIndoorRingerPlayer.pause(); // add by jimmy at 2014.5.6
			mIndoorRingerPlayer.stop();
			mIndoorRingerPlayer.release();
			mIndoorRingerPlayer = null;
			Log.d("Ring", "stop ring success");
		}
		if (mVibrator != null) {
			mVibrator.cancel();
		}
		// mAudioManager.setMode(mSaveAudioMode);
	}

	public static synchronized void startRingForAlarm() { // add by jimmy at
															// 2014.5.20
		isWakeupRinging = true;
		// mSaveAudioMode = mAudioManager.getMode();
		// mAudioManager.setMode(MODE_RINGTONE);
		// }
		try {
			if ((mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE
					|| mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) && mVibrator != null) {
				long[] patern = { 0, 1000, 1000 };
				mVibrator.vibrate(patern, 1);
			}
			if (mAlarmRingerPlayer == null) {
				mAlarmRingerPlayer = new MediaPlayer();
				// mRingerPlayer.setVolume(5, 5);
				// mRingerPlayer.setAudioStreamType(STREAM_RING);

				mAlarmRingerPlayer.setDataSource(mRingbackSoundFile);

				mAlarmRingerPlayer.prepare();
				mAlarmRingerPlayer.setLooping(true);
				mAlarmRingerPlayer.start();
				if (devInfoSp.getBoolean("DISTURBE", false)) {
					Thread.sleep(2000);
					stopRingForAlarm();
				}

			} else {
				Log.w("already ringing");
			}
		} catch (Exception e) {
			Log.e(e, "cannot handle incoming call");
		}

	}

	public static synchronized void stopRingForAlarm() {
		if (mAlarmRingerPlayer != null) {
			mAlarmRingerPlayer.pause(); // add by jimmy at 2014.5.6
			mAlarmRingerPlayer.stop();
			mAlarmRingerPlayer.release();
			mAlarmRingerPlayer = null;
			Log.d("Ring", "stop ring success");
		}
		if (mVibrator != null) {
			mVibrator.cancel();
		}
		// mAudioManager.setMode(mSaveAudioMode);
		isWakeupRinging = false;
	}

	public static synchronized void startRingForBusy() { // add by jimmy at
															// 2014.5.20
		// mSaveAudioMode = mAudioManager.getMode();
		// mAudioManager.setMode(MODE_RINGTONE);
		// }
		try {
			if ((mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE
					|| mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) && mVibrator != null) {
				long[] patern = { 0, 1000, 1000 };
				mVibrator.vibrate(patern, 1);
			}
			if (mBusyRingerPlayer == null) {
				mBusyRingerPlayer = new MediaPlayer();

				mBusyRingerPlayer.setDataSource("/mnt/sdcard/Ringtones" + "/busy_tone.wav");

				mBusyRingerPlayer.prepare();
				mBusyRingerPlayer.setLooping(true);
				mBusyRingerPlayer.start();
				if (devInfoSp.getBoolean("DISTURBE", false)) {
					Thread.sleep(2000);
					stopRingForAlarm();
				}

			} else {
				Log.w("already ringing");
			}
		} catch (Exception e) {
			Log.e(e, "cannot handle incoming call");
		}

	}

	public static synchronized void stopRingForBusy() {
		if (mBusyRingerPlayer != null) {
			mBusyRingerPlayer.pause(); // add by jimmy at 2014.5.6
			mBusyRingerPlayer.stop();
			mBusyRingerPlayer.release();
			mBusyRingerPlayer = null;
			Log.d("Ring", "stop ring success");
		}
		if (mVibrator != null) {
			mVibrator.cancel();
		}
		// mAudioManager.setMode(mSaveAudioMode);
	}

	public static synchronized void startRingForCongestion() { // add by jimmy
																// at 2014.5.20
		// mSaveAudioMode = mAudioManager.getMode();
		// mAudioManager.setMode(MODE_RINGTONE);
		// }
		try {
			if ((mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE
					|| mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) && mVibrator != null) {
				long[] patern = { 0, 1000, 1000 };
				mVibrator.vibrate(patern, 1);
			}
			if (mCongestionRingerPlayer == null) {
				mCongestionRingerPlayer = new MediaPlayer();

				mCongestionRingerPlayer.setDataSource("/mnt/sdcard/Ringtones" + "/congestion_tone.wav");

				mCongestionRingerPlayer.prepare();
				mCongestionRingerPlayer.setLooping(true);
				mCongestionRingerPlayer.start();
				isCongestionRinging = true;
				if (devInfoSp.getBoolean("DISTURBE", false)) {
					Thread.sleep(2000);
					stopRingForAlarm();
				}

			} else {
				Log.w("already ringing");
			}
		} catch (Exception e) {
		}

	}

	public static synchronized void stopRingForCongestion() {
		if (mCongestionRingerPlayer != null) {
			mCongestionRingerPlayer.pause(); // add by jimmy at 2014.5.6
			mCongestionRingerPlayer.stop();
			mCongestionRingerPlayer.release();
			mCongestionRingerPlayer = null;
			isCongestionRinging = false;
			Log.d("Ring", "stop ring success");
		}
		if (mVibrator != null) {
			mVibrator.cancel();
		}
		// mAudioManager.setMode(mSaveAudioMode);
	}

	public static synchronized void startRingForSecurity(int whichSong, long time) { // add
																						// by
																						// jimmy
																						// at
																						// 2014.5.20
		// mSaveAudioMode = mAudioManager.getMode();
		// mAudioManager.setMode(MODE_RINGTONE);
		// }
		try {
			if ((mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE
					|| mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) && mVibrator != null) {
				long[] patern = { 0, 1000, 1000 };
				mVibrator.vibrate(patern, 1);
			}
			if (mSecurityRingerPlayer == null) {
				mSecurityRingerPlayer = new MediaPlayer();
				// mRingerPlayer.setVolume(5, 5);
				// mRingerPlayer.setAudioStreamType(STREAM_RING);
				switch (whichSong) {
				case 10:
					mSecurityRingerPlayer.setDataSource(mRing_10);
					break;
				case 11:
					mHandler.sendEmptyMessageDelayed(STOP_ALARM_RING, time);
					mSecurityRingerPlayer.setDataSource(mRing_11);
					break;
				case 12:
					mSecurityRingerPlayer.setDataSource(mRing_12);
					break;
				default:
					break;
				}
				mSecurityRingerPlayer.prepare();
				mSecurityRingerPlayer.setLooping(true);
				mSecurityRingerPlayer.start();
				if (devInfoSp.getBoolean("DISTURBE", false)) {
					Thread.sleep(2000);
					stopRingForAlarm();
				}

			} else {
				Log.w("already ringing");
			}
		} catch (Exception e) {
		}

	}

	public static synchronized void stopRingForSecurity() {
		if (mSecurityRingerPlayer != null) {
			mSecurityRingerPlayer.pause(); // add by jimmy at 2014.5.6
			mSecurityRingerPlayer.stop();
			mSecurityRingerPlayer.release();
			mSecurityRingerPlayer = null;
			Log.d("Ring", "stop ring success");
		}
		if (mVibrator != null) {
			mVibrator.cancel();
		}
		// mAudioManager.setMode(mSaveAudioMode);
	}

	public static Handler mHandler;

	private static LinphoneI2CInterface mLinphoneI2CInterface;

	public static void setCamera(Camera camera) {
		curCamera = camera;
	}

	public static Camera getCamera() {
		return curCamera;
	}

	public static int[] saveTemperature = new int[1];
	public static boolean setTimeByI2c = false;

	public static void setTemperature(int[] temp) {
		saveTemperature = temp;
	}

	public static int[] getTemperature() {
		return saveTemperature;
	}

	public static void setTimeChangeFlag(boolean byI2c) {
		setTimeByI2c = byI2c;
	}

	public static boolean getTimeChangeFlag() {
		return setTimeByI2c;
	}

	private static List<LinphoneSimpleListener> simpleListeners = new ArrayList<LinphoneSimpleListener>();

	public static void addListener(LinphoneSimpleListener listener) {
		android.util.Log.w("zg", "=====LinphoneManager===addListener====");
		if (!simpleListeners.contains(listener)) {
			simpleListeners.add(listener);
		}
	}

	public static void removeListener(LinphoneSimpleListener listener) {
		android.util.Log.w("zg", "=====LinphoneManager===removeListener====");
		simpleListeners.remove(listener);
	}

	protected LinphoneManager(final Context c, LinphoneServiceListener listener) {
		android.util.Log.w("zg", "=====LinphoneManager===LinphoneManager==start==Context==>" + c
				+ "LinphoneServiceListener==>" + listener);
		sExited = false;
		mServiceContext = c;
		mListenerDispatcher = new ListenerDispatcher(listener);

		basePath = c.getFilesDir().getAbsolutePath();
		android.util.Log.w("zg", "LinphoneManager====LinphoneManager====basePath==>" + basePath);
		mLPConfigXsd = basePath + "/lpconfig.xsd";
		mLinphoneInitialConfigFile = basePath + "/linphonerc";
		mLinphoneConfigFile = basePath + "/.linphonerc";
		mLinphoneRootCaFile = basePath + "/rootca.pem";
		mRingSoundFile = "/mnt/sdcard/Ringtones" + "/oldphone_mono.wav";
		mRingbackSoundFile = "/mnt/sdcard/Ringtones" + "/playback.mp3";
		mPauseSoundFile = "/mnt/sdcard/Ringtones" + "/toy_mono.wav";

		// mRing_1 = basePath+"/ring_1.wav";
		// mRing_2 = basePath+"/ring_2.wav";
		// mRing_3 = basePath+"/ring_3.wav";
		// mRing_4 = basePath+"/ring_4.wav";
		// mRing_5 = basePath+"/ring_5.wav";
		// mRing_7 = basePath+"/ring_7.wav";
		// mRing_8 = basePath+"/ring_8.wav";
		// mRing_9 = basePath+"/ring_9.wav";
		// mRing_10 = basePath+"/ring_10.mp3";
		// mRing_11 = basePath+"/ring_11.mp3";
		// mRing_12 = basePath+"/ring_12.mp3";
		mRing_1 = "/mnt/sdcard/Ringtones" + "/ring_1.wav";
		mRing_2 = "/mnt/sdcard/Ringtones" + "/ring_2.wav";
		mRing_3 = "/mnt/sdcard/Ringtones" + "/ring_3.wav";
		mRing_4 = "/mnt/sdcard/Ringtones" + "/ring_4.wav";
		mRing_5 = "/mnt/sdcard/Ringtones" + "/ring_5.wav";
		mRing_7 = "/mnt/sdcard/Ringtones" + "/ring_7.wav";
		mRing_8 = "/mnt/sdcard/Ringtones" + "/ring_8.wav";
		mRing_9 = "/mnt/sdcard/Ringtones" + "/ring_9.wav";
		mRing_10 = "/mnt/sdcard/Ringtones" + "/ring_10.mp3";
		mRing_11 = "/mnt/sdcard/Ringtones" + "/ring_11.mp3";
		mRing_12 = "/mnt/sdcard/Ringtones" + "/ring_12.mp3";

		sLPref = LinphonePreferenceManager.getInstance(c);
		android.util.Log.w("zg", "LinphoneManager====LinphoneManager====sLPref==>" + sLPref);
		// 得到AudioManager
		mAudioManager = ((AudioManager) c.getSystemService(Context.AUDIO_SERVICE));
		// 得到Vibrator
		mVibrator = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);

		// 得到存储设备铃音的sharepreference
		devInfoSp = c.getSharedPreferences("deviceInfo", 0);
		String curRingVolumeString = devInfoSp.getString("ringLevel", "5");

		int curVolume = Integer.parseInt(curRingVolumeString);
		int maxRingVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM); // add
																							// by
																							// jimmy
																							// at
																							// 2014.05.15
		mAudioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, curVolume, 0);

		mPref = PreferenceManager.getDefaultSharedPreferences(c);

		mPowerManager = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
		mConnectivityManager = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
		mR = c.getResources();

		chatStorage = new ChatStorage(mServiceContext);
		android.util.Log.w("zg", "LinphoneManager====LinphoneManager====end==");

	}

	private static final int LINPHONE_VOLUME_STREAM = STREAM_VOICE_CALL;
	private static final int dbStep = 4;
	public static boolean isWakeupRinging = false;
	/** Called when the activity is first created. */
	private final String mLPConfigXsd;
	private final String mLinphoneInitialConfigFile;
	private final String mLinphoneRootCaFile;
	private final String mLinphoneConfigFile;
	private static String mRingSoundFile;
	private static String mRingbackSoundFile;
	private static String mPauseSoundFile;

	private static String mRing_1;
	private static String mRing_2;
	private static String mRing_3;
	private static String mRing_4;
	private static String mRing_5;
	private static String mRing_7;
	private static String mRing_8;
	private static String mRing_9;
	private static String mRing_10;
	private static String mRing_11;
	private static String mRing_12;
	private Timer mTimer = new Timer("Linphone scheduler");

	private BroadcastReceiver mKeepAliveReceiver = new KeepAliveReceiver();

	/*
	 * 设置是通过蓝牙，听筒，话筒出声音
	 */
	private void routeAudioToSpeakerHelper(boolean speakerOn) {
		// 设置不用蓝牙
		isUsingBluetoothAudioRoute = false;
		if (mAudioManager != null) {
			Compatibility.setAudioManagerInCallMode(mAudioManager);
			mAudioManager.stopBluetoothSco();
			mAudioManager.setBluetoothScoOn(false);
		}

		if (!speakerOn) {
			mLc.enableSpeaker(false);
		} else {
			mLc.enableSpeaker(true);
		}

		for (LinphoneOnAudioChangedListener listener : getSimpleListeners(LinphoneOnAudioChangedListener.class)) {
			listener.onAudioStateChanged(speakerOn ? AudioState.SPEAKER : AudioState.EARPIECE);
		}
	}

	/**
	 * 设置由话筒出声音
	 */
	public void routeAudioToSpeaker() {
		routeAudioToSpeakerHelper(true);
	}

	/**
	 * 获得用户代理
	 * 
	 * @return
	 * @throws NameNotFoundException
	 */
	public String getUserAgent() throws NameNotFoundException {
		StringBuilder userAgent = new StringBuilder();
		userAgent.append("LinphoneAndroid/"
				+ mServiceContext.getPackageManager().getPackageInfo(mServiceContext.getPackageName(), 0).versionCode);
		userAgent.append(" (");
		userAgent.append("Linphone/" + LinphoneManager.getLc().getVersion() + "; ");
		userAgent.append(Build.DEVICE + " " + Build.MODEL + " Android/" + Build.VERSION.SDK_INT);
		userAgent.append(")");
		return userAgent.toString();
	}

	/**
	 * 设置由听筒出声
	 */
	public void routeAudioToReceiver() {
		routeAudioToSpeakerHelper(false);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@SuppressWarnings("deprecation")
	// 启动蓝牙
	public void startBluetooth() {
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter.isEnabled()) {
			if (Version.sdkAboveOrEqual(Version.API11_HONEYCOMB_30)) {
				mProfileListener = new BluetoothProfile.ServiceListener() {
					@TargetApi(Build.VERSION_CODES.HONEYCOMB)
					public void onServiceConnected(int profile, BluetoothProfile proxy) {
						if (profile == BluetoothProfile.HEADSET) {
							mBluetoothHeadset = (BluetoothHeadset) proxy;
							Log.d("Bluetooth headset connected");
						}
					}

					@TargetApi(Build.VERSION_CODES.HONEYCOMB)
					public void onServiceDisconnected(int profile) {
						if (profile == BluetoothProfile.HEADSET) {
							mBluetoothHeadset = null;
							Log.d("Bluetooth headset disconnected");
							isBluetoothScoConnected = false;
							// routeAudioToReceiver();
						}
					}
				};
				mBluetoothAdapter.getProfileProxy(mServiceContext, mProfileListener, BluetoothProfile.HEADSET);
			} else {
				try {
					mServiceContext.unregisterReceiver(bluetoothReiceiver);
				} catch (Exception e) {
				}

				Intent currentValue = mServiceContext.registerReceiver(bluetoothReiceiver,
						new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_CHANGED));
				int state = currentValue == null ? 0 : currentValue.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, 0);
				if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
					isBluetoothScoConnected = true;
				}
			}
		} else {
			isBluetoothScoConnected = false;
			scoDisconnected();
			// routeAudioToReceiver();
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public boolean routeAudioToBluetooth() {
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter.isEnabled() && mAudioManager.isBluetoothScoAvailableOffCall()) {
			mAudioManager.setBluetoothScoOn(true);
			mAudioManager.startBluetoothSco();

			if (Version.sdkAboveOrEqual(Version.API11_HONEYCOMB_30)) {
				isUsingBluetoothAudioRoute = false;
				if (mBluetoothHeadset != null) {
					List<BluetoothDevice> devices = mBluetoothHeadset.getConnectedDevices();
					for (final BluetoothDevice dev : devices) {
						isUsingBluetoothAudioRoute |= mBluetoothHeadset
								.getConnectionState(dev) == BluetoothHeadset.STATE_CONNECTED;
					}
				}

				if (!isUsingBluetoothAudioRoute) {
					Log.d("No bluetooth device available");
					scoDisconnected();
				} else {
					mAudioManager.setMode(AudioManager.MODE_IN_CALL);
					for (LinphoneOnAudioChangedListener listener : getSimpleListeners(
							LinphoneOnAudioChangedListener.class)) {
						listener.onAudioStateChanged(AudioState.SPEAKER);
					}
				}
			}
			return isUsingBluetoothAudioRoute;
		}

		return false;
	}

	public void scoConnected() {
		Log.e("Bluetooth sco connected!");
		isBluetoothScoConnected = true;
	}

	public void scoDisconnected() {
		Log.e("Bluetooth sco disconnected!");
		isUsingBluetoothAudioRoute = false;
		isBluetoothScoConnected = false;
		if (mAudioManager != null) {
			mAudioManager.setMode(AudioManager.MODE_NORMAL);
			mAudioManager.stopBluetoothSco();
			mAudioManager.setBluetoothScoOn(false);
		}
	}

	/**
	 * 创建并启动LinphoneManager,由LinphoneService中创建
	 * 
	 * @param c
	 * @param listener
	 * @return
	 */
	public synchronized static final LinphoneManager createAndStart(final Context c, LinphoneServiceListener listener) {
		android.util.Log.w(Constance.LOGTAG, "LinphoneManager createAndStart");
		if (instance != null)
			throw new RuntimeException("Linphone Manager is already initialized");

		instance = new LinphoneManager(c, listener);
		instance.startLibLinphone(c);
		TelephonyManager tm = (TelephonyManager) c.getSystemService(Context.TELEPHONY_SERVICE);
		boolean gsmIdle = tm.getCallState() == TelephonyManager.CALL_STATE_IDLE;
		setGsmIdle(gsmIdle);

		if (Version.isVideoCapable()) {

			Log.d("Version", "sdk version has video capable,sdk version" + Version.sdk());

			AndroidVideoApi5JniWrapper.setAndroidSdkVersion(Version.sdk());
		} else {
			Log.d("Version", "sdk version no video capable,sdk version" + Version.sdk());
		}

		mLinphoneI2CInterface = new LinphoneI2CInterface();

		mHandler = new Handler() {

			public void dispatchMessage(android.os.Message msg) {

				if (msg.what == INDOOR_RING) {
					// screen_wakeLock =
					// mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK|PowerManager.ACQUIRE_CAUSES_WAKEUP,"My
					// Tag" );
					// if(!ScreenState){
					// screen_wakeLock.acquire();
					// }
					// showAlertWindow();
					startRingForIndoor();
					mHandler.sendEmptyMessageDelayed(STOP_RING, 3000);
				} else if (msg.what == STOP_RING) {

					stopRingForIndoor();

				} else if (msg.what == ALARM_RING) {
					screen_wakeLock = mPowerManager.newWakeLock(
							PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "My Tag");
					if (!ScreenState) {
						screen_wakeLock.acquire();
					}
					startRingForSecurity(12, 0);
					// mHandler.sendEmptyMessageDelayed(STOP_ALARM_RING,
					// 120000);
				} else if (msg.what == STOP_ALARM_RING) {
					stopRingForSecurity();
				} else if (msg.what == ALARM_ICON_CHANGE) {
					startRingForSecurity(11, 2000);
				} else if (msg.what == SHOW_NETWORK_CONNECTTVITY) {
					MyApplication.getInstnce().close();
					showNetworkWindow();
				}
			}
		};

		applicationContext = c;

		threadRunning = true;

		Thread mThread = new Thread() {
			@Override
			public void run() {
				super.run();
				while (threadRunning) {
					try {
						sleep(1000);
						// int resultGetWakeup =
						// mLinphoneI2CInterface.GetWakeup(); //sense a visitor
						// coming
						// Log.d("LinphoneManager","GetWakeup result =
						// "+resultGetWakeup);
						// if(isInMainPage){
						// Log.d("LinphoneManager","current page is main page");
						// }
						// if(resultGetWakeup == 0 && isInMainPage){
						// Log.d("LinphoneManager","detected visitor coming");
						// Intent intent = new
						// Intent(c,ReadyToCallActivity.class);
						// intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						// c.startActivity(intent);
						// }
						int resultEnterSetting = mLinphoneI2CInterface.GetSetupDoorpanel(); // enter
																							// setting
																							// page
						// Log.d("LinphoneManager","GetSetupDoorpanel result =
						// "+resultEnterSetting);
						if (resultEnterSetting == 0) {
							isInSetting = true;
							Log.d("LinphoneManager", "enter setting page");
							Intent intent = new Intent(c, SettingActivity.class);
							intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							c.startActivity(intent);

						}

					} catch (InterruptedException e) {
						e.printStackTrace();
					} finally {

					}
				}

			}
		};
		mThread.start();

		return instance;
	}

	public static void indoorRingTask() {
		// screen_wakeLock =
		// mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK|PowerManager.ACQUIRE_CAUSES_WAKEUP,"My
		// Tag" );
		// screen_wakeLock.acquire();
		// showAlertWindow();
		startRingForIndoor();
		mHandler.sendEmptyMessageDelayed(STOP_RING, 3000);
	}

	public static synchronized final LinphoneManager getInstance() {
		if (instance != null)
			return instance;

		if (sExited) {
			throw new RuntimeException("Linphone Manager was already destroyed. "
					+ "Better use getLcIfManagerNotDestroyed and check returned value");
		}

		throw new RuntimeException("Linphone Manager should be created before accessed");
	}

	public static synchronized final LinphoneCore getLc() {
		return getInstance().mLc;
	}

	public String getLPConfigXsdPath() {
		return mLPConfigXsd;
	}

	public void newOutgoingCallString(final String address, final boolean isMonitoring) {

		ONLYVIDEO = false;

		String to = address;

		// if (mLc.isIncall()) {
		// listenerDispatcher.tryingNewOutgoingCallButAlreadyInCall();
		// return;
		// }
		Log.e("LinphoneManager:newOutgoingCallString", "address:" + address);
		Log.e("LinphoneManager:newOutgoingCallString", "isMonitoring:" + isMonitoring);

		LinphoneAddress lAddress;
		try {
			lAddress = mLc.interpretUrl(to);
			//
			Log.e("LinphoneManager:newOutgoingCallString", "lAddress:" + lAddress);
			//
			Log.e("Lm====", "mServiceContext:"
					+ mServiceContext.getResources().getBoolean(R.bool.override_domain_using_default_one));
			if (mServiceContext.getResources().getBoolean(R.bool.override_domain_using_default_one)) {
				lAddress.setDomain(mServiceContext.getString(R.string.default_domain));
			}
			LinphoneProxyConfig lpc = mLc.getDefaultProxyConfig();

			if (mR.getBoolean(R.bool.forbid_self_call) && lpc != null
					&& lAddress.asStringUriOnly().equals(lpc.getIdentity())) {
				mListenerDispatcher.tryingNewOutgoingCallButWrongDestinationAddress();
				return;
			}
		} catch (LinphoneCoreException e) {
			mListenerDispatcher.tryingNewOutgoingCallButWrongDestinationAddress();
			return;
		}
		lAddress.setDisplayName(address);

		// 调用 是否是高带宽速率连接
		boolean isLowBandwidthConnection = !LinphoneUtils
				.isHightBandwidthConnection(LinphoneService.instance().getApplicationContext());

		//
		Log.e("Lm======", "isHightBandwidthConnection:----->" + !isLowBandwidthConnection);
		Log.e("Lm======", "mLc.isNetworkReachable():---->" + mLc.isNetworkReachable());
		Log.e("Lm======", "LinphoneActivity.isInstanciated():------>" + LinphoneActivity.isInstanciated());

		retryTimesForCall++;
		// 加入是否存在网络连接
		if (getActiveNetwork(getContext()) == null) { // removed
														// mLc.isNetworkReachable()
														// by jimmy at
														// 2014.10.14,
														// mLc.isNetworkReachable()
														// maybe judge error
														// sometimes
			Log.e("Lm=====", "要求重新发送网络状态广播!");
			getContext().sendBroadcast(new Intent("android.net.conn.RESTART_CONNECTIVITY_CHANGE"));
			if (retryTimesForCall < 10) {
				newOutgoingCallString(address, isMonitoring);
			} else {
				mHandler.sendEmptyMessage(SHOW_NETWORK_CONNECTTVITY);
			}
			/*
			 * new Thread(new Runnable() {
			 * 
			 * @Override public void run() {
			 * 
			 * try { Thread.sleep(1000); } catch (InterruptedException e) {
			 * e.printStackTrace(); }
			 * newOutgoingCallString(address,isMonitoring); } }).start();
			 */

			return;
		}

		retryTimesForCall = 0;

		if (getActiveNetwork(getContext()) != null) { // removed
														// mLc.isNetworkReachable()
														// by jimmy at
														// 2014.10.14
			try {
				if (Version.isVideoCapable()) {
					boolean prefVideoEnable = isVideoEnabled();
					int key = R.string.pref_video_initiate_call_with_video_key;
					boolean prefInitiateWithVideo = getPrefBoolean(key, false);
					CallManager.getInstance().inviteAddress(lAddress, prefVideoEnable && prefInitiateWithVideo,
							isLowBandwidthConnection, isMonitoring);
				} else {
					CallManager.getInstance().inviteAddress(lAddress, false, isLowBandwidthConnection, isMonitoring);
				}

			} catch (LinphoneCoreException e) {
				mListenerDispatcher.tryingNewOutgoingCallButCannotGetCallParameters();
				return;
			}
		} else if (LinphoneActivity.isInstanciated()) {
			LinphoneActivity.instance().displayCustomToast(getString(R.string.error_network_unreachable),
					Toast.LENGTH_LONG);
		} else {
			Log.e("Error: " + getString(R.string.error_network_unreachable));
		}
	}

	// only video conversation
	public void newOutgoingCallStringOnlyVideo(final String address, final boolean isMonitoring,
			final boolean isFromDip40) {

		ONLYVIDEO = true;

		String to = address;

		// if (mLc.isIncall()) {
		// listenerDispatcher.tryingNewOutgoingCallButAlreadyInCall();
		// return;
		// }
		Log.e("LinphoneManager:newOutgoingCallString", "address:" + address);
		Log.e("LinphoneManager:newOutgoingCallString", "isMonitoring:" + isMonitoring);

		LinphoneAddress lAddress;
		try {
			lAddress = mLc.interpretUrl(to);
			//
			Log.e("LinphoneManager:newOutgoingCallString", "lAddress:" + lAddress);
			//
			Log.e("Lm====", "mServiceContext:"
					+ mServiceContext.getResources().getBoolean(R.bool.override_domain_using_default_one));
			if (mServiceContext.getResources().getBoolean(R.bool.override_domain_using_default_one)) {
				lAddress.setDomain(mServiceContext.getString(R.string.default_domain));
			}
			LinphoneProxyConfig lpc = mLc.getDefaultProxyConfig();

			if (mR.getBoolean(R.bool.forbid_self_call) && lpc != null
					&& lAddress.asStringUriOnly().equals(lpc.getIdentity())) {
				mListenerDispatcher.tryingNewOutgoingCallButWrongDestinationAddress();
				return;
			}
		} catch (LinphoneCoreException e) {
			mListenerDispatcher.tryingNewOutgoingCallButWrongDestinationAddress();
			return;
		}
		lAddress.setDisplayName(address);

		// 调用 是否是高带宽速率连接
		boolean isLowBandwidthConnection = !LinphoneUtils
				.isHightBandwidthConnection(LinphoneService.instance().getApplicationContext());

		//
		Log.e("Lm======", "isHightBandwidthConnection:----->" + !isLowBandwidthConnection);
		Log.e("Lm======", "mLc.isNetworkReachable():---->" + mLc.isNetworkReachable());
		Log.e("Lm======", "LinphoneActivity.isInstanciated():------>" + LinphoneActivity.isInstanciated());
		retryTimesForCall++;
		// 加入是否存在网络连接
		if (getActiveNetwork(getContext()) == null) {
			Log.e("Lm=====", "要求重新发送网络状态广播!");
			getContext().sendBroadcast(new Intent("android.net.conn.RESTART_CONNECTIVITY_CHANGE"));
			if (retryTimesForCall < 10) {
				newOutgoingCallStringOnlyVideo(address, isMonitoring, isFromDip40);
			} else {
				mHandler.sendEmptyMessage(SHOW_NETWORK_CONNECTTVITY);
			}
			/*
			 * new Thread(new Runnable() {
			 * 
			 * @Override public void run() {
			 * 
			 * try { Thread.sleep(1000); } catch (InterruptedException e) {
			 * e.printStackTrace(); }
			 * newOutgoingCallString(address,isMonitoring); } }).start();
			 */

			return;
		}
		retryTimesForCall = 0;
		if (getActiveNetwork(getContext()) != null) {
			try {
				if (Version.isVideoCapable()) {
					boolean prefVideoEnable = isVideoEnabled();
					int key = R.string.pref_video_initiate_call_with_video_key;
					boolean prefInitiateWithVideo = getPrefBoolean(key, false);
					CallManager.getInstance().inviteAddress(lAddress, prefVideoEnable && prefInitiateWithVideo,
							isLowBandwidthConnection, isMonitoring, isFromDip40);
				} else {
					CallManager.getInstance().inviteAddress(lAddress, false, isLowBandwidthConnection, isMonitoring,
							isFromDip40);
				}

			} catch (LinphoneCoreException e) {
				mListenerDispatcher.tryingNewOutgoingCallButCannotGetCallParameters();
				return;
			}
		} else if (LinphoneActivity.isInstanciated()) {
			LinphoneActivity.instance().displayCustomToast(getString(R.string.error_network_unreachable),
					Toast.LENGTH_LONG);
		} else {
			Log.e("Error: " + getString(R.string.error_network_unreachable));
		}
	}

	// 不带监听选项的newOutgoingCall
	public void newOutgoingCall(AddressType address) {

		ONLYVIDEO = false;

		String to = address.getText().toString();

		// if (mLc.isIncall()) {
		// listenerDispatcher.tryingNewOutgoingCallButAlreadyInCall();
		// return;
		// }
		LinphoneAddress lAddress;
		try {
			lAddress = mLc.interpretUrl(to);
			if (mServiceContext.getResources().getBoolean(R.bool.override_domain_using_default_one)) {
				lAddress.setDomain(mServiceContext.getString(R.string.default_domain));
			}
			LinphoneProxyConfig lpc = mLc.getDefaultProxyConfig();

			if (mR.getBoolean(R.bool.forbid_self_call) && lpc != null
					&& lAddress.asStringUriOnly().equals(lpc.getIdentity())) {
				mListenerDispatcher.tryingNewOutgoingCallButWrongDestinationAddress();
				return;
			}
		} catch (LinphoneCoreException e) {
			mListenerDispatcher.tryingNewOutgoingCallButWrongDestinationAddress();
			return;
		}
		lAddress.setDisplayName(address.getDisplayedName());

		// 调用 是否是高带宽速率连接
		boolean isLowBandwidthConnection = !LinphoneUtils
				.isHightBandwidthConnection(LinphoneService.instance().getApplicationContext());

		if (mLc.isNetworkReachable()) {
			try {
				if (Version.isVideoCapable()) {
					boolean prefVideoEnable = isVideoEnabled();
					int key = R.string.pref_video_initiate_call_with_video_key;
					boolean prefInitiateWithVideo = getPrefBoolean(key, false);
					CallManager.getInstance().inviteAddress(lAddress, prefVideoEnable && prefInitiateWithVideo,
							isLowBandwidthConnection, false);
				} else {
					CallManager.getInstance().inviteAddress(lAddress, false, isLowBandwidthConnection, false);
				}

			} catch (LinphoneCoreException e) {
				mListenerDispatcher.tryingNewOutgoingCallButCannotGetCallParameters();
				return;
			}
		} else if (LinphoneActivity.isInstanciated()) {
			LinphoneActivity.instance().displayCustomToast(getString(R.string.error_network_unreachable),
					Toast.LENGTH_LONG);
		} else {
			Log.e("Error: " + getString(R.string.error_network_unreachable));
		}
	}

	private void resetCameraFromPreferences() {
		boolean useFrontCam = getPrefBoolean(R.string.pref_video_use_front_camera_key,
				mR.getBoolean(R.bool.pref_video_use_front_camera_default));

		int camId = 0;
		AndroidCamera[] cameras = AndroidCameraConfiguration.retrieveCameras();
		for (AndroidCamera androidCamera : cameras) {
			if (androidCamera.frontFacing == useFrontCam)
				camId = androidCamera.id;
		}
		LinphoneManager.getLc().setVideoDevice(camId);
	}

	public static interface AddressType {
		void setText(CharSequence s);

		CharSequence getText();

		void setDisplayedName(String s);

		String getDisplayedName();
	}

	public static interface NewOutgoingCallUiListener {
		public void onWrongDestinationAddress();

		public void onCannotGetCallParameters();

		public void onAlreadyInCall();
	}

	public boolean toggleEnableCamera() {
		if (mLc.isIncall()) {
			boolean enabled = !mLc.getCurrentCall().cameraEnabled();
			enableCamera(mLc.getCurrentCall(), enabled);
			return enabled;
		}
		return false;
	}

	public void enableCamera(LinphoneCall call, boolean enable) {
		if (call != null) {
			call.enableCamera(enable);
			// if
			// (mServiceContext.getResources().getBoolean(R.bool.enable_call_notification))
			// LinphoneService.instance().refreshIncallIcon(mLc.getCurrentCall());
		}
	}

	public void sendStaticImage(boolean send) {
		if (mLc.isIncall()) {
			enableCamera(mLc.getCurrentCall(), !send);
		}
	}

	public void playDtmf(ContentResolver r, char dtmf) {
		try {
			if (Settings.System.getInt(r, Settings.System.DTMF_TONE_WHEN_DIALING) == 0) {
				// audible touch disabled: don't play on speaker, only send in
				// outgoing stream
				return;
			}
		} catch (SettingNotFoundException e) {
		}

		getLc().playDtmf(dtmf, -1);
	}

	public void changeResolution() {
		BandwidthManager manager = BandwidthManager.getInstance();
		manager.setUserRestriction(!manager.isUserRestriction());
	}

	public void terminateCall() {
		if (mLc.isIncall()) {
			mLc.terminateCall(mLc.getCurrentCall());
		}
	}

	public State getCurrentCallState() {
		// return LinphoneCall.getState();
		if (mLc.isIncall()) {
			return mLc.getCurrentCall().getState();
		} else
			return State.Error;
	}

	private boolean isTunnelNeeded(NetworkInfo info) {
		if (info == null) {
			Log.i("No connectivity: tunnel should be disabled");
			return false;
		}

		String pref = getPrefString(R.string.pref_tunnel_mode_key, R.string.default_tunnel_mode_entry_value);

		if (getString(R.string.tunnel_mode_entry_value_always).equals(pref)) {
			return true;
		}

		if (info.getType() != ConnectivityManager.TYPE_WIFI
				&& getString(R.string.tunnel_mode_entry_value_3G_only).equals(pref)) {
			Log.i("need tunnel: 'no wifi' connection");
			return true;
		}

		return false;
	}

	public void manageTunnelServer(NetworkInfo info) {
		if (mLc == null)
			return;
		if (!mLc.isTunnelAvailable())
			return;

		Log.i("Managing tunnel");
		if (isTunnelNeeded(info)) {
			Log.i("Tunnel need to be activated");
			mLc.tunnelEnable(true);
		} else {
			Log.i("Tunnel should not be used");
			String pref = getPrefString(R.string.pref_tunnel_mode_key, R.string.default_tunnel_mode_entry_value);
			mLc.tunnelEnable(false);
			if (getString(R.string.tunnel_mode_entry_value_auto).equals(pref)) {
				mLc.tunnelAutoDetect();
			}
		}
	}

	/**
	 * 开始lib包中的Linphone
	 * 
	 * @param c
	 */
	private synchronized void startLibLinphone(Context c) {
		try {
			// 从Assents文件夹中Copy资源
			copyAssetsFromPackage();
			// traces alway start with traces enable to not missed first
			// initialization

			boolean isDebugLogEnabled = !(mR.getBoolean(R.bool.disable_every_log))
					&& getPrefBoolean(R.string.pref_debug_key, mR.getBoolean(R.bool.pref_debug_default));
			LinphoneCoreFactory.instance().setDebugMode(isDebugLogEnabled, getString(R.string.app_name));

			// Try to get remote provisioning
			String remote_provisioning = (getPrefString(R.string.pref_remote_provisioning_key,
					mR.getString(R.string.pref_remote_provisioning_default)));
			if (remote_provisioning != null && remote_provisioning.length() > 0 && RemoteProvisioning.isAvailable()) {
				RemoteProvisioning.download(remote_provisioning, mLinphoneConfigFile);
			}

			mLc = LinphoneCoreFactory.instance().createLinphoneCore(this, mLinphoneConfigFile,
					mLinphoneInitialConfigFile, null);
			mLc.getConfig().setInt("sip", "store_auth_info", 0);
			mLc.setContext(c);
			try {
				String versionName = c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
				if (versionName == null) {
					versionName = String
							.valueOf(c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionCode);
				}
				mLc.setUserAgent("LinphoneAndroid", versionName);
			} catch (NameNotFoundException e) {
				Log.e(e, "cannot get version name");
			}

			mLc.enableIpv6(getPrefBoolean(R.string.pref_ipv6_key, false));
			mLc.setZrtpSecretsCache(basePath + "/zrtp_secrets");

			mLc.setRing(null);
			mLc.setRootCA(mLinphoneRootCaFile);
			mLc.setPlayFile(mPauseSoundFile);

			int availableCores = Runtime.getRuntime().availableProcessors();
			Log.w("MediaStreamer : " + availableCores + " cores detected and configured");
			mLc.setCpuCount(availableCores);

			try {
				initFromConf();
			} catch (LinphoneException e) {
				Log.w("no config ready yet");
			}

			TimerTask lTask = new TimerTask() {
				@Override
				public void run() {
					mLc.iterate();
				}
			};
			mTimer.scheduleAtFixedRate(lTask, 0, 20);

			IntentFilter lFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
			lFilter.addAction(Intent.ACTION_SCREEN_OFF);
			mServiceContext.registerReceiver(mKeepAliveReceiver, lFilter);

			// startBluetooth(); //CYY
			resetCameraFromPreferences();
		} catch (Exception e) {
			Log.e(e, "Cannot start org.linphone");
		}
	}

	private void copyAssetsFromPackage() throws IOException {
		// copyIfNotExist(R.raw.oldphone_mono, mRingSoundFile);
		// copyIfNotExist(R.raw.playback, mRingbackSoundFile);
		// copyIfNotExist(R.raw.toy_mono, mPauseSoundFile);
		//
		// copyIfNotExist(R.raw.ring_1, mRing_1);
		// copyIfNotExist(R.raw.ring_2, mRing_2);
		// copyIfNotExist(R.raw.ring_3, mRing_3);
		// copyIfNotExist(R.raw.ring_4, mRing_4);
		// copyIfNotExist(R.raw.ring_5, mRing_5);
		// copyIfNotExist(R.raw.ring_7, mRing_7);
		// copyIfNotExist(R.raw.ring_8, mRing_8);
		// copyIfNotExist(R.raw.ring_9, mRing_9);
		// copyIfNotExist(R.raw.ring_10, mRing_10);
		// copyIfNotExist(R.raw.ring_11, mRing_11);
		// copyIfNotExist(R.raw.ring_12, mRing_12);

		copyFromPackage(R.raw.linphonerc, new File(mLinphoneInitialConfigFile).getName());
		copyIfNotExist(R.raw.lpconfig, new File(mLPConfigXsd).getName());
		copyIfNotExist(R.raw.rootca, new File(mLinphoneRootCaFile).getName());
	}

	private void copyIfNotExist(int ressourceId, String target) throws IOException {
		File lFileToCopy = new File(target);
		if (!lFileToCopy.exists()) {
			copyFromPackage(ressourceId, lFileToCopy.getName());
		}
	}

	private void copyFromPackage(int ressourceId, String target) throws IOException {
		FileOutputStream lOutputStream = mServiceContext.openFileOutput(target, 0);
		InputStream lInputStream = mR.openRawResource(ressourceId);
		int readByte;
		byte[] buff = new byte[8048];
		while ((readByte = lInputStream.read(buff)) != -1) {
			lOutputStream.write(buff, 0, readByte);
		}
		lOutputStream.flush();
		lOutputStream.close();
		lInputStream.close();
	}

	public boolean detectVideoCodec(String mime) {
		for (PayloadType videoCodec : mLc.getVideoCodecs()) {
			if (mime.equals(videoCodec.getMime()))
				return true;
		}
		return false;
	}

	public boolean detectAudioCodec(String mime) {
		for (PayloadType audioCodec : mLc.getAudioCodecs()) {
			if (mime.equals(audioCodec.getMime()))
				return true;
		}
		return false;
	}

	void initMediaEncryption() {
		String pref = getPrefString(R.string.pref_media_encryption_key, R.string.pref_media_encryption_key_none);
		MediaEncryption me = MediaEncryption.None;
		if (pref.equals(getString(R.string.pref_media_encryption_key_srtp)))
			me = MediaEncryption.SRTP;
		else if (pref.equals(getString(R.string.pref_media_encryption_key_zrtp)))
			me = MediaEncryption.ZRTP;
		Log.i("Media encryption set to " + pref);
		mLc.setMediaEncryption(me);
	}

	private void initFromConfTunnel() {
		if (!mLc.isTunnelAvailable())
			return;

		NetworkInfo info = mConnectivityManager.getActiveNetworkInfo();
		mLc.tunnelCleanServers();
		String host = getString(R.string.tunnel_host);
		if (host == null || host.length() == 0)
			host = mPref.getString(getString(R.string.pref_tunnel_host_key), "");
		int port = Integer.parseInt(getPrefString(R.string.pref_tunnel_port_key, "443"));
		mLc.tunnelAddServerAndMirror(host, port, 12345, 500);
		manageTunnelServer(info);
	}

	public void initAccounts() throws LinphoneCoreException {
		mLc.clearAuthInfos();
		mLc.clearProxyConfigs();

		for (int i = 0; i < getPrefExtraAccountsNumber(); i++) {
			String key = i == 0 ? "" : String.valueOf(i);
			if (!getPrefBoolean(getString(R.string.pref_disable_account_key) + key, false)) {
				initAccount(key, i == getPrefInt(R.string.pref_default_account_key, 0));
			}
		}

		LinphoneProxyConfig lDefaultProxyConfig = mLc.getDefaultProxyConfig();
		if (lDefaultProxyConfig != null) {
			// prefix
			String lPrefix = getPrefString(R.string.pref_prefix_key, null);
			if (lPrefix != null) {
				lDefaultProxyConfig.setDialPrefix(lPrefix);
			}
			// escape +
			lDefaultProxyConfig.setDialEscapePlus(getPrefBoolean(R.string.pref_escape_plus_key, false));
		} else if (LinphoneService.isReady()) {
			LinphoneService.instance().onRegistrationStateChanged(RegistrationState.RegistrationNone, null);
		}
	}

	private void initAccount(String key, boolean defaultAccount) throws LinphoneCoreException {
		String username = getPrefString(getString(R.string.pref_username_key) + key, null);
		String password = getPrefString(getString(R.string.pref_passwd_key) + key, null);
		String domain = getPrefString(getString(R.string.pref_domain_key) + key, null);
		if (username != null && username.length() > 0 && password != null) {
			LinphoneAuthInfo lAuthInfo = LinphoneCoreFactory.instance().createAuthInfo(username, password, null);
			mLc.addAuthInfo(lAuthInfo);

			if (domain != null && domain.length() > 0) {
				String identity = "sip:" + username + "@" + domain;
				String proxy = getPrefString(getString(R.string.pref_proxy_key) + key, null);
				if (proxy == null || proxy.length() == 0) {
					proxy = "sip:" + domain;
				}
				if (!proxy.startsWith("sip:")) {
					proxy = "sip:" + proxy;
				}

				LinphoneProxyConfig proxycon = LinphoneCoreFactory.instance().createProxyConfig(identity, proxy, null,
						true);
				String defaultExpire = getString(R.string.pref_expire_default);
				proxycon.setExpires(
						tryToParseIntValue(getPrefString(R.string.pref_expire_key, defaultExpire), defaultExpire));

				// Add parameters for push notifications
				if (mR.getBoolean(R.bool.enable_push_id)) {
					String regId = getPrefString(R.string.push_reg_id_key, null);
					String appId = getString(R.string.push_sender_id);
					if (regId != null && getPrefBoolean(R.string.pref_push_notification_key,
							mR.getBoolean(R.bool.pref_push_notification_default))) {
						String contactInfos = "app-id=" + appId + ";pn-type=google;pn-tok=" + regId
								+ ";pn-msg-str=IM_MSG;pn-call-str=IC_MSG;pn-call-snd=ring.caf;pn-msg-snd=msg.caf";
						proxycon.setContactParameters(contactInfos);
					}
				} else if (contactParams != null) {
					proxycon.setContactParameters(contactParams);
				}
				mLc.addProxyConfig(proxycon);

				// outbound proxy
				if (getPrefBoolean(getString(R.string.pref_enable_outbound_proxy_key) + key, false)) {
					proxycon.setRoute(proxy);
				} else {
					proxycon.setRoute(null);
				}
				proxycon.done();

				if (defaultAccount) {
					mLc.setDefaultProxyConfig(proxycon);
				}
			}
		}
	}

	private void readAndSetAudioAndVideoPorts() throws NumberFormatException {
		int aPortStart, aPortEnd, vPortStart, vPortEnd;
		int defaultAudioPort, defaultVideoPort;
		defaultAudioPort = Integer.parseInt(getString(R.string.default_audio_port));
		defaultVideoPort = Integer.parseInt(getString(R.string.default_video_port));
		aPortStart = aPortEnd = defaultAudioPort;
		vPortStart = vPortEnd = defaultVideoPort;

		String audioPort = getPrefString(R.string.pref_audio_port_key, String.valueOf(aPortStart));
		String videoPort = getPrefString(R.string.pref_video_port_key, String.valueOf(vPortStart));

		if (audioPort.contains("-")) {
			// Port range
			aPortStart = Integer.parseInt(audioPort.split("-")[0]);
			aPortEnd = Integer.parseInt(audioPort.split("-")[1]);
		} else {
			try {
				aPortStart = aPortEnd = Integer.parseInt(audioPort);
			} catch (NumberFormatException nfe) {
				aPortStart = aPortEnd = defaultAudioPort;
			}
		}

		if (videoPort.contains("-")) {
			// Port range
			vPortStart = Integer.parseInt(videoPort.split("-")[0]);
			vPortEnd = Integer.parseInt(videoPort.split("-")[1]);
		} else {
			try {
				vPortStart = vPortEnd = Integer.parseInt(videoPort);
			} catch (NumberFormatException nfe) {
				vPortStart = vPortEnd = defaultVideoPort;
			}
		}

		if (aPortStart >= aPortEnd) {
			mLc.setAudioPort(aPortStart);
		} else {
			mLc.setAudioPortRange(aPortStart, aPortEnd);
		}

		if (vPortStart >= vPortEnd) {
			mLc.setVideoPort(vPortStart);
		} else {
			mLc.setVideoPortRange(vPortStart, vPortEnd);
		}
	}

	private int tryToParseIntValue(String valueToParse, String defaultValue) {
		return tryToParseIntValue(valueToParse, Integer.parseInt(defaultValue));
	}

	private int tryToParseIntValue(String valueToParse, int defaultValue) {
		try {
			int returned = Integer.parseInt(valueToParse);
			return returned;
		} catch (NumberFormatException nfe) {

		}
		return defaultValue;
	}

	public void setContactParams(String params) {
		contactParams = params;
	}

	public void initFromConf() throws LinphoneConfigException {
		boolean isDebugLogEnabled = !(mR.getBoolean(R.bool.disable_every_log))
				&& getPrefBoolean(R.string.pref_debug_key, mR.getBoolean(R.bool.pref_debug_default));
		LinphoneCoreFactory.instance().setDebugMode(isDebugLogEnabled, getString(R.string.app_name));
		initFromConfTunnel();

		if (initialTransports == null)
			initialTransports = mLc.getSignalingTransportPorts();

		setSignalingTransportsFromConfiguration(initialTransports);
		initMediaEncryption();

		mLc.setVideoPolicy(isAutoInitiateVideoCalls(), isAutoAcceptCamera());

		readAndSetAudioAndVideoPorts();

		String defaultIncomingCallTimeout = getString(R.string.pref_incoming_call_timeout_default);
		int incomingCallTimeout = tryToParseIntValue(
				getPrefString(R.string.pref_incoming_call_timeout_key, defaultIncomingCallTimeout),
				defaultIncomingCallTimeout);
		mLc.setIncomingTimeout(incomingCallTimeout);

		try {
			// Configure audio codecs
			// enableDisableAudioCodec("speex", 32000, 1,
			// R.string.pref_codec_speex32_key);
			enableDisableAudioCodec("speex", 32000, 1, false);
			enableDisableAudioCodec("speex", 16000, 1, R.string.pref_codec_speex16_key);
			enableDisableAudioCodec("speex", 8000, 1, R.string.pref_codec_speex8_key);
			enableDisableAudioCodec("iLBC", 8000, 1, R.string.pref_codec_ilbc_key);
			enableDisableAudioCodec("GSM", 8000, 1, R.string.pref_codec_gsm_key);
			enableDisableAudioCodec("G722", 8000, 1, R.string.pref_codec_g722_key);
			enableDisableAudioCodec("G729", 8000, 1, R.string.pref_codec_g729_key);
			enableDisableAudioCodec("PCMU", 8000, 1, R.string.pref_codec_pcmu_key);
			enableDisableAudioCodec("PCMA", 8000, 1, R.string.pref_codec_pcma_key);
			enableDisableAudioCodec("AMR", 8000, 1, R.string.pref_codec_amr_key);
			enableDisableAudioCodec("AMR-WB", 16000, 1, R.string.pref_codec_amrwb_key);
			// enableDisableAudioCodec("SILK", 24000, 1,
			// R.string.pref_codec_silk24_key);
			enableDisableAudioCodec("SILK", 24000, 1, false);
			enableDisableAudioCodec("SILK", 16000, 1, R.string.pref_codec_silk16_key);
			// enableDisableAudioCodec("SILK", 12000, 1,
			// R.string.pref_codec_silk12_key);
			enableDisableAudioCodec("SILK", 12000, 1, false);
			enableDisableAudioCodec("SILK", 8000, 1, R.string.pref_codec_silk8_key);

			// Configure video codecs
			for (PayloadType videoCodec : mLc.getVideoCodecs()) {
				enableDisableVideoCodecs(videoCodec);
			}

			boolean useEC = getPrefBoolean(R.string.pref_echo_cancellation_key,
					mR.getBoolean(R.bool.pref_echo_canceller_default));
			mLc.enableEchoCancellation(useEC);
		} catch (LinphoneCoreException e) {
			throw new LinphoneConfigException(getString(R.string.wrong_settings), e);
		}
		boolean isVideoEnabled = isVideoEnabled();
		mLc.enableVideo(isVideoEnabled, isVideoEnabled);

		// stun server
		String lStun = getPrefString(R.string.pref_stun_server_key, getString(R.string.default_stun));
		boolean useICE = getPrefBoolean(R.string.pref_ice_enable_key, mR.getBoolean(R.bool.pref_ice_enabled_default));
		mLc.setStunServer(lStun);
		if (lStun != null && lStun.length() > 0) {
			mLc.setFirewallPolicy(useICE ? FirewallPolicy.UseIce : FirewallPolicy.UseStun);
		} else {
			mLc.setFirewallPolicy(FirewallPolicy.NoFirewall);
		}

		mLc.setUseRfc2833ForDtmfs(
				getPrefBoolean(R.string.pref_rfc2833_dtmf_key, mR.getBoolean(R.bool.pref_rfc2833_dtmf_default)));
		mLc.setUseSipInfoForDtmfs(
				getPrefBoolean(R.string.pref_sipinfo_dtmf_key, mR.getBoolean(R.bool.pref_sipinfo_dtmf_default)));

		String displayName = getPrefString(R.string.pref_display_name_key,
				getString(R.string.pref_display_name_default));
		String username = getPrefString(R.string.pref_user_name_key, getString(R.string.pref_user_name_default));
		mLc.setPrimaryContact(displayName, username);

		// accounts
		try {
			initAccounts();

			// init network state
			NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
			SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mServiceContext);
			boolean wifiOnly = pref.getBoolean(getString(R.string.pref_wifi_only_key),
					mR.getBoolean(R.bool.pref_wifi_only_default));
			boolean isConnected = false;
			if (networkInfo != null) {
				isConnected = networkInfo.getState() == NetworkInfo.State.CONNECTED
						&& (networkInfo.getTypeName().equals("WIFI")
								|| (networkInfo.getTypeName().equals("mobile") && !wifiOnly));
			}
			mLc.setNetworkReachable(isConnected);
		} catch (LinphoneCoreException e) {
			throw new LinphoneConfigException(getString(R.string.wrong_settings), e);
		}
	}

	private void setSignalingTransportsFromConfiguration(Transports t) {
		Transports ports = new Transports(t);
		boolean useRandomPort = getPrefBoolean(R.string.pref_transport_use_random_ports_key,
				mR.getBoolean(R.bool.pref_transport_use_random_ports_default));
		int lPreviousPort = tryToParseIntValue(
				getPrefString(R.string.pref_sip_port_key, getString(R.string.pref_sip_port_default)), 5060);
		if (lPreviousPort > 0xFFFF || useRandomPort) {
			lPreviousPort = (int) (Math.random() * (0xFFFF - 1024)) + 1024;
			Log.w("Using random port " + lPreviousPort);
		}

		String transport = getPrefString(R.string.pref_transport_key, getString(R.string.pref_transport_udp_key));
		if (transport.equals(getString(R.string.pref_transport_tcp_key))) {
			ports.udp = 0;
			ports.tls = 0;
			ports.tcp = lPreviousPort;
		} else if (transport.equals(getString(R.string.pref_transport_udp_key))) {
			ports.tcp = 0;
			ports.tls = 0;
			ports.udp = lPreviousPort;
		} else if (transport.equals(getString(R.string.pref_transport_tls_key))) {
			ports.udp = 0;
			ports.tcp = 0;
			ports.tls = lPreviousPort;
		}

		mLc.setSignalingTransportPorts(ports);
	}

	private void enableDisableAudioCodec(String codec, int rate, int channels, int key) throws LinphoneCoreException {
		PayloadType pt = mLc.findPayloadType(codec, rate, channels);
		if (pt != null) {
			boolean enable = getPrefBoolean(key, false);
			mLc.enablePayloadType(pt, enable);
		}
	}

	private void enableDisableAudioCodec(String codec, int rate, int channels, boolean enable)
			throws LinphoneCoreException {
		PayloadType pt = mLc.findPayloadType(codec, rate, channels);
		if (pt != null) {
			mLc.enablePayloadType(pt, enable);
		}
	}

	private void enableDisableVideoCodecs(PayloadType videoCodec) throws LinphoneCoreException {
		String mime = videoCodec.getMime();
		int key;
		int defaultValueKey;

		if ("MP4V-ES".equals(mime)) {
			key = R.string.pref_video_codec_mpeg4_key;
			defaultValueKey = R.bool.pref_video_codec_mpeg4_default;
		} else if ("H264".equals(mime)) {
			key = R.string.pref_video_codec_h264_key;
			defaultValueKey = R.bool.pref_video_codec_h264_default;
		} else if ("H263-1998".equals(mime)) {
			key = R.string.pref_video_codec_h263_key;
			defaultValueKey = R.bool.pref_video_codec_h263_default;
		} else if ("VP8".equals(mime)) {
			key = R.string.pref_video_codec_vp8_key;
			defaultValueKey = R.bool.pref_video_codec_vp8_default;
		} else {
			Log.e("Unhandled video codec ", mime);
			mLc.enablePayloadType(videoCodec, false);
			return;
		}

		boolean enable = getPrefBoolean(key, mR.getBoolean(defaultValueKey));
		mLc.enablePayloadType(videoCodec, enable);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void doDestroy() {
		if (chatStorage != null) {
			chatStorage.close();
			chatStorage = null;
		}

		try {
			mServiceContext.unregisterReceiver(bluetoothReiceiver);
		} catch (Exception e) {
		}

		try {
			if (Version.sdkAboveOrEqual(Version.API11_HONEYCOMB_30))
				mBluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, mBluetoothHeadset);
		} catch (Exception e) {
		}

		try {
			mTimer.cancel();
			mLc.destroy();
		} catch (RuntimeException e) {
			e.printStackTrace();
		} finally {
			mServiceContext.unregisterReceiver(instance.mKeepAliveReceiver);
			mLc = null;
			instance = null;
		}
	}

	public static synchronized void destroy() {
		if (instance == null)
			return;
		sExited = true;
		threadRunning = false;
		instance.doDestroy();
	}

	private String getString(int key) {
		return mR.getString(key);
	}

	private boolean getPrefBoolean(int key, boolean value) {
		return mPref.getBoolean(mR.getString(key), value);
	}

	private boolean getPrefBoolean(String key, boolean value) {
		return mPref.getBoolean(key, value);
	}

	private String getPrefString(int key, String value) {
		return mPref.getString(mR.getString(key), value);
	}

	private int getPrefInt(int key, int value) {
		return mPref.getInt(mR.getString(key), value);
	}

	private String getPrefString(int key, int value) {
		return mPref.getString(mR.getString(key), mR.getString(value));
	}

	private String getPrefString(String key, String value) {
		return mPref.getString(key, value);
	}

	private int getPrefExtraAccountsNumber() {
		return mPref.getInt(getString(R.string.pref_extra_accounts), 1);
	}

	/*
	 * Simple implementation as Android way seems very complicate: For example:
	 * with wifi and mobile actives; when pulling mobile down: I/Linphone(
	 * 8397): WIFI connected: setting network reachable I/Linphone( 8397): new
	 * state [RegistrationProgress] I/Linphone( 8397): mobile disconnected:
	 * setting network unreachable I/Linphone( 8397): Managing tunnel
	 * I/Linphone( 8397): WIFI connected: setting network reachable
	 */

	// 网络连接管理池
	public void connectivityChanged(ConnectivityManager cm, boolean noConnectivity) {
		NetworkInfo eventInfo = cm.getActiveNetworkInfo();

		if (eventInfo != null && eventInfo.isAvailable()) {
			Log.e("Lm==========", " eventInfo.getType():---------->" + eventInfo.getTypeName());
		}

		//
		Log.e("Lm==========", "eventInfo:---------->" + eventInfo);
		Log.e("Lm==========", "noConnectivity:---------->" + noConnectivity);
		// Log.e("Lm==========","eventInfo.getState():---------->"+eventInfo!=null?eventInfo.getState():"event
		// is null");

		if (noConnectivity || eventInfo == null || eventInfo.getState() == NetworkInfo.State.DISCONNECTED) {
			Log.i("No connectivity: setting network unreachable");
			mLc.setNetworkReachable(false);
		} else if (eventInfo.getState() == NetworkInfo.State.CONNECTED) {

			manageTunnelServer(eventInfo); // 管理伺服务

			SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mServiceContext);
			boolean wifiOnly = pref.getBoolean(getString(R.string.pref_wifi_only_key),
					mR.getBoolean(R.bool.pref_wifi_only_default));

			if (eventInfo.getTypeName().equals("WIFI") || eventInfo.getTypeName().equals("ETHERNET")
					|| (eventInfo.getTypeName().equals("mobile") && !wifiOnly)) {
				mLc.setNetworkReachable(true);

				Log.i(eventInfo.getTypeName(), " connected: setting network reachable");

			} else {
				mLc.setNetworkReachable(false);
				Log.i(eventInfo.getTypeName(), " connected: wifi only activated, setting network unreachable");
			}
		}

		if (connectivityListener != null) {
			connectivityListener.onConnectivityChanged(mServiceContext, eventInfo, cm);
		}
	}

	private ConnectivityChangedListener connectivityListener;

	public void addConnectivityChangedListener(ConnectivityChangedListener l) {
		connectivityListener = l;
	}

	public interface EcCalibrationListener {
		void onEcCalibrationStatus(EcCalibratorStatus status, int delayMs);
	}

	private ListenerDispatcher mListenerDispatcher;
	private LinphoneCall ringingCall;

	private static MediaPlayer mRingerPlayer;
	private static Vibrator mVibrator;

	public void displayWarning(LinphoneCore lc, String message) {
	}

	public void authInfoRequested(LinphoneCore lc, String realm, String username) {
	}

	public void byeReceived(LinphoneCore lc, String from) {
	}

	public void displayMessage(LinphoneCore lc, String message) {
	}

	public void show(LinphoneCore lc) {
	}

	public void newSubscriptionRequest(LinphoneCore lc, LinphoneFriend lf, String url) {
		for (LinphoneSimpleListener listener : getSimpleListeners(LinphoneActivity.class)) {
			((LinphoneActivity) listener).onNewSubscriptionRequestReceived(lf, url);
		}
	}

	public void notifyPresenceReceived(LinphoneCore lc, LinphoneFriend lf) {
		for (LinphoneSimpleListener listener : getSimpleListeners(LinphoneActivity.class)) {
			((LinphoneActivity) listener).onNotifyPresenceReceived(lf);
		}
	}

	public void textReceived(LinphoneCore lc, LinphoneChatRoom cr, LinphoneAddress from, String message) {
		// deprecated
	}

	@Override
	public void dtmfReceived(LinphoneCore lc, LinphoneCall call, int dtmf) {
		Log.d("DTMF received: " + dtmf);
		if (dtmfReceivedListener != null)
			dtmfReceivedListener.onDTMFReceived(call, dtmf);
	}

	private LinphoneOnDTMFReceivedListener dtmfReceivedListener;

	public void setOnDTMFReceivedListener(LinphoneOnDTMFReceivedListener listener) {
		dtmfReceivedListener = listener;
	}

	@Override
	public void messageReceived(LinphoneCore lc, LinphoneChatRoom cr, LinphoneChatMessage message) {
		Log.d("chatRoom message", "received message:" + message.getText());
		if (mServiceContext.getResources().getBoolean(R.bool.disable_chat)) {
			return;
		}

		// add by jimmy at 2014.5.20

		if (message.getText().equals("START_AUDIO")) {
			mAudioManager.setMicrophoneMute(false);
			Intent intentDoorOpened = new Intent("com.intercom40.conversation.startAudio");
			getContext().sendBroadcast(intentDoorOpened);
		}

		if (message.getText().equals("BUSY")) {
			Intent intentDoorOpened = new Intent("com.intercom40.conversation.busy");
			getContext().sendBroadcast(intentDoorOpened);
		}

		if (message.getText().equals("STOP_AUDIO")) {
			mAudioManager.setMicrophoneMute(true);
		}

		if (message.getText().equals("OPEN DOOR")) {
			Intent intentDoorOpened = new Intent("com.intercom70.conversation.doorOpened");
			getContext().sendBroadcast(intentDoorOpened);
			Log.d("send open door code to base");
			mLinphoneI2CInterface.SetDoorRelease();
		}

		if (message.getText().startsWith("FOLLOW_ME:")) {
			String[] data = message.getText().split(":");
			Log.d("FOLLOW_ME sipip = " + data[1]);

			Intent intentFollowMe = new Intent("com.intercom40.conversation.isSetFollowMe");
			intentFollowMe.putExtra("followMeNumber", data[1]);
			getContext().sendBroadcast(intentFollowMe);

			// if (mLc.getCallsNb() > 1) {
			// mLc.terminateCall(mCall);
			// } else {
			// terminateCall();
			// }

			// new CallThread(data[1]).start();

		}

		// Add by wuhua
		if ("Accepted Video?".equals(message.getText())) {
			Log.e("createchatroom", "LinphoneManager receive message content------>" + message.getText());
			getContext().sendBroadcast(new Intent("com.intercom70.conversation.videorequest"));
		} else if ("YES".equals(message.getText())) {
			getContext().sendBroadcast(new Intent("com.intercom70.conversation.acceptvideorequest"));
		}

		LinphoneAddress from = message.getFrom();

		String textMessage = message.getText();
		String url = message.getExternalBodyUrl();
		String notificationText = null;
		int id = -1;
		if (textMessage != null && textMessage.length() > 0) {
			id = chatStorage.saveTextMessage(from.asStringUriOnly(), "", textMessage, message.getTime());
			notificationText = textMessage;
		} else if (url != null && url.length() > 0) {
			// Bitmap bm = ChatFragment.downloadImage(url);
			id = chatStorage.saveImageMessage(from.asStringUriOnly(), "", null, message.getExternalBodyUrl(),
					message.getTime());
			notificationText = url;
		}

		LinphoneUtils.findUriPictureOfContactAndSetDisplayName(from, mServiceContext.getContentResolver());
		// LinphoneService.instance().displayMessageNotification(from.asStringUriOnly(),
		// from.getDisplayName(), notificationText);

		for (LinphoneSimpleListener listener : getSimpleListeners(LinphoneActivity.class)) {
			((LinphoneOnMessageReceivedListener) listener).onMessageReceived(from, message, id);
		}
	}

	public String getLastLcStatusMessage() {
		return lastLcStatusMessage;
	}

	public void displayStatus(final LinphoneCore lc, final String message) {
		Log.i(message);
		lastLcStatusMessage = message;
		mListenerDispatcher.onDisplayStatus(message);
	}

	public void globalState(final LinphoneCore lc, final LinphoneCore.GlobalState state, final String message) {
		Log.i("new state [", state, "]");
		mListenerDispatcher.onGlobalStateChanged(state, message);
	}

	public void registrationState(final LinphoneCore lc, final LinphoneProxyConfig cfg,
			final LinphoneCore.RegistrationState state, final String message) {
		Log.i("new state [" + state + "]");
		mListenerDispatcher.onRegistrationStateChanged(state, message);
	}

	private int savedMaxCallWhileGsmIncall;

	private synchronized void preventSIPCalls() {
		if (savedMaxCallWhileGsmIncall != 0) {
			Log.w("SIP calls are already blocked due to GSM call running");
			return;
		}
		savedMaxCallWhileGsmIncall = mLc.getMaxCalls();
		mLc.setMaxCalls(0);
	}

	private synchronized void allowSIPCalls() {
		if (savedMaxCallWhileGsmIncall == 0) {
			Log.w("SIP calls are already allowed as no GSM call knowned to be running");
			return;
		}
		mLc.setMaxCalls(savedMaxCallWhileGsmIncall);
		savedMaxCallWhileGsmIncall = 0;
	}

	public static void setGsmIdle(boolean gsmIdle) {
		LinphoneManager mThis = instance;
		if (mThis == null)
			return;
		if (gsmIdle) {
			mThis.allowSIPCalls();
		} else {
			mThis.preventSIPCalls();
		}
	}

	public Context getContext() {
		try {
			if (LinphoneActivity.isInstanciated())
				return LinphoneActivity.instance();
			else if (InCallActivity.isInstanciated())
				return InCallActivity.instance();
			else if (IncomingCallActivity.isInstanciated())
				return IncomingCallActivity.instance();
			else
				return LinphoneService.instance().getApplicationContext();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@SuppressLint("Wakelock")
	public void callState(final LinphoneCore lc, final LinphoneCall call, final State state, final String message) {
		Log.i("new state [", state, "]");
		if (state == IncomingReceived && !call.equals(lc.getCurrentCall())) {
			String fromSipip = call.getRemoteAddress().getDomain();
			LinphoneChatRoom mChatRoom = getLcIfManagerNotDestroyedOrNull().createChatRoom("sip:" + fromSipip);
			LinphoneChatMessage mMessage = mChatRoom.createLinphoneChatMessage("BUSY");
			mChatRoom.sendMessage(mMessage, new StateListener() {

				@Override
				public void onLinphoneChatMessageStateChanged(LinphoneChatMessage msg,
						org.linphone.core.LinphoneChatMessage.State state) {
					// TODO Auto-generated method stub

				}
			});
			return;

			// if (call.getReplacedCall()!=null){
			// // attended transfer
			// // it will be accepted automatically.
			// return;
			// }
		}

		mCall = call;

		if (state == LinphoneCall.State.Connected) {
			if (mLc.getCallsNb() == 1) {
				Log.d("Audio focus requested: " + (mAudioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL,
						AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED ? "Granted"
								: "Denied"));
			}
			// routeAudioToReceiver(); // CYY
		}

		if (state == LinphoneCall.State.OutgoingInit) {
			// routeAudioToReceiver(); // CYY
			willCall = true;
		}

		if (state == LinphoneCall.State.Paused) {
			if (willCall) {
				lc.terminateCall(call);
			}
			willCall = false;
		}

		if (state == IncomingReceived
				|| (state == State.CallIncomingEarlyMedia && mR.getBoolean(R.bool.allow_ringing_while_early_media))) {
			// Brighten screen for at least 10 seconds
			if (!devInfoSp.getBoolean("isSetFollowMe", false)) { // default
																	// value
																	// should be
																	// false
																	// after
																	// test
				if (call.getRemoteParams().getCustomHeader(LinphoneManager.HEARDER_IS_ONLYVIDEO)
						.equals(LinphoneManager.VALUE_FALSE)) {
					Log.d("IncomingReceived ", "normal conversation start ring");

					boolean onlyGuardCall = devInfoSp.getBoolean("onlyGuardCall", false);
					String inComingIPTem = mCall.getRemoteAddress().getDomain();
					String[] arrIPTem = (inComingIPTem == null) ? null : inComingIPTem.split("\\.");
					int forthNum = Integer.parseInt(arrIPTem[3]);
					if (onlyGuardCall) { // added by jimmy at 2014.06.16
						Log.e("onlyGuardCall", "onlyGuardCall-->" + onlyGuardCall + "&&" + inComingIPTem);
						if (arrIPTem[2].equals("24") && (forthNum >= 1 && forthNum <= 9)) {
							if (mLc.getCallsNb() == 1) {
								ringingCall = call;
								startRinging(ringingCall);
								// otherwise there is the beep
							}
						} else if (inComingIPTem.startsWith("10.99.26.") && (forthNum >= 1 && forthNum <= 19)) {
							if (mLc.getCallsNb() == 1) {
								ringingCall = call;
								startRinging(ringingCall);
								// otherwise there is the beep
							}
						}
					} else {
						if (mLc.getCallsNb() == 1) {
							ringingCall = call;
							startRinging(ringingCall);
							// otherwise there is the beep
						}
					}

					// if (mLc.getCallsNb() == 1) {
					// ringingCall = call;
					// startRinging(ringingCall);
					// // otherwise there is the beep
					// }

				}
			}
		} else if (call == ringingCall && isRinging) {
			// previous state was ringing, so stop ringing
			stopRinging();
		}

		if (state == CallEnd || state == Error) {
			if (mLc.getCallsNb() == 0) {
				Log.d("Audio focus released: "
						+ (mAudioManager.abandonAudioFocus(null) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED ? "Granted"
								: "Denied"));
				mAudioManager.setMode(AudioManager.MODE_NORMAL);
			}
			Context activity = getContext();
			if (activity != null) {
				TelephonyManager tm = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
				if (tm.getCallState() == TelephonyManager.CALL_STATE_IDLE) {
					mAudioManager.setMode(AudioManager.MODE_NORMAL);
				}
			}
		}
		if (state == State.Connected) {
			// if (Hacks.needSoftvolume() || sLPref.useSoftvolume()) {
			// adjustVolume(0); // Synchronize //CYY
			// }

			// mAudioManager.setMode(AudioManager.MODE_IN_CALL); //add by jimmy
			// at 2014.05.16

		}

		if (state == CallEnd) {
			if (mLc.getCallsNb() == 0) {
				if (mIncallWakeLock != null && mIncallWakeLock.isHeld()) {
					mIncallWakeLock.release();
					Log.i("Last call ended: releasing incall (CPU only) wake lock");
				} else {
					Log.i("Last call ended: no incall (CPU only) wake lock were held");
				}
			} else {
				return; // CYY
			}
		} else if (state == LinphoneCall.State.CallReleased && mLc.getCallsNb() > 0) {
			return; // CYY
		}

		if (state == State.StreamsRunning) {
			if (mIncallWakeLock == null) {
				mIncallWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "incall");
			}
			if (!mIncallWakeLock.isHeld()) {
				Log.i("New call active : acquiring incall (CPU only) wake lock");
				mIncallWakeLock.acquire();
			} else {
				Log.i("New call active while incall (CPU only) wake lock already active");
			}
			Compatibility.setAudioManagerInCallMode(mAudioManager); // set
																	// audioManager
																	// mode to
																	// in call
																	// mode
		}
		mListenerDispatcher.onCallStateChanged(call, state, message);
	}

	public void callStatsUpdated(final LinphoneCore lc, final LinphoneCall call, final LinphoneCallStats stats) {
	}

	public void callEncryptionChanged(LinphoneCore lc, LinphoneCall call, boolean encrypted,
			String authenticationToken) {
		mListenerDispatcher.onCallEncryptionChanged(call, encrypted, authenticationToken);
	}

	public void ecCalibrationStatus(final LinphoneCore lc, final EcCalibratorStatus status, final int delayMs,
			final Object data) {
		EcCalibrationListener listener = (EcCalibrationListener) data;
		listener.onEcCalibrationStatus(status, delayMs);
	}

	public void startEcCalibration(EcCalibrationListener l) throws LinphoneCoreException {
		int oldVolume = mAudioManager.getStreamVolume(STREAM_VOICE_CALL);
		int maxVolume = mAudioManager.getStreamMaxVolume(STREAM_VOICE_CALL);
		mAudioManager.setStreamVolume(STREAM_VOICE_CALL, maxVolume, 0);

		mLc.startEchoCalibration(l);

		mAudioManager.setStreamVolume(STREAM_VOICE_CALL, oldVolume, 0);
	}

	private static boolean isRinging;
	private boolean disableRinging = false;

	public void disableRinging() {
		disableRinging = true;
	}

	private synchronized void startRinging(LinphoneCall mCall) {
		if (disableRinging) {
			return;
		}

		// CYY
		// if (Hacks.needGalaxySAudioHack()) {
		// mSaveAudioMode = mAudioManager.getMode();
		// mAudioManager.setMode(MODE_RINGTONE);
		// }
		try {
			if ((mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE
					|| mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) && mVibrator != null) {
				long[] patern = { 0, 1000, 1000 };
				mVibrator.vibrate(patern, 1);
			}
			if (mRingerPlayer == null) {
				mRingerPlayer = new MediaPlayer();

				ringByRole(mCall);
				mRingerPlayer.prepare();
				mRingerPlayer.setLooping(true);
				mRingerPlayer.start();
				// mListenerDispatcher.onRingerPlayerCreated(mRingerPlayer);

				// add by sima 动态管理免打扰
				Log.e("ma", "====mPref.getBoolean:===" + mPref.getBoolean("DISTURBE", false));
				if (devInfoSp.getBoolean("DISTURBE", false)) {
					Thread.sleep(2000);
					stopRinging();
				}
			} else {
				Log.w("already ringing");
			}
		} catch (Exception e) {
			Log.e(e, "cannot handle incoming call");
		}
		isRinging = true;
		// routeAudioToSpeaker();// CYY
	}

	private synchronized void stopRinging() {
		Log.d("Ring", "stop ring bingin");
		if (mRingerPlayer != null) {
			mRingerPlayer.pause(); // add by jimmy at 2014.5.6
			mRingerPlayer.stop();
			mRingerPlayer.release();
			mRingerPlayer = null;
			Log.d("Ring", "stop ring success");
		}
		if (mVibrator != null) {
			mVibrator.cancel();
		}

		isRinging = false;
		// mAudioManager.setMode(mSaveAudioMode);
		// You may need to call galaxys audio hack after this method
		// routeAudioToReceiver();
	}

	public static String extractADisplayName(Resources r, LinphoneAddress address) {
		if (address == null)
			return r.getString(R.string.unknown_incoming_call_name);

		final String displayName = address.getDisplayName();
		if (displayName != null) {
			return displayName;
		} else if (address.getUserName() != null) {
			return address.getUserName();
		} else {
			String rms = address.toString();
			if (rms != null && rms.length() > 1)
				return rms;

			return r.getString(R.string.unknown_incoming_call_name);
		}
	}

	public static boolean reinviteWithVideo() {
		return CallManager.getInstance().reinviteWithVideo();
	}

	public boolean isVideoEnabled() {
		return getPrefBoolean(R.string.pref_video_enable_key, false);
	}

	public boolean isAutoAcceptCamera() {
		return isVideoEnabled() && getPrefBoolean(R.string.pref_video_automatically_accept_video_key, false);
	}

	public boolean isAutoInitiateVideoCalls() {
		return isVideoEnabled() && getPrefBoolean(R.string.pref_video_initiate_call_with_video_key, false);
	}

	// Called on first launch only
	public void initializePayloads() {
		Log.i("Initializing supported payloads");
		Editor e = mPref.edit();
		boolean fastCpu = Version.hasFastCpu();

		e.putBoolean(getString(R.string.pref_codec_gsm_key), true);
		e.putBoolean(getString(R.string.pref_codec_pcma_key), true);
		e.putBoolean(getString(R.string.pref_codec_pcmu_key), true);
		e.putBoolean(getString(R.string.pref_codec_speex8_key), true);
		e.putBoolean(getString(R.string.pref_codec_g722_key), false);
		e.putBoolean(getString(R.string.pref_codec_speex16_key), fastCpu);
		e.putBoolean(getString(R.string.pref_codec_speex32_key), fastCpu);

		boolean ilbc = LinphoneService.isReady() && LinphoneManager.getLc().findPayloadType("iLBC", 8000, 1) != null;
		e.putBoolean(getString(R.string.pref_codec_ilbc_key), ilbc);

		boolean amr = LinphoneService.isReady() && LinphoneManager.getLc().findPayloadType("AMR", 8000, 1) != null;
		e.putBoolean(getString(R.string.pref_codec_amr_key), amr);

		boolean amrwb = LinphoneService.isReady()
				&& LinphoneManager.getLc().findPayloadType("AMR-WB", 16000, 1) != null;
		e.putBoolean(getString(R.string.pref_codec_amrwb_key), amrwb);

		boolean g729 = LinphoneService.isReady() && LinphoneManager.getLc().findPayloadType("G729", 8000, 1) != null;
		e.putBoolean(getString(R.string.pref_codec_g729_key), g729);

		if (Version.sdkStrictlyBelow(5) || !Version.hasNeon() || !Hacks.hasCamera()) {
			e.putBoolean(getString(R.string.pref_video_enable_key), false);
		}

		e.commit();
	}

	/**
	 * 
	 * @return false if already in video call.
	 */
	public boolean addVideo() {
		LinphoneCall call = mLc.getCurrentCall();
		enableCamera(call, true);
		return reinviteWithVideo();
	}

	public boolean acceptCallIfIncomingPending() throws LinphoneCoreException {
		if (mLc.isInComingInvitePending()) {
			mLc.acceptCall(mLc.getCurrentCall());
			return true;
		}
		return false;
	}

	public boolean acceptCall(LinphoneCall call) {
		try {
			mLc.acceptCall(call);
			return true;
		} catch (LinphoneCoreException e) {
			Log.i(e, "Accept call failed");
		}
		return false;
	}

	public boolean acceptCallWithParams(LinphoneCall call, LinphoneCallParams params) {
		try {
			mLc.acceptCallWithParams(call, params);
			return true;
		} catch (LinphoneCoreException e) {
			Log.i(e, "Accept call failed");
		}
		return false;
	}

	public static String extractIncomingRemoteName(Resources r, LinphoneAddress linphoneAddress) {
		return extractADisplayName(r, linphoneAddress);
	}

	public void adjustVolume(int i) {
		if (Build.VERSION.SDK_INT < 15) {
			int oldVolume = mAudioManager.getStreamVolume(LINPHONE_VOLUME_STREAM);
			int maxVolume = mAudioManager.getStreamMaxVolume(LINPHONE_VOLUME_STREAM);

			int nextVolume = oldVolume + i;
			if (nextVolume > maxVolume)
				nextVolume = maxVolume;
			if (nextVolume < 0)
				nextVolume = 0;

			mLc.setPlaybackGain((nextVolume - maxVolume) * dbStep);
		} else
			// starting from ICS, volume must be adjusted by the application, at
			// least for STREAM_VOICE_CALL volume stream
			mAudioManager.adjustStreamVolume(LINPHONE_VOLUME_STREAM,
					i < 0 ? AudioManager.ADJUST_LOWER : AudioManager.ADJUST_RAISE, 0);

	}

	public static Boolean isProximitySensorNearby(final SensorEvent event) {
		float threshold = 4.001f; // <= 4 cm is near

		final float distanceInCm = event.values[0];
		final float maxDistance = event.sensor.getMaximumRange();
		Log.d("Proximity sensor report [", distanceInCm, "] , for max range [", maxDistance, "]");

		if (maxDistance <= threshold) {
			// Case binary 0/1 and short sensors
			threshold = maxDistance;
		}

		return distanceInCm < threshold;
	}

	private static boolean sLastProximitySensorValueNearby;
	private static Set<Activity> sProximityDependentActivities = new HashSet<Activity>();
	private static SensorEventListener sProximitySensorListener = new SensorEventListener() {
		@Override
		public void onSensorChanged(SensorEvent event) {
			if (event.timestamp == 0)
				return; // just ignoring for nexus 1
			sLastProximitySensorValueNearby = isProximitySensorNearby(event);
			proximityNearbyChanged();
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}
	};
	private static boolean ScreenState = true;
	public static boolean isInMainPage = true;
	public static boolean isInSetting = false;

	private static void simulateProximitySensorNearby(Activity activity, boolean nearby) {
		final Window window = activity.getWindow();
		LayoutParams params = window.getAttributes();
		View view = ((ViewGroup) window.getDecorView().findViewById(android.R.id.content)).getChildAt(0);
		if (nearby) {
			params.screenBrightness = 0.1f;
			view.setVisibility(View.INVISIBLE);
			Compatibility.hideNavigationBar(activity);
		} else {
			params.screenBrightness = LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
			view.setVisibility(View.VISIBLE);
			Compatibility.showNavigationBar(activity);
		}
		window.setAttributes(params);
	}

	private static void proximityNearbyChanged() {
		boolean nearby = sLastProximitySensorValueNearby;
		for (Activity activity : sProximityDependentActivities) {
			simulateProximitySensorNearby(activity, nearby);
		}
	}

	public static synchronized void startProximitySensorForActivity(Activity activity) {
		if (sProximityDependentActivities.contains(activity)) {
			Log.i("proximity sensor already active for " + activity.getLocalClassName());
			return;
		}

		if (sProximityDependentActivities.isEmpty()) {
			SensorManager sm = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
			Sensor s = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY);
			if (s != null) {
				sm.registerListener(sProximitySensorListener, s, SensorManager.SENSOR_DELAY_UI);
				Log.i("Proximity sensor detected, registering");
			}
		} else if (sLastProximitySensorValueNearby) {
			simulateProximitySensorNearby(activity, true);
		}

		sProximityDependentActivities.add(activity);
	}

	public static synchronized void stopProximitySensorForActivity(Activity activity) {
		sProximityDependentActivities.remove(activity);
		simulateProximitySensorNearby(activity, false);
		if (sProximityDependentActivities.isEmpty()) {
			SensorManager sm = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
			sm.unregisterListener(sProximitySensorListener);
			sLastProximitySensorValueNearby = false;
		}
	}

	public static synchronized LinphoneCore getLcIfManagerNotDestroyedOrNull() {
		if (sExited) {
			// Can occur if the UI thread play a posted event but in the
			// meantime the LinphoneManager was destroyed
			// Ex: stop call and quickly terminate application.
			Log.w("Trying to get org.linphone core while LinphoneManager already destroyed");
			return null;
		}
		return getLc();
	}

	@SuppressWarnings("unchecked")
	private <T> List<T> getSimpleListeners(Class<T> clazz) {
		List<T> list = new ArrayList<T>();
		for (LinphoneSimpleListener l : simpleListeners) {
			if (clazz.isInstance(l))
				list.add((T) l);
		}
		return list;
	}

	/*
	 * 监听调度
	 */
	private class ListenerDispatcher implements LinphoneServiceListener {
		private LinphoneServiceListener serviceListener;

		public ListenerDispatcher(LinphoneServiceListener listener) {
			this.serviceListener = listener;
		}

		public void onCallEncryptionChanged(LinphoneCall call, boolean encrypted, String authenticationToken) {
			if (serviceListener != null) {
				serviceListener.onCallEncryptionChanged(call, encrypted, authenticationToken);
			}
			for (LinphoneOnCallEncryptionChangedListener l : getSimpleListeners(
					LinphoneOnCallEncryptionChangedListener.class)) {
				l.onCallEncryptionChanged(call, encrypted, authenticationToken);
			}
		}

		public void onCallStateChanged(LinphoneCall call, State state, String message) {
			if (state == State.OutgoingInit || state == State.IncomingReceived) {
				boolean sendCamera = mLc.getConferenceSize() == 0;
				enableCamera(call, sendCamera);
			}

			Context activity = getContext();
			if (activity != null) {
				TelephonyManager tm = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
				if (state == State.CallEnd && mLc.getCallsNb() == 0
						&& tm.getCallState() == TelephonyManager.CALL_STATE_IDLE) {
					// routeAudioToReceiver();
					// routeAudioToSpeaker();// CYY
				}
			}

			if (serviceListener != null)
				serviceListener.onCallStateChanged(call, state, message);
			for (LinphoneOnCallStateChangedListener l : getSimpleListeners(LinphoneOnCallStateChangedListener.class)) {
				l.onCallStateChanged(call, state, message);
			}
		}

		public void onDisplayStatus(String message) {
			if (serviceListener != null)
				serviceListener.onDisplayStatus(message);
		}

		public void onGlobalStateChanged(GlobalState state, String message) {
			if (serviceListener != null)
				serviceListener.onGlobalStateChanged(state, message);
		}

		public void onRegistrationStateChanged(RegistrationState state, String message) {
			if (serviceListener != null)
				serviceListener.onRegistrationStateChanged(state, message);
			for (LinphoneOnRegistrationStateChangedListener listener : getSimpleListeners(
					LinphoneOnRegistrationStateChangedListener.class)) {
				listener.onRegistrationStateChanged(state);
			}
		}

		public void onRingerPlayerCreated(MediaPlayer mRingerPlayer) {
			if (serviceListener != null)
				serviceListener.onRingerPlayerCreated(mRingerPlayer);
		}

		public void tryingNewOutgoingCallButAlreadyInCall() {
			if (serviceListener != null)
				serviceListener.tryingNewOutgoingCallButAlreadyInCall();
		}

		public void tryingNewOutgoingCallButCannotGetCallParameters() {
			if (serviceListener != null)
				serviceListener.tryingNewOutgoingCallButCannotGetCallParameters();
		}

		public void tryingNewOutgoingCallButWrongDestinationAddress() {
			if (serviceListener != null)
				serviceListener.tryingNewOutgoingCallButWrongDestinationAddress();
		}
	}// end of ListenerDispatcher

	public static final boolean isInstanciated() {
		return instance != null;
	}

	public synchronized LinphoneCall getPendingIncomingCall() {
		LinphoneCall currentCall = mLc.getCurrentCall();
		if (currentCall == null)
			return null;

		LinphoneCall.State state = currentCall.getState();
		boolean incomingPending = currentCall.getDirection() == CallDirection.Incoming
				&& (state == State.IncomingReceived || state == State.CallIncomingEarlyMedia);

		return incomingPending ? currentCall : null;
	}

	@SuppressWarnings("serial")
	public static class LinphoneConfigException extends LinphoneException {

		public LinphoneConfigException() {
			super();
		}

		public LinphoneConfigException(String detailMessage, Throwable throwable) {
			super(detailMessage, throwable);
		}

		public LinphoneConfigException(String detailMessage) {
			super(detailMessage);
		}

		public LinphoneConfigException(Throwable throwable) {
			super(throwable);
		}
	}

	@Override
	public void notifyReceived(LinphoneCore lc, LinphoneCall call, LinphoneAddress from, byte[] event) {
	}

	// add by sima NetWorkInfo
	public static NetworkInfo getActiveNetwork(Context context) {
		if (context == null)
			return null;
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm == null)
			return null;
		NetworkInfo aActiveInfo = cm.getActiveNetworkInfo();
		return aActiveInfo;
	}

	/**
	 * Add by wuhua 根据角色Indoor，BlockDoor，Guard选定不同的铃音
	 * 
	 * @param mCall
	 */
	public void ringByRole(LinphoneCall mCall) {
		String blockDoorLevelString = devInfoSp.getString("bdLevel", "1");
		String inDoorLevelString = devInfoSp.getString("indoorLevel", "2");
		String guardLevelString = devInfoSp.getString("guardLevel", "3");
		String inComingIP = mCall.getRemoteAddress().getDomain();
		String[] arrIP = (inComingIP == null) ? null : inComingIP.split("\\.");
		// arrIP[2] = "5";//很多IP没有办法测试,这里修改IP进行模拟测试
		if (inComingIP != null) {
			// 根据呼进电话的IP地址决定响铃的铃音
			if (arrIP != null) {
				if ("24".equals(arrIP[2]) || "26".equals(arrIP[2])) {
					// Guard角色
					// doorMessage = "GUARD";
					chooseMusicByNum("ring3");

				} else if ("21".equals(arrIP[2]) || "29".equals(arrIP[2])) {
					// BlockDoor角色
					// doorMessage = "DOORPANEL";
					chooseMusicByNum("ring1");
				} else if ("23".equals(arrIP[2]) || "27".equals(arrIP[2]) || "1".equals(arrIP[2])
						|| "2".equals(arrIP[2]) || "3".equals(arrIP[2]) || "4".equals(arrIP[2]) || "5".equals(arrIP[2])
						|| "6".equals(arrIP[2]) || "7".equals(arrIP[2]) || "8".equals(arrIP[2]) || "9".equals(arrIP[2])
						|| "10".equals(arrIP[2]) || "11".equals(arrIP[2]) || "12".equals(arrIP[2])
						|| "13".equals(arrIP[2]) || "14".equals(arrIP[2]) || "15".equals(arrIP[2])
						|| "16".equals(arrIP[2]) || "17".equals(arrIP[2]) || "18".equals(arrIP[2])
						|| "19".equals(arrIP[2]) || "20".equals(arrIP[2])) {
					// doorMessage = "MAINDOOR";
					chooseMusicByNum("ring4");
				} else if ("22".equals(arrIP[2])) {
					// doorMessage = "MAINDOOR";
					chooseMusicByNum("ring5");
				}
			}
		}
	}

	/**
	 * Add by wuhua 根据不同角色存储的数字设置对应的铃音
	 * 
	 * @param numString
	 */
	private void chooseMusicByNum(String numString) {
		try {
			Log.e("chooseMusicByNum", "chooseMusicByNum------>" + numString);

			if ("ring1".equals(numString)) {
				String blockRingPath = devInfoSp.getString("bdLevel", "1");

				switch (Integer.parseInt(blockRingPath)) {
				case 1:
					mRingerPlayer.setDataSource(mRing_1);
					break;
				case 2:
					mRingerPlayer.setDataSource(mRing_2);
					break;
				case 3:
					mRingerPlayer.setDataSource(mRing_3);
					break;
				case 4:
					mRingerPlayer.setDataSource(mRing_4);
					break;
				case 5:
					mRingerPlayer.setDataSource(mRing_5);
					break;
				case 6:
					mRingerPlayer.setDataSource(mRingbackSoundFile);
					break;
				case 7:
					mRingerPlayer.setDataSource(mRing_7);
					break;
				case 8:
					mRingerPlayer.setDataSource(mRing_8);
					break;
				case 9:
					mRingerPlayer.setDataSource(mRing_9);
					break;

				default:
					mRingerPlayer.setDataSource(mRing_1);
					break;
				}

			} else if ("ring2".equals(numString)) {
				mRingerPlayer.setDataSource(mRing_2);// 根据角色设置不同铃音
			} else if ("ring3".equals(numString)) {
				String guardRingPath = devInfoSp.getString("guardLevel", "3");

				switch (Integer.parseInt(guardRingPath)) {
				case 1:
					mRingerPlayer.setDataSource(mRing_1);
					break;
				case 2:
					mRingerPlayer.setDataSource(mRing_2);
					break;
				case 3:
					mRingerPlayer.setDataSource(mRing_3);
					break;
				case 4:
					mRingerPlayer.setDataSource(mRing_4);
					break;
				case 5:
					mRingerPlayer.setDataSource(mRing_5);
					break;
				case 6:
					mRingerPlayer.setDataSource(mRingbackSoundFile);
					break;
				case 7:
					mRingerPlayer.setDataSource(mRing_7);
					break;
				case 8:
					mRingerPlayer.setDataSource(mRing_8);
					break;
				case 9:
					mRingerPlayer.setDataSource(mRing_9);
					break;

				default:
					mRingerPlayer.setDataSource(mRing_3);
					break;
				}

			} else if ("ring4".equals(numString)) {
				mRingerPlayer.setDataSource(mRing_4);// 根据角色设置不同铃音
			} else if ("ring5".equals(numString)) {
				mRingerPlayer.setDataSource(mRing_5);// 根据角色设置不同铃音
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}

	public static synchronized void setScreenState(boolean state) {
		// TODO Auto-generated method stub
		ScreenState = state;
	}

	public static boolean getScreenState() {
		return ScreenState;
	}

	// private static void showAlertWindow() {
	//// Context applicationContext = getApplicationContext();
	//
	// WindowManager manager =
	// (WindowManager)
	// applicationContext.getSystemService(Context.WINDOW_SERVICE);
	// LayoutParams params = new LayoutParams();
	// params.type = LayoutParams.TYPE_TOAST;
	//// params.flags = WindowManager.LayoutParams.FLAG_FULLSCREEN;
	//// params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
	// params.format = PixelFormat.RGBA_8888;
	// params.gravity = Gravity.CENTER;
	// params.x = 0;
	// params.y = 0;
	// params.width = LayoutParams.WRAP_CONTENT;
	// params.height = LayoutParams.WRAP_CONTENT;
	//
	// manager.addView(new
	// com.wits.intercom70.NotificationView(applicationContext), params);
	// }

	// public static void showMessageWindow(){
	// WindowManager manager =
	// (WindowManager)
	// applicationContext.getSystemService(Context.WINDOW_SERVICE);
	// LayoutParams params = new LayoutParams();
	// params.type = LayoutParams.TYPE_TOAST;
	// params.format = PixelFormat.RGBA_8888;
	// params.gravity = Gravity.CENTER;
	// params.x = 0;
	// params.y = 0;
	// params.width = LayoutParams.WRAP_CONTENT;
	// params.height = LayoutParams.WRAP_CONTENT;
	//
	// manager.addView(new com.wits.intercom70.MessageView(applicationContext),
	// params);
	// }

	public static void showNetworkWindow() {
		WindowManager manager = (WindowManager) applicationContext.getSystemService(Context.WINDOW_SERVICE);
		LayoutParams params = new LayoutParams();
		params.type = LayoutParams.TYPE_TOAST;
		params.format = PixelFormat.RGBA_8888;
		params.gravity = Gravity.CENTER;
		params.x = 0;
		params.y = 0;
		params.width = LayoutParams.WRAP_CONTENT;
		params.height = LayoutParams.WRAP_CONTENT;

		manager.addView(new com.wits.intercom.ShowNetworkView(applicationContext), params);
	}

	private class detectIndoorRingThread extends Thread {
		@Override
		public void run() {
			super.run();
			while (true) {
				try {
					sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					// int resultIndoorRing =
					// mLinphoneI2CInterface.IndoorButton();
					// Log.d("indoor ring"," detect indoor ring result =
					// "+resultIndoorRing);
					// if(resultIndoorRing == 0){
					// Log.d("indoor ring"," detect indoor ring start ring and
					// show icon");
					// mHandler.sendEmptyMessage(INDOOR_RING);
					// }
				}
			}

		}
	}

	private class CallThread extends Thread { // add by jimmy at 2014.5.20
		private String sip;

		public CallThread(String toSipip) {
			// TODO Auto-generated constructor stub
			sip = toSipip;
		}

		@Override
		public void run() {
			super.run();
			try {
				sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				LinphoneManager.getInstance().newOutgoingCallString("sip:" + sip, false);
			}

		}
	}

}
