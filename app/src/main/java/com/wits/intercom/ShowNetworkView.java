package com.wits.intercom;

import org.linphone.mediastream.Log;

import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.AbsoluteLayout;
import android.widget.LinearLayout;

import com.keep.lin.R;

public class ShowNetworkView extends AbsoluteLayout {
	private int CLOSE = 1;
	private final WindowManager mWindowManager;
	    
	public Handler mHandler = new Handler(){
	
			public void dispatchMessage(android.os.Message msg) {
				Log.w(Constance.LOGTAG, "get message");
	
				if (msg.what == CLOSE) {
					mWindowManager.removeView(ShowNetworkView.this);
				}
			}
	};
	
	public ShowNetworkView(Context context) {
        super(context);

        mWindowManager = (WindowManager) context.getApplicationContext()
                .getSystemService(Context.WINDOW_SERVICE);

        updateView();

    }
	
	private void updateView() {

        LayoutInflater.from(getContext()).inflate(R.layout.show_network_connectivity, this, true);
        mHandler.sendEmptyMessageDelayed(CLOSE, 2000);
    
	}

}

