package org.linphone.ui.witsui;

import java.util.ArrayList;




import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView.FindListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.PopupWindow;

import com.keep.lin.R;

public class ZgPrintinView extends PopupWindow implements OnItemClickListener,OnClickListener{
	private Context mContext;
	private View mContentView;
	private ArrayList<ZgpringinText> zgPrintinList;
	private LayoutInflater mInflater;
	private EditText showPrintInText;
	private ZgPrintinView instatnce;
	private StringBuffer printStringBuffer;
	private String printIn;
	
	
	public String getPrintIn() {
		return printIn;
	}


	public void setPrintIn(String printIn) {
		this.printIn = printIn;
	}


	public ZgPrintinView(Context co,View view){
		super(view, 800, 420);
		Log.i("zg", "ZgPrintinView");
		mContext=co;
		mInflater=LayoutInflater.from(mContext);
	    mContentView=mInflater.inflate(R.layout.zgprintin, null);
	    this.setContentView(mContentView);
	    
	    this.showPrintInText=(EditText) mContentView.findViewById(R.id.show_printin);
	    this.setFocusable(true);
	    instatnce=this;
	    printStringBuffer=new StringBuffer();
	    setZgList();
	    GridView gridView=(GridView)mContentView.findViewById(R.id.printin_gridview);
	    gridView.setAdapter(new PrintInAdapter());
	    gridView.setOnItemClickListener(this);
	    
//	    Button OkButton=(Button)mContentView.findViewById(R.id.ok_button);
//	    OkButton.setOnClickListener(this);
	    
	}

	private void setZgList() {
		if(null==zgPrintinList||zgPrintinList.size()==0){
			zgPrintinList=new ArrayList<ZgpringinText>();
			addZgList(zgPrintinList,"1");
			addZgList(zgPrintinList,"2");
			addZgList(zgPrintinList,"3");
			addZgList(zgPrintinList,"4");
			addZgList(zgPrintinList,"5");
			addZgList(zgPrintinList,"6");
			addZgList(zgPrintinList,"7");
			addZgList(zgPrintinList,"8");
			addZgList(zgPrintinList,"9");
			addZgList(zgPrintinList,"0");
			addZgList(zgPrintinList,"space");
			addZgList(zgPrintinList,".");
			addZgList(zgPrintinList,"dismiss");
			addZgList(zgPrintinList,"ok");
			addZgList(zgPrintinList,"del");
		
		}
	}
	private void addZgList(ArrayList<ZgpringinText> zgList,String text){
		ZgpringinText zgText=new ZgpringinText();
		zgText.showText=text;
		zgList.add(zgText);
	}



	class PrintInAdapter extends BaseAdapter{
		
		@Override
		public int getCount() {
			Log.i("zg", "zgPrintinList.size()===>"+zgPrintinList.size());
			return zgPrintinList.size();
		}

		@Override
		public Object getItem(int arg0) {
			Log.i("zg", "zgPrintinList.get(arg0)==>"+zgPrintinList.get(arg0));
			return zgPrintinList.get(arg0);
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHold viewHold;
			if(convertView==null){
				viewHold=new ViewHold();
				convertView	=mInflater.inflate(R.layout.zgprintin_item, null);
				viewHold.printinButton=(Button)convertView.findViewById(R.id.printin_button);
				convertView.setTag(viewHold);
			}else{
				viewHold=(ViewHold) convertView.getTag();
			}
			viewHold.printinButton.setText(zgPrintinList.get(position).showText);
			return convertView;
		}
		
		class ViewHold{
			Button printinButton;
		}
		
	}



	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		
		if(arg2==10){
			printStringBuffer.append(" ");
			showPrintInText.setText(printStringBuffer.toString());
		}else if(arg2==11){
			printStringBuffer.append(".");
			showPrintInText.setText(printStringBuffer.toString());
		}else if(arg2==12){
			printStringBuffer.delete(0, printStringBuffer.length());
			showPrintInText.setText("");
			instatnce.dismiss();
		}else if(arg2==13){
			setPrintIn(showPrintInText.getText().toString());
			instatnce.dismiss();
		}else if(arg2==14){
			printStringBuffer.delete(0, printStringBuffer.length());
			showPrintInText.setText("");
		}else{
			printStringBuffer.append(zgPrintinList.get(arg2).showText);
			showPrintInText.setText(printStringBuffer.toString());
		}

	}


	@Override
	public void onClick(View v) {
//		switch (v.getId()) {
//		case R.id.ok_button:
//		
//			break;
//
//		default:
//			break;
//		}
		
	}
}
