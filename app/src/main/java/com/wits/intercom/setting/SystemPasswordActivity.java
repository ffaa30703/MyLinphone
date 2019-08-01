package com.wits.intercom.setting;

import com.keep.lin.R;
import com.wits.intercom.MyApplication;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;

public class SystemPasswordActivity extends FragmentActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_system_password);
	}

	@Override  
	public boolean dispatchKeyEvent(KeyEvent event) {  
	   // TODO Auto-generated method stub  
	   // 判断普通按键  
	   int keyCode = event.getKeyCode();
	   if(event.getAction() == KeyEvent.ACTION_DOWN){
		   
		   Log.d("Key Event", "Key code = "+keyCode);
		  if(keyCode == 19){                       //left up
			   
			   return true;
			   
		   }else if(keyCode == 20){            //left down
			   finish();
			   return true;
			   
		   }else if(keyCode == 21){          //right up
			   
			   return true;
			   
		   }else if(keyCode == 22){        //right down
			   Intent mSetupSystemPasswordIntent = new Intent(this,UserInputActivity.class);
			   mSetupSystemPasswordIntent.putExtra("type", "SET_SYSTEM_PASSWORD");
			   startActivity(mSetupSystemPasswordIntent);
			   return true;
			   
		   }
	   }
	   return super.dispatchKeyEvent(event);
	   
	} 


}
