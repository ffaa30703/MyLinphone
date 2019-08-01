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
package org.linphone;

import static android.content.Intent.ACTION_MAIN;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.TextView;


/**
 * 
 * Launch Linphone main activity when Service is ready.
 * 
 * @author Guillaume Beraudo
 * 
 */
public class LinphoneLauncherActivity extends FragmentActivity  {
	private Handler mHandler;
	private ServiceWaitThread mThread;
	private TextView uptownName, fistRoomName, lastRoomName, timeTextView;
	private String blockNo, flatNo, sitName;
	private Context mContext;
	private Class<? extends Activity> classToStart;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		setContentView(R.layout.wits_activity_welcome);
		mContext = LinphoneLauncherActivity.this;
		findViews();
		mHandler = new Handler();
	}

	@Override
	protected void onStart() {
		super.onStart();
//		if (blockNo == null || "".equals(blockNo)) {
//			blockNo = Tools.getBlock(mContext);
//		}
//		fistRoomName.setText(blockNo);
//		if (flatNo == null || "".equals(flatNo)) {
//			flatNo = Tools.getFlat(mContext);
//		}
//		lastRoomName.setText(flatNo);
//		if (sitName == null || "".equals(sitName)) {
//			sitName = Tools.getSitName(mContext);
//		}
//		uptownName.setText(sitName);
		// 如果count大于0就跳往下一个页面否则关闭屏幕

		if (LinphoneService.isReady()) {
			onServiceReady();
		} else {
			// start linphone as background
			startService(new Intent(ACTION_MAIN).setClass(this,
					LinphoneService.class));
			mThread = new ServiceWaitThread();
			mThread.start();
		}
	}

	private void findViews() {
		Log.w("zg", "LinphoneLauncherActivity findview starts");
		
		
//		uptownName = (TextView) findViewById(R.id.upton_name);
//		fistRoomName = (TextView) findViewById(R.id.room_first_name);
//		lastRoomName = (TextView) findViewById(R.id.room_last_name);
//		timeTextView = (TextView) findViewById(R.id.time_textview);
		
		
		Log.w("zg", "LinphoneLauncherActivity findview end");
	}

	protected void onServiceReady() {
//		classToStart = ReadyToCallActivity.class;
//		LinphoneService.instance().setActivityToLaunchOnIncomingReceived(
//				classToStart);
//		DeladyProcess.doDelay();
//		DeladyProcess.getInstance(mHandler, 1000, this);
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
		// DeladyProcess.stopDelayThread();
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



}
