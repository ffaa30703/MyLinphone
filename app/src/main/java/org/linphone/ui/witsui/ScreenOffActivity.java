package org.linphone.ui.witsui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;

import com.keep.lin.R;
import com.wits.intercom.LinphoneWelcomeActivity;

/**
 * 
 * @author chris
 */
public class ScreenOffActivity extends Activity  {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wits_activity_scrennoff);
		LinearLayout layout=(LinearLayout)findViewById(R.id.screen_off_layout);
		layout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent=new Intent(ScreenOffActivity.this,LinphoneWelcomeActivity.class);
				startActivity(intent);	
			}
		});
	}

}
