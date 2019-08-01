package com.android.flypigeon.home;

import java.util.ArrayList;
import java.util.Map;

import com.android.flypigeon.service.MainService;
import com.android.flypigeon.util.Constant;
import com.android.flypigeon.util.FileName;
import com.android.flypigeon.util.FileState;
import com.android.flypigeon.util.Person;
import com.keep.lin.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class FlyPigeonMainActivity extends Activity implements View.OnClickListener{
	private ExpandableListView ev = null;
	private String[] groupIndicatorLabeles = null;
	private SettingDialog settingDialog = null;
	private MyBroadcastRecv broadcastRecv = null;
	private IntentFilter bFilter = null;
	private ArrayList<Map<Integer,Person>> children = null;
	private ArrayList<Integer> personKeys = null;
	private MainService mService = null;
	private Intent mMainServiceIntent = null;
	private ExListAdapter adapter = null;
	private Person me = null;
	private Person person = null;
	private AlertDialog dialog = null;
	private boolean isPaused = false;//�жϱ����ǲ��ǿɼ�
	private boolean isRemoteUserClosed = false; //�Ƿ�Զ���û��Ѿ��ر���ͨ���� 
	private ArrayList<FileState> receivedFileNames = null;//���յ��ĶԷ����������ļ���
	private ArrayList<FileState> beSendFileNames = null;//���͵��Է����ļ�����Ϣ
	/**
	 * MainService�����뵱ǰActivity�İ�������
	 */
	private ServiceConnection sConnection = new ServiceConnection(){
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = ((MainService.ServiceBinder)service).getService();
			System.out.println("Service connected to activity...");
		}
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
			System.out.println("Service disconnected to activity...");
		}
	};
	
	private ReceiveSendFileListAdapter receiveFileListAdapter = new ReceiveSendFileListAdapter(this);
	private ReceiveSendFileListAdapter sendFileListAdapter = new ReceiveSendFileListAdapter(this);
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fly_bird_main);
        groupIndicatorLabeles = getResources().getStringArray(R.array.groupIndicatorLabeles);
        
        //��ǰActivity���̨MainService���а�
        mMainServiceIntent = new Intent(this,MainService.class);
        bindService(mMainServiceIntent, sConnection, BIND_AUTO_CREATE);
        startService(mMainServiceIntent);
        
        ev = (ExpandableListView)findViewById(R.id.main_list);
        regBroadcastRecv();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.main_menu, menu);
    	return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getOrder()){
    	case 1:
    		showSettingDialog();
    		break;
    	case 2:
    		break;
    	}
    	return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	isPaused = false;
    	getMyInfomation();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	isPaused = true;
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	unregisterReceiver(broadcastRecv);
    	stopService(mMainServiceIntent);
		unbindService(sConnection);
    }    
//==============================ExpandableListView���������===================================
    private class ExListAdapter extends BaseExpandableListAdapter implements OnLongClickListener{
    	private Context context = null;
    	
    	public ExListAdapter(Context context){
    		this.context = context;
    	}

        //���ĳ���û�����
		@Override
		public Object getChild(int groupPosition, int childPosition) {
			return children.get(groupPosition).get(personKeys.get(childPosition));
		}
		//����û����û��б��е����
		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return personKeys.get(childPosition);
		}
		//����û�����View
		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,ViewGroup parentView) {
			View view = null;
			if(groupPosition<children.size()){//���groupPosition������ܴ�children�б��л��һ��children����
				Person person = children.get(groupPosition).get(personKeys.get(childPosition));//��õ�ǰ�û�ʵ��
				view = getLayoutInflater().inflate(R.layout.person_item_layout, null);//���List�û���Ŀ���ֶ���
				view.setOnLongClickListener(this);//��ӳ����¼�
				view.setOnClickListener(FlyPigeonMainActivity.this);
				view.setTag(person);//���һ��tag����Ա��ڳ����¼��͵���¼��и�ݸñ�ǽ�����ش���
				view.setPadding(30, 0, 0, 0);//����������հ׾���
				ImageView headIconView = (ImageView)view.findViewById(R.id.person_head_icon);//ͷ��
				TextView nickeNameView = (TextView)view.findViewById(R.id.person_nickename);//�ǳ�
				TextView loginTimeView = (TextView)view.findViewById(R.id.person_login_time);//��¼ʱ��
				TextView msgCountView = (TextView)view.findViewById(R.id.person_msg_count);//δ����Ϣ����
			//	TextView ipaddressView = (TextView)view.findViewById(R.id.person_ipaddress);//IP��ַ
				headIconView.setImageResource(person.personHeadIconId);
				nickeNameView.setText(person.personNickeName);
				loginTimeView.setText(person.loginTime);
				String msgCountStr = getString(R.string.init_msg_count);
				//����û�id��service���ø��û�����Ϣ����
				msgCountView.setText(String.format(msgCountStr, mService.getMessagesCountById(person.personId)));
			//	ipaddressView.setText(person.ipAddress);
			}
			return view;
		}
		//���ĳ���û����е��û���
		@Override
		public int getChildrenCount(int groupPosition) {
			int childrenCount = 0;
			if(groupPosition<children.size())childrenCount=children.get(groupPosition).size();
			return childrenCount;
		}
		//���ý���û������
		@Override
		public Object getGroup(int groupPosition) {
			return children.get(groupPosition);
		}
		//����û�������,�ô����û����������ص�������Ƶ�����
		@Override
		public int getGroupCount() {
			return groupIndicatorLabeles.length;
		}
		//����û������
		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}
		//����û��鲼��View
		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView,ViewGroup parent) {
			AbsListView.LayoutParams lp = new AbsListView.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 60);
            TextView textView = new TextView(context);
            textView.setLayoutParams(lp);
            textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
            textView.setPadding(50, 0, 0, 0);
            int childrenCount = 0;
            if(groupPosition<children.size()){//���groupPosition����ܴ�children�б��л��children�������ø�children�����е��û�����
            	childrenCount = children.get(groupPosition).size();
            }
			textView.setText(groupIndicatorLabeles[groupPosition]+"("+childrenCount+")");
			return textView;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}

		/*���û��б?��ʱ�䰴��ʱ�ᴥ�����¼�*/
		@Override
		public boolean onLongClick(View view) {
			person = (Person)view.getTag();
			AlertDialog.Builder  builder = new AlertDialog.Builder(context);
			builder.setTitle(person.personNickeName);
			builder.setMessage(R.string.pls_select_opr);
			builder.setIcon(person.personHeadIconId);
			View vi = getLayoutInflater().inflate(R.layout.person_long_click_layout, null);
			builder.setView(vi);
			dialog = builder.show();

			Button sendMsgBtn = (Button)vi.findViewById(R.id.long_send_msg);
			sendMsgBtn.setTag(person);
			sendMsgBtn.setOnClickListener(FlyPigeonMainActivity.this);
			
			Button sendFileBtn = (Button)vi.findViewById(R.id.long_send_file);
			sendFileBtn.setTag(person);
			sendFileBtn.setOnClickListener(FlyPigeonMainActivity.this);
			
			Button callBtn = (Button)vi.findViewById(R.id.long_click_call);
			callBtn.setTag(person);
			callBtn.setOnClickListener(FlyPigeonMainActivity.this);
			
			Button cancelBtn = (Button)vi.findViewById(R.id.long_click_cancel);
			cancelBtn.setTag(person);
			cancelBtn.setOnClickListener(FlyPigeonMainActivity.this);
		
			return true;
		}
    }
    //=================================ExpandableListView�������������===================================================
    
    //������ѵ������Ϣ
    private void getMyInfomation(){
    	SharedPreferences pre = PreferenceManager.getDefaultSharedPreferences(this);
    	int iconId = pre.getInt("headIconId", R.drawable.black_bird);
    	String nickeName = pre.getString("nickeName", "��������ǳ�");
    	ImageView myHeadIcon = (ImageView)findViewById(R.id.my_head_icon);
    	myHeadIcon.setImageResource(iconId);
    	TextView myNickeName = (TextView)findViewById(R.id.my_nickename);
    	myNickeName.setText(nickeName);
    	me = new Person();
    	me.personHeadIconId = iconId;
    	me.personNickeName = nickeName;
    }

	@Override
	public void onClick(View view) {
		switch(view.getId()){
		case R.id.myinfo_panel://����ϵͳ���ô���
			showSettingDialog();
			break;
		case R.id.person_item_layout://ת������Ϣҳ��
			person = (Person)view.getTag();//�û��б��childView�����ʱ
			openChartPage(person);
			break;
		case R.id.long_send_msg://�����б��childViewʱ�ڵ����Ĵ����е��"������Ϣ"��ťʱ
			person = (Person)view.getTag();
			openChartPage(person);
			if(null!=dialog)dialog.dismiss();
			break;
		case R.id.long_send_file:
			Intent intent = new Intent(this,MyFileManager.class);
			intent.putExtra("selectType", Constant.SELECT_FILES);
			startActivityForResult(intent, 0);
			dialog.dismiss();
			break;
		case R.id.long_click_call:
			person = (Person)view.getTag();
			AlertDialog.Builder  builder = new AlertDialog.Builder(this);
			builder.setTitle(me.personNickeName);
			String title = String.format(getString(R.string.talk_with), person.personNickeName);
			builder.setMessage(title);
			builder.setIcon(me.personHeadIconId);
			builder.setNegativeButton(getString(R.string.close), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface cdialog, int which) {
					cdialog.dismiss();
				}
			});
			final AlertDialog callDialog = builder.show();
			callDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface arg0) {
					mService.stopTalk(person.personId);
				}
			});
			mService.startTalk(person.personId);
			break;
		case R.id.long_click_cancel:
			dialog.dismiss();
			break;
		}
	}
	
	boolean finishedSendFile = false;//��¼��ǰ��Щ�ļ��ǲ��Ǳ����Ѿ����չ���
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == RESULT_OK){
			if(null!=data){
				int selectType = data.getExtras().getInt("selectType");
				if(selectType == Constant.SELECT_FILE_PATH){//����յ������ļ���ѡ��ģʽ��˵��������Ҫ����Է����������ļ�����ѵ�ǰѡ����ļ���·�����ط����
					String fileSavePath = data.getExtras().getString("fileSavePath");
					if(null!=fileSavePath){
						mService.receiveFiles(fileSavePath);
						finishedSendFile = true;//�ѱ��ν���״̬��Ϊtrue
						System.out.println("over save file ...");
					}else{
						Toast.makeText(this, getString(R.string.folder_can_not_write), Toast.LENGTH_SHORT).show();
					}
				}else if(selectType == Constant.SELECT_FILES){//����յ������ļ�ѡ��ģʽ��˵��������Ҫ�����ļ�����ѵ�ǰѡ��������ļ����ظ����㡣
					@SuppressWarnings("unchecked")
					final ArrayList<FileName> files = (ArrayList<FileName>)data.getExtras().get("files");
					mService.sendFiles(person.personId, files);//�ѵ�ǰѡ��������ļ����ظ�����
					
					//��ʾ�ļ������б�
					beSendFileNames = mService.getBeSendFileNames();//�ӷ������������Ҫ���յ��ļ����ļ���
					if(beSendFileNames.size()<=0)return;
					sendFileListAdapter.setResources(beSendFileNames);
					AlertDialog.Builder  builder = new AlertDialog.Builder(this);
					builder.setTitle(me.personNickeName);
					builder.setMessage(R.string.start_to_send_file);
					builder.setIcon(me.personHeadIconId);
					View vi = getLayoutInflater().inflate(R.layout.request_file_popupwindow_layout, null);
					builder.setView(vi);
					final AlertDialog fileListDialog = builder.show();
					fileListDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
						@Override
						public void onDismiss(DialogInterface arg0) {
							beSendFileNames.clear();
				        	files.clear();
						}
					});
					ListView lv = (ListView)vi.findViewById(R.id.receive_file_list);//��Ҫ���յ��ļ��嵥
					lv.setAdapter(sendFileListAdapter);
					Button btn_ok = (Button)vi.findViewById(R.id.receive_file_okbtn);
					btn_ok.setVisibility(View.GONE);
			        Button btn_cancle = (Button)vi.findViewById(R.id.receive_file_cancel);
			      //���ð�ť���������ļ�ѡ�����������ó��ļ���ѡ��ģʽ��ѡ��һ���������նԷ��ļ����ļ���
			        btn_ok.setOnClickListener(new View.OnClickListener() {   
			            @Override  
			            public void onClick(View v) { 
			            	if(!finishedSendFile){//�����ļ��Ѿ����չ������ٴ��ļ���ѡ����
				            	Intent intent = new Intent(FlyPigeonMainActivity.this,MyFileManager.class);
				    			intent.putExtra("selectType", Constant.SELECT_FILE_PATH);
				    			startActivityForResult(intent, 0);
			            	}
			            }   
				     });   
				    //���ð�ť������������㷢���û��ܾ�����ļ��Ĺ㲥       
			        btn_cancle.setOnClickListener(new View.OnClickListener() {   
				        @Override  
				        public void onClick(View v) { 
				        	fileListDialog.dismiss();
				        }   
			        });
				
				}
			}
		}
	}
	
	//��ʾ��Ϣ���öԻ���
	private void showSettingDialog(){
		if(null==settingDialog)settingDialog = new SettingDialog(this);
		settingDialog.show();
	}
    
    //=========================�㲥������==========================================================
    private class MyBroadcastRecv extends BroadcastReceiver{
    	
		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals(Constant.updateMyInformationAction)){
				getMyInfomation();
			}else if(intent.getAction().equals(Constant.dataReceiveErrorAction) 
					|| intent.getAction().equals(Constant.dataSendErrorAction)){
				Toast.makeText(FlyPigeonMainActivity.this, intent.getExtras().getString("msg"), Toast.LENGTH_SHORT).show();
			}else if(intent.getAction().equals(Constant.fileReceiveStateUpdateAction)){//�յ����Է������ļ�����״̬֪ͨ
				if(!isPaused){
					receivedFileNames = mService.getReceivedFileNames();//��õ�ǰ�����ļ�����״̬
					receiveFileListAdapter.setResources(receivedFileNames);
					receiveFileListAdapter.notifyDataSetChanged();//�����ļ������б�
				}
			}else if(intent.getAction().equals(Constant.fileSendStateUpdateAction)){//�յ����Է������ļ�����״̬֪ͨ
				if(!isPaused){
					beSendFileNames = mService.getBeSendFileNames();//��õ�ǰ�����ļ�����״̬
					sendFileListAdapter.setResources(beSendFileNames);
					sendFileListAdapter.notifyDataSetChanged();//�����ļ������б�
				}
			}else if(intent.getAction().equals(Constant.receivedTalkRequestAction)){
				if(!isPaused){
					isRemoteUserClosed = false;
					final Person psn = (Person)intent.getExtras().get("person");
					String title = String.format(getString(R.string.talk_with), psn.personNickeName);
					AlertDialog.Builder  builder = new AlertDialog.Builder(FlyPigeonMainActivity.this);
					builder.setTitle(me.personNickeName);
					builder.setMessage(title);
					builder.setIcon(me.personHeadIconId);
					View vi = getLayoutInflater().inflate(R.layout.request_talk_layout, null);
					builder.setView(vi);
					final AlertDialog revTalkDialog = builder.show();
					revTalkDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
						@Override
						public void onDismiss(DialogInterface arg0) {
							mService.stopTalk(psn.personId);
						}
					});
					Button talkOkBtn = (Button)vi.findViewById(R.id.receive_talk_okbtn);
					talkOkBtn.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View okBtn) {
							if(!isRemoteUserClosed){//���Զ���û�δ�ر�ͨ��������Է�����ͬ�����ͨ��ָ��
								mService.acceptTalk(psn.personId);
								okBtn.setEnabled(false);
							}
						}
					});
					Button talkCancelBtn = (Button)vi.findViewById(R.id.receive_talk_cancel);
					talkCancelBtn.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View cancelBtn) {
							revTalkDialog.dismiss();
						}
					});
				}
			}else if(intent.getAction().equals(Constant.remoteUserClosedTalkAction)){
				isRemoteUserClosed = true;//�����յ�Զ���û��ر�ͨ��ָ����Ѹñ����Ϊtrue
			}else if(intent.getAction().equals(Constant.remoteUserRefuseReceiveFileAction)){
				Toast.makeText(FlyPigeonMainActivity.this, getString(R.string.refuse_receive_file), Toast.LENGTH_SHORT).show();
			}else if(intent.getAction().equals(Constant.personHasChangedAction)){
				children = mService.getChildren();
				personKeys = mService.getPersonKeys();
				if(null==adapter){
					adapter = new ExListAdapter(FlyPigeonMainActivity.this);
			        ev.setAdapter(adapter);
			        ev.expandGroup(0);
			        ev.setGroupIndicator(getResources().getDrawable(R.drawable.all_bird));
		        }
		        adapter.notifyDataSetChanged();
			}else if(intent.getAction().equals(Constant.hasMsgUpdatedAction)){
				adapter.notifyDataSetChanged();
			}else if(intent.getAction().equals(Constant.receivedSendFileRequestAction)){//���յ��ļ�����������������ļ�
				if(!isPaused){//������?�ڿɼ�״̬����Ӧ�㲥,����һ����ʾ���Ƿ�Ҫ���շ��������ļ�
					receivedFileNames = mService.getReceivedFileNames();//�ӷ������������Ҫ���յ��ļ����ļ���
					if(receivedFileNames.size()<=0)return;
					receiveFileListAdapter.setResources(receivedFileNames);
					Person psn = (Person)intent.getExtras().get("person");
					AlertDialog.Builder  builder = new AlertDialog.Builder(context);
					builder.setTitle(psn.personNickeName);
					builder.setMessage(R.string.sending_file_to_you);
					builder.setIcon(psn.personHeadIconId);
					View vi = getLayoutInflater().inflate(R.layout.request_file_popupwindow_layout, null);
					builder.setView(vi);
					final AlertDialog recDialog = builder.show();
					recDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
						@Override
						public void onDismiss(DialogInterface arg0) {
							receivedFileNames.clear();
				        	if(!finishedSendFile){//�����ļ���δ���վ͹رս��մ��ڣ�˵������ν��գ�ͬʱ��Զ�̷���һ���ܾ���յ�ָ�
					        	Intent intent = new Intent();
								intent.setAction(Constant.refuseReceiveFileAction);
								sendBroadcast(intent);
				        	}
				        	finishedSendFile = false;//�ر��ļ����նԻ��򣬱���ʾ�����ļ�������ɣ��ѱ����ļ�����״̬��Ϊfalse
						}
					});
					ListView lv = (ListView)vi.findViewById(R.id.receive_file_list);//��Ҫ���յ��ļ��嵥
					lv.setAdapter(receiveFileListAdapter);
					Button btn_ok = (Button)vi.findViewById(R.id.receive_file_okbtn);
			        Button btn_cancle = (Button)vi.findViewById(R.id.receive_file_cancel);
			      //���ð�ť���������ļ�ѡ�����������ó��ļ���ѡ��ģʽ��ѡ��һ���������նԷ��ļ����ļ���
			        btn_ok.setOnClickListener(new View.OnClickListener() {   
			            @Override  
			            public void onClick(View v) { 
			            	if(!finishedSendFile){//�����ļ��Ѿ����չ������ٴ��ļ���ѡ����
				            	Intent intent = new Intent(FlyPigeonMainActivity.this,MyFileManager.class);
				    			intent.putExtra("selectType", Constant.SELECT_FILE_PATH);
				    			startActivityForResult(intent, 0);
			            	}
			    		//	dialog.dismiss();
			            }   
				     });   
				    //���ð�ť������������㷢���û��ܾ�����ļ��Ĺ㲥       
			        btn_cancle.setOnClickListener(new View.OnClickListener() {   
				        @Override  
				        public void onClick(View v) { 
				        	recDialog.dismiss();
				        }   
			        });
				}
			}
		}
    }
    //=========================�㲥����������==========================================================
    
    
	//�㲥������ע��
	private void regBroadcastRecv(){
        broadcastRecv = new MyBroadcastRecv();
        bFilter = new IntentFilter();
        bFilter.addAction(Constant.updateMyInformationAction);
        bFilter.addAction(Constant.personHasChangedAction);
        bFilter.addAction(Constant.hasMsgUpdatedAction);
        bFilter.addAction(Constant.receivedSendFileRequestAction);
        bFilter.addAction(Constant.remoteUserRefuseReceiveFileAction);
        bFilter.addAction(Constant.dataReceiveErrorAction);
        bFilter.addAction(Constant.dataSendErrorAction);
        bFilter.addAction(Constant.fileReceiveStateUpdateAction);
        bFilter.addAction(Constant.fileSendStateUpdateAction);
        bFilter.addAction(Constant.receivedTalkRequestAction);
        bFilter.addAction(Constant.remoteUserClosedTalkAction);
        registerReceiver(broadcastRecv, bFilter);
	}
	//�򿪷�����ҳ��
	private void openChartPage(Person person){
		Intent intent = new Intent(this,ChartMsgActivity.class);
		intent.putExtra("person", person);
		intent.putExtra("me", me);
		startActivity(intent);
	}
}