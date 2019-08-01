package org.linphone.ui.witsui;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

public class TimeHandle extends Handler  {
	private ISetCurrentTime mISetCurrentTime;
	@Override
	public void handleMessage(Message msg) {
		super.handleMessage(msg);
		switch (msg.what) {
		case Constant.TIME_CHANGE_MESSAGE:
			mISetCurrentTime.setCurrentTime();
			break;
		default:
			break;
		}
	}

	public TimeHandle(ISetCurrentTime setCurrentTime){
		mISetCurrentTime=setCurrentTime;
	}
}
