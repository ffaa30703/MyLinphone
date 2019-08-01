/*
LinphoneLauncherActivity.java
Copyright (C) 2011  Belledonne Communications, Grenoble, France

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
package com.wits.intercom;

import static android.content.Intent.ACTION_MAIN;

import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Locale;

import org.linphone.LinphoneManager;
import org.linphone.LinphoneService;
import org.linphone.core.LinphoneCoreFactoryImpl;
import org.linphone.ui.witsui.Tools;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.TextView;

import com.android.flypigeon.service.MainService;
import com.keep.lin.R;
import com.wits.intercom.DeladyProcess.IdelayMethodrd;
import com.wits.intercom.ReadyToCallActivity;
import com.wits.intercom.dialing.EditToCallActivity;
import com.wits.intercom.setting.SettingActivity;
import com.wits.linphone.LinphoneI2CInterface;

/**
 * Launch Linphone main activity when Service is ready.
 *
 * @author Guillaume Beraudo
 */
public class LinphoneWelcomeActivity extends FragmentActivity implements
        IdelayMethodrd, OnTouchListener {
    private Handler mHandler;
    private ServiceWaitThread mThread;
    private TextView uptownName, fistRoomName, lastRoomName, timeTextView;
    private Button btStartCallPage;
    private String blockNo, flatNo, sitName;
    private Context mContext;
    private Class<? extends Activity> classToStart;
    private String devIpString;
    private int nine_down = 0;
    private LinphoneI2CInterface mLinphoneI2CInterface;

    private SharedPreferences mPreferences;
    private SharedPreferences sp;
    private Editor editor;
    private int[] dataArry;
    private final int REFLUSH_TIME = 11;
    private int mYear, mMonth, mDay, mHour, mMinunt;

    private boolean firstBoot;

    private Handler mAlarmHandler = new Handler() {
        public void dispatchMessage(android.os.Message msg) {
            Log.w(Constance.LOGTAG, "get message");
            if (msg.what == REFLUSH_TIME) {
                mYear = dataArry[4];
                String mYearForString = orderTime(mYear);
                String temYearString = "20" + mYearForString;
                mYear = Integer.parseInt(temYearString);

                mMonth = dataArry[3];
                mDay = dataArry[2];

                mHour = dataArry[0];
                mMinunt = dataArry[1];


                Log.d("get time from I2C", "time is " + mYear + "/" + mMonth + "/" + mDay + "  " + mHour + ":" + mMinunt);

                mMonth -= 1;      // month range is 0-11 in Time class

                LinphoneManager.setTimeChangeFlag(true);

                Time mTime = new Time();
                mTime.set(0, mMinunt, mHour, mDay, mMonth, mYear);
                SystemClock.setCurrentTimeMillis(mTime.toMillis(false));

//				Intent timeChangeIntent = new Intent("android.intent.action.SET_SYSTEM_TIME_I2C");
//				timeChangeIntent.putExtra("year", mYear);
//				timeChangeIntent.putExtra("month", mMonth);
//				timeChangeIntent.putExtra("day", mDay);
//				timeChangeIntent.putExtra("hour", mHour);
//				timeChangeIntent.putExtra("minute", mMinunt);
//				sendBroadcast(timeChangeIntent);               
            }
        }

        ;
    };
    private Button btCallIp;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wits_activity_welcome);
        mContext = LinphoneWelcomeActivity.this;
        findViews();
        dataArry = new int[5];
        mPreferences = getSharedPreferences("deviceInfo", 0);
        sp = getSharedPreferences("bootInfo", 0);
        editor = sp.edit();
        firstBoot = true;
        try {
            devIpString = getLocalIPAddress();
            Log.d("System", "dev ip = " + devIpString);
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        mHandler = new Handler();
        LinphoneCoreFactoryImpl.init();

    }

    @Override
    protected void onStart() {
        super.onStart();

        // 如果count大于0就跳往下一个页面否则关闭屏幕

        mLinphoneI2CInterface = new LinphoneI2CInterface();

        if (firstBoot) {
            firstBoot = false;
            editor.putBoolean("firstStarted", false);
            editor.commit();

            int errNumber = 1;
            int setResult = mLinphoneI2CInterface.SetAskTime();
            Log.d("set ask time from i2c", "set ask time result==" + setResult + "try times=" + errNumber);
            while (setResult == -1 && errNumber < 10) {
                setResult = mLinphoneI2CInterface.SetAskTime();
                errNumber++;
                Log.d("set ask time from i2c", "set ask time result==" + setResult + "try times=" + errNumber);
            }

            new getTimeThread().start();

        }

        new startServuceThread().start();


    }

    private void findViews() {
        Log.w("zg", "LinphoneLauncherActivity findview starts");

        uptownName = (TextView) findViewById(R.id.upton_name);
        fistRoomName = (TextView) findViewById(R.id.room_first_name);
        lastRoomName = (TextView) findViewById(R.id.room_last_name);
        timeTextView = (TextView) findViewById(R.id.time_textview);
        btStartCallPage = (Button) findViewById(R.id.bt_call_page);
        btStartCallPage.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                startCallPageActivity();
            }
        });
        btCallIp = (Button) findViewById(R.id.bt_call_ip);
        btCallIp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                callIp();
            }
        });

        Log.w("zg", "LinphoneLauncherActivity findview end");
    }

    protected void onServiceReady() {
        classToStart = ReadyToCallActivity.class;
        LinphoneService.instance().setActivityToLaunchOnIncomingReceived(classToStart);
    }

    @Override
    protected void onResume() {
        super.onResume();
        blockNo = mPreferences.getString("blockNo", "A");
        flatNo = mPreferences.getString("apartment_number", "001");
        sitName = mPreferences.getString("apartment_name", "Multitek");
//		if (blockNo == null || "".equals(blockNo)) {
//			blockNo = Tools.getBlock(mContext);
//		}
        fistRoomName.setText(blockNo);

//		if (flatNo == null || "".equals(flatNo)) {
//			flatNo = Tools.getFlat(mContext);
//		}
        lastRoomName.setText(flatNo);

//		if (sitName == null || "".equals(sitName)) {
//			sitName = Tools.getSitName(mContext);
//		}
        uptownName.setText(sitName); //住宅区名称
//		DeladyProcess.doDelay();                                      //should close after debug
//		DeladyProcess.getInstance(mHandler, 2000, this);
        LinphoneManager.isInMainPage = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        LinphoneManager.isInMainPage = false;
    }

    @Override
    public void finish() {
        Log.w("zg", "=====do finish====");
//		DeladyProcess.stopDelayThread();
        super.finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
//	    DeladyProcess.stopDelayThread(); // changed at 20140703
    }

    @Override
    protected void onDestroy() {
//		DeladyProcess.stopDelayThread();
//		stopService(new Intent(ACTION_MAIN).setClass(this,
//				LinphoneService.class));
//		stopService(new Intent(ACTION_MAIN).setClass(this,
//				MainService.class));
        super.onDestroy();
    }


    private class ServiceWaitThread extends Thread {
        public void run() {
            while (!LinphoneService.isReady()) {
                try {
                    sleep(30);
                } catch (InterruptedException e) {
                    throw new RuntimeException(
                            "waiting thread sleep() has been interrupted");
                }
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    onServiceReady();
                }
            });
            mThread = null;
        }
    }

    @Override
    public void delayProcess() {
        Intent intent = new Intent(LinphoneWelcomeActivity.this, ReadyToCallActivity.class);
        startActivity(intent);

        //测试用例
//		startActivity(new Intent(LinphoneWelcomeActivity.this, EditToCallActivity.class));
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // TODO Auto-generated method stub
        // 判断普通按键
        int keyCode = event.getKeyCode();
        if (event.getAction() == KeyEvent.ACTION_DOWN) {

            Log.d("Key Event", "Key code = " + keyCode);
            if (keyCode == 19) {
                startCallPageActivity();
                return true;

            } else if (keyCode == 20) {
                nine_down = 0;
                LinphoneManager.isShowingInputActivity = true;
                LinphoneManager.isShowingInputActivity = true;
                Intent mPhoneBookIntent = new Intent(this, PhoneBookActivity.class);
                startActivity(mPhoneBookIntent);
                return true;

            } else if (keyCode == 21) {
                callIp();
                return true;

            } else if (keyCode == 22) {
                nine_down = 0;
                LinphoneManager.isShowingInputActivity = true;
                Intent mOpenDoorIntent = new Intent(this, PasswordActivity.class);
                mOpenDoorIntent.putExtra("type", "door");
                startActivity(mOpenDoorIntent);
                return true;

            } else if (keyCode == 66) {
//			   nine_down = 0;
//			   Intent intent = new Intent(this,
//						EditToCallActivity.class);
//				startActivity(intent);

                return true;

            } else if (keyCode >= 7 && keyCode <= 16) {
                nine_down = 0;
                Intent intent = new Intent(this,
                        EditToCallActivity.class);
                intent.putExtra("keyCode", keyCode);
                startActivity(intent);

                return true;
            } else if (keyCode == 67) {
                nine_down = 0;
//			   Intent intent = new Intent(this,
//						SettingActivity.class);
//				startActivity(intent);

                return true;

            } else if (keyCode == 57) {
                nine_down = 0;
                Intent intent = new Intent(this, ReadyToCallActivity.class);
                startActivity(intent);
                return true;
            }
        }
        return super.dispatchKeyEvent(event);

    }

    private void callIp() {
        nine_down = 0;
        LinphoneManager.isShowingInputActivity = true;
        initCallInfor(1, "10.99.26.1");
        Intent mCallIntent = new Intent(this, CallPageActivity.class);

        mCallIntent.putExtra("roomNumber", "901");
        startActivity(mCallIntent);
    }

    private void startCallPageActivity() {
        nine_down = 0;
        LinphoneManager.isShowingInputActivity = true;
        String callSip = getCallSip("841");
        Log.d("Conversation", "call sip =  " + callSip);
        initCallInfor(1, callSip);
        Intent mCallIntent = new Intent(this, CallPageActivity.class);

        mCallIntent.putExtra("roomNumber", "841");
        startActivity(mCallIntent);
    }

    private String getCallSip(String inputRoomNumber) {
        // TODO Auto-generated method stub
        String ipString = "10";
        String thirdPartString = "1";
        String forthPartString = "1";
        if (inputRoomNumber == null && devIpString == null) {
            return null;
        } else {
            Log.d("parse call sip", "input room number= " + inputRoomNumber);

            int room = Integer.parseInt(inputRoomNumber);
            if (room >= 1 && room <= 254) {
                thirdPartString = "1";
                forthPartString = room + "";
            } else if (room >= 255 && room <= 508) {
                thirdPartString = "2";
                forthPartString = (room - 254) + "";
            } else if (room >= 509 && room <= 762) {
                thirdPartString = "3";
                forthPartString = (room - 254 * 2) + "";
            } else if (room >= 763 && room <= 799) {
                thirdPartString = "4";
                forthPartString = (room - 254 * 3) + "";
            } else if (room == 841) {
                thirdPartString = "23";
                forthPartString = "1";
            }

            String[] arr = devIpString.split("\\.");      //changed by jimmy at 2014.5.4
            if (arr != null) {
                for (int i = 1; i < arr.length; i++) {
                    if (i == 1) {
                        ipString += "." + arr[i];
                    } else if ((i == 2)) {
                        ipString += "." + thirdPartString;
                    } else if ((i == 3)) {
                        ipString += "." + forthPartString;
                    }
                }
            }

            Log.d("get Indoor Ip", "this devive indoor ip is " + ipString);

            return ipString;
        }
    }

    private String getLocalIPAddress() throws SocketException {
        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
            NetworkInterface intf = en.nextElement();
            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                InetAddress inetAddress = enumIpAddr.nextElement();
                if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                    return inetAddress.getHostAddress().toString();
                }
            }
        }
        return "null";
    }

    private CallInfor initCallInfor(int role, String sipString) {
        CallInfor callInfor = CallInfor.getInstance();
//		callInfor.sipip=sipString;
//		callInfor.role=role;
        CallInfor.setSipip(sipString);
        CallInfor.setRole(role);

        return callInfor;
    }

    public void chooseLanguage(Locale locale) {

        Resources resources = getResources();//获得res资源对象

        Configuration config = resources.getConfiguration();//获得设置对象

        DisplayMetrics dm = resources.getDisplayMetrics();//获得屏幕参数：主要是分辨率，像素等。

        config.locale = Locale.CHINA; //语言
        Locale.setDefault(Locale.CHINA);
        resources.updateConfiguration(config, dm);
        Log.d("language", "change local");

    }

    @SuppressWarnings("unchecked")
    private void updateLanguage(Locale locale) {
        Log.d("ANDROID_LAB", locale.toString());
        try {
            Object objIActMag, objActMagNative;
            Class clzIActMag = Class.forName("android.app.IActivityManager");
            Class clzActMagNative = Class.forName("android.app.ActivityManagerNative");
            Method mtdActMagNative$getDefault = clzActMagNative.getDeclaredMethod("getDefault");
            // IActivityManager iActMag = ActivityManagerNative.getDefault();
            objIActMag = mtdActMagNative$getDefault.invoke(clzActMagNative);
            // Configuration config = iActMag.getConfiguration();
            Method mtdIActMag$getConfiguration = clzIActMag.getDeclaredMethod("getConfiguration");
            Configuration config = (Configuration) mtdIActMag$getConfiguration.invoke(objIActMag);
            config.locale = locale;
            // iActMag.updateConfiguration(config);
            // 此处需要声明权限:android.permission.CHANGE_CONFIGURATION
            // 会重新调用 onCreate();
            Class[] clzParams = {Configuration.class};
            Method mtdIActMag$updateConfiguration = clzIActMag.getDeclaredMethod(
                    "updateConfiguration", clzParams);
            mtdIActMag$updateConfiguration.invoke(objIActMag, config);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class getTimeThread extends Thread {
        @Override
        public void run() {
            // TODO Auto-generated method stub
            try {
                Thread.sleep(500);

                int errNumber = 1;

                int resultGetTime = mLinphoneI2CInterface.GetTime(dataArry);
                Log.d("get time from i2c", "get time result==" + resultGetTime);

                while (resultGetTime == -1 && errNumber < 10) {
                    resultGetTime = mLinphoneI2CInterface.GetTime(dataArry);
                    errNumber++;
                    Log.d("get time from i2c", "get time result==" + resultGetTime + "try times=" + errNumber);
                }
                if (resultGetTime == 0) {
                    mAlarmHandler.sendEmptyMessage(REFLUSH_TIME);
                }

                int[] dataTem = new int[1];
                int resultTem = mLinphoneI2CInterface.GetTemperature(dataTem);
                errNumber = 1;
                while (resultTem == -1 && errNumber < 10) {
                    resultTem = mLinphoneI2CInterface.GetTemperature(dataTem);
                    errNumber++;
                }
                if (resultTem != -1) {
                    LinphoneManager.setTemperature(dataTem);
                }

            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }

    class startServuceThread extends Thread {
        @Override
        public void run() {
            // TODO Auto-generated method stub
            try {
                Thread.sleep(2000);
                if (LinphoneService.isReady()) {
                    Log.d("Linphone40", "service is ready");
                    onServiceReady();
                } else {
                    // start org.linphone as background
                    startService(new Intent(ACTION_MAIN).setClass(LinphoneWelcomeActivity.this,
                            LinphoneService.class));
                    startService(new Intent(ACTION_MAIN).setClass(LinphoneWelcomeActivity.this,
                            MainService.class));
                    mThread = new ServiceWaitThread();
                    mThread.start();
                }

            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }

    private String orderTime(int time) {
        String value = String.valueOf(time);
        if (time < 10) {
            return "0" + value;
        } else {
            return value;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // TODO Auto-generated method stub
        finish();
        return false;
    }


}
