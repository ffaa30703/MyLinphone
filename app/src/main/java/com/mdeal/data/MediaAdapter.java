package com.mdeal.data;

import java.util.ArrayList;
import java.util.logging.Logger;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.keep.lin.R;

public class MediaAdapter extends BaseAdapter {
	private final String TAG = MediaAdapter.class.getSimpleName();
	
	private ArrayList<Media> Medias;
	private LayoutInflater inflater;	
	private MediaDao mMediaDao;
	public Button  btnDelete;

	public MediaAdapter(Context context,ArrayList<Media> Medias){
		this.inflater = LayoutInflater.from(context);
		this.setMedias(Medias);
		mMediaDao = new MediaDao(context);
	}
	
	
	public void setMedias(ArrayList<Media> Medias) {
		if(Medias!=null)
			this.Medias = Medias;
		else
			this.Medias = new ArrayList<Media>();
	}
	
	public void removeItem(int position){
		Medias.remove(position);
		notifyDataSetChanged();
	}
	
	public void changeData(ArrayList<Media> Medias){
		setMedias(Medias);
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return Medias.size();
	}

	@Override
	public Object getItem(int position) {
		return Medias.get(position);
	}

	@Override
	public long getItemId(int position) {
		return Medias.get(position).getId();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		if(convertView==null){
			convertView = inflater.inflate(R.layout.media_row	, null);
			holder = new ViewHolder();
			holder.mediaNameView = (TextView)convertView.findViewById(R.id.mediaName);
			btnDelete = (Button)convertView.findViewById(R.id.bt_delete);
			btnDelete.setOnClickListener(new lvButtonListener(position));
					
			convertView.setTag(holder);
		}else{
			holder = (ViewHolder)convertView.getTag();
		}	
		Media media = Medias.get(position);		
		String path = media.getPath();
		
		
		
		if(path!=null){
//			holder.mediaNameView.setText(path.substring(path.lastIndexOf("/")+1, path.lastIndexOf(".")));
			int nameIndex = path.lastIndexOf("/");
			String temFileNameString = path.substring(nameIndex+1);
			holder.mediaNameView.setText(temFileNameString);
		}			
		return convertView;
	}

   class lvButtonListener implements OnClickListener {
	        private int position;
	        lvButtonListener(int pos) {
	            position = pos;
	        }
	        @Override
	        public void onClick(View v) {
	            int vid=v.getId();
	            if (vid == btnDelete.getId()){
//		              int result = mMediaDao.removeMedia(position+1);
	            	  int result = mMediaDao.removeMedia(Medias.get(position).getPath());
			          removeItem(position); 
		            }             
	        }
	    }
	   
	class ViewHolder{
		TextView mediaNameView;	
	}
}
