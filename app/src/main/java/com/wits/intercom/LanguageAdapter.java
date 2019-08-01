package com.wits.intercom;

import java.util.ArrayList;

import com.keep.lin.R;
import com.mdeal.data.Media;


import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class LanguageAdapter extends BaseAdapter {
	private Context mContext;
	private ArrayList<String> mArrayList;
	private LayoutInflater layoutInflater;
	private int currentPosition;

	public LanguageAdapter(Context context, ArrayList<String> list) {
		mContext = context;
		mArrayList = list;
		
	}

	public void setCurrentItem(int pos){
		currentPosition = pos;
		
		Log.d("item ", "set current item = "+pos);
	}
	
	@Override
	public int getCount() {
		if (mArrayList == null) {
			return 0;
		} else {
			return mArrayList.size();
		}
	}

	@Override
	public Object getItem(int arg0) {
		if (mArrayList == null && mArrayList.size() != 0) {

			return mArrayList.get(arg0);

		} else {

			return null;
		}
	}

	@Override
	public long getItemId(int arg0) {
		// TODO Auto-generated method stub
		return arg0;
	}

	@Override
	public View getView(int arg0, View conView, ViewGroup arg2) {
		
		if (conView == null) {
			layoutInflater = LayoutInflater.from(mContext);
			conView = layoutInflater.inflate(R.layout.language_selecte_items, null);
		} 
		if(mArrayList != null){
			String language = mArrayList.get(arg0);
			TextView languageName = (TextView) conView
					.findViewById(R.id.language_name);
			languageName.setText(language);
			if(currentPosition == arg0){
				Log.d("item ", "selected");
				languageName.setTextSize(35);
			}else{
				languageName.setTextSize(30);
			}
		}
		return conView;
	}

	public void notifySetDataChange() {
		// TODO Auto-generated method stub
		super.notifyDataSetChanged();
	}
	


}
