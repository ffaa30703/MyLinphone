package com.wits.intercom.dialing;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.keep.lin.R;
import com.wits.intercom.DeladyProcess;
import com.wits.intercom.DeladyProcess.IdelayMethodrd;
import com.wits.intercom.LinphoneWelcomeActivity;
import com.wits.intercom.MainFunctionsActivity;


public class InvalidRoomShowActivity extends Activity implements IdelayMethodrd{
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wits_dailing_invalid_room_show);
	}
	
	@Override
	protected void onStart() {	
		super.onStart();
		DeladyProcess.doDelay();
		DeladyProcess.getInstance(new Handler(), 1000, this);				
	}
	
	@Override
	public void delayProcess() {
		finish();
//		startActivity(new Intent(InvalidRoomShowActivity.this,EditToCallActivity.class));			
	}
}
