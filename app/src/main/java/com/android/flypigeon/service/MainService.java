package com.android.flypigeon.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.keep.lin.R;
import com.mdeal.data.Media;
import com.mdeal.data.MediaDao;

import com.android.flypigeon.util.ByteAndInt;
import com.android.flypigeon.util.Constant;
import com.android.flypigeon.util.FileName;
import com.android.flypigeon.util.FileState;
import com.android.flypigeon.util.Message;
import com.android.flypigeon.util.Person;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class MainService extends Service {
	private ServiceBinder sBinder = new ServiceBinder();//�������
	private static ArrayList<Map<Integer,Person>> children = new ArrayList<Map<Integer,Person>>();//�����������е��û���ÿ��map���󱣴�һ�����ȫ���û�
	private static Map<Integer,Person> childrenMap = new HashMap<Integer,Person>();//��ǰ�����û�
	private static ArrayList<Integer> personKeys = new ArrayList<Integer>();//��ǰ�����û�id
	private static Map<Integer,List<Message>> msgContainer = new HashMap<Integer,List<Message>>();//�����û���Ϣ����
	private SharedPreferences pre = null;
	private SharedPreferences.Editor editor = null;
	private WifiManager wifiManager = null;
	private ServiceBroadcastReceiver receiver = null;
	public InetAddress localInetAddress = null;
	private String localIp = null;
	private byte[] localIpBytes = null; 
	private byte[] regBuffer = new byte[Constant.bufferSize];//��������ע�ύ��ָ��
	private byte[] msgSendBuffer = new byte[Constant.bufferSize];//��Ϣ���ͽ���
	private byte[] fileSendBuffer = new byte[Constant.bufferSize];//�ļ����ͽ���ָ��
	private byte[] talkCmdBuffer = new byte[Constant.bufferSize];//ͨ��ָ��
	private static Person me = null;//������������������Ϣ
	private CommunicationBridge comBridge = null;//ͨѶ��Э�����ģ��
	
	private MediaDao mMediaDao;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return sBinder;
	}
	@Override
	public boolean onUnbind(Intent intent) {
		return false;
	}
	@Override
	public void onRebind(Intent intent) {
		
	}
	@Override
	public void onCreate() {
		mMediaDao =  new MediaDao(this);
	}
	@Override
	public void onStart(Intent intent, int startId) {
		initCmdBuffer();//��ʼ��ָ���
		wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		new CheckNetConnectivity().start();//�������״̬����ȡIP��ַ
		
		comBridge = new CommunicationBridge();//����socket����
		comBridge.start();
		
		pre = PreferenceManager.getDefaultSharedPreferences(this);
		editor = pre.edit();
		
		regBroadcastReceiver();//ע��㲥������
		getMyInfomation();//���������Ϣ
		new UpdateMe().start();//�����緢�������ע��
		new CheckUserOnline().start();//����û��б��Ƿ��г�ʱ�û�
		sendPersonHasChangedBroadcast();//֪ͨ�����û�������˳�
		System.out.println("Service started...");
	}
	
	//�����
	public class ServiceBinder extends Binder{
		public MainService getService(){
			return MainService.this;
		}
	}
	
    //������ѵ������Ϣ
    private void getMyInfomation(){
    	SharedPreferences pre = PreferenceManager.getDefaultSharedPreferences(this);
    	int iconId = pre.getInt("headIconId", R.drawable.black_bird);
    	String nickeName = pre.getString("nickeName", "Zhang San");
    	int myId = pre.getInt("myId", Constant.getMyId());
		editor.putInt("myId", myId);
		editor.commit();
		
    	if(null==me)me = new Person();
    	me.personHeadIconId = iconId;
    	me.personNickeName = nickeName;
    	me.personId = myId;
    	me.ipAddress = localIp;
    	
    	//����ע�������û����
    	System.arraycopy(ByteAndInt.int2ByteArray(myId), 0, regBuffer, 6, 4);
    	System.arraycopy(ByteAndInt.int2ByteArray(iconId), 0, regBuffer, 10, 4);
    	for(int i=14;i<44;i++)regBuffer[i] = 0;//��ԭ�����ǳ��������
    	byte[] nickeNameBytes = nickeName.getBytes();
    	System.arraycopy(nickeNameBytes, 0, regBuffer, 14, nickeNameBytes.length);
    	
    	//����ͨ�������û����
    	System.arraycopy(ByteAndInt.int2ByteArray(myId), 0, talkCmdBuffer, 6, 4);
    	System.arraycopy(ByteAndInt.int2ByteArray(iconId), 0, talkCmdBuffer, 10, 4);
    	for(int i=14;i<44;i++)talkCmdBuffer[i] = 0;//��ԭ�����ǳ��������
    	System.arraycopy(nickeNameBytes, 0, talkCmdBuffer, 14, nickeNameBytes.length);
    }
	
	private String getCurrentTime(){
		Date date = new Date();
		return date.toLocaleString();
	}

    //�����������״̬,��ñ���IP��ַ
	private class CheckNetConnectivity extends Thread {
		public void run() {
			try {
//				if (!wifiManager.isWifiEnabled()) {
//					wifiManager.setWifiEnabled(true);
//				}
				
				for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
					NetworkInterface intf = en.nextElement();
					for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
						InetAddress inetAddress = enumIpAddr.nextElement();
						if (!inetAddress.isLoopbackAddress()&& inetAddress instanceof Inet4Address) {
//							if(inetAddress.isReachable(1000)){
								localInetAddress = inetAddress;
								localIp = inetAddress.getHostAddress().toString();
								localIpBytes = inetAddress.getAddress();
								System.arraycopy(localIpBytes,0,regBuffer,44,4);
//							}
						 
						}
					}
				}
				
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		};
	};
	
	//��ʼ��ָ���
	private void initCmdBuffer(){
		//��ʼ���û�ע��ָ���
		for(int i=0;i<Constant.bufferSize;i++)regBuffer[i]=0;
		System.arraycopy(Constant.pkgHead, 0, regBuffer, 0, 3);
		regBuffer[3] = Constant.CMD80;
		regBuffer[4] = Constant.CMD_TYPE1;
		regBuffer[5] = Constant.OPR_CMD1;
		
		//��ʼ����Ϣ����ָ���
		for(int i=0;i<Constant.bufferSize;i++)msgSendBuffer[i]=0;
		System.arraycopy(Constant.pkgHead, 0, msgSendBuffer, 0, 3);
		msgSendBuffer[3] = Constant.CMD81;
		msgSendBuffer[4] = Constant.CMD_TYPE1;
		msgSendBuffer[5] = Constant.OPR_CMD1;
		
		//��ʼ�������ļ�ָ���
		for(int i=0;i<Constant.bufferSize;i++)fileSendBuffer[i]=0;
		System.arraycopy(Constant.pkgHead, 0, fileSendBuffer, 0, 3);
		fileSendBuffer[3] = Constant.CMD82;
		fileSendBuffer[4] = Constant.CMD_TYPE1;
		fileSendBuffer[5] = Constant.OPR_CMD1;
		
		//��ʼ��ͨ��ָ��
		//��ʼ�������ļ�ָ���
		for(int i=0;i<Constant.bufferSize;i++)talkCmdBuffer[i]=0;
		System.arraycopy(Constant.pkgHead, 0, talkCmdBuffer, 0, 3);
		talkCmdBuffer[3] = Constant.CMD83;
		talkCmdBuffer[4] = Constant.CMD_TYPE1;
		talkCmdBuffer[5] = Constant.OPR_CMD1;
	}
	//��������û�����
	public ArrayList<Map<Integer,Person>> getChildren(){
		return children;
	}
	//��������û�id
	public ArrayList<Integer> getPersonKeys(){
		return personKeys;
	}
	//����û�id��ø��û�����Ϣ
	public List<Message> getMessagesById(int personId){
		return msgContainer.get(personId);
	}
	//����û�id��ø��û�����Ϣ����
	public int getMessagesCountById(int personId){
		List<Message> msgs = msgContainer.get(personId);
		if(null!=msgs){
			return msgs.size();
		}else {
			return 0;
		}
	}
	
	//ÿ��10�뷢��һ�������
	boolean isStopUpdateMe = false;
	private class UpdateMe extends Thread{
		@Override
		public void run() {
			while(!isStopUpdateMe){
				try{
					comBridge.joinOrganization();
					sleep(10000);
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
	}
	
	//����û��Ƿ����ߣ�����15˵���û������ߣ�������б��������û�
	private class CheckUserOnline extends Thread{
		@Override
		public void run() {
			super.run();
			boolean hasChanged = false;
//			while(!isStopUpdateMe){
//				if(childrenMap.size()>0){
//					Set<Integer> keys = childrenMap.keySet();
//					for (Integer key : keys) {
//						if(System.currentTimeMillis()-childrenMap.get(key).timeStamp>15000){
//							childrenMap.remove(key);
//							personKeys.remove(Integer.valueOf(key));
//							hasChanged = true;
//						}
//					}
//				}
//				if(hasChanged)sendPersonHasChangedBroadcast();
//				try {sleep(5000);} catch (InterruptedException e) {e.printStackTrace();}
//			}
		}
	}
	
	//�����û����¹㲥
	private void sendPersonHasChangedBroadcast(){
		Intent intent = new Intent();
		intent.setAction(Constant.personHasChangedAction);
		sendBroadcast(intent);
	}
	
	//ע��㲥������
	private void regBroadcastReceiver(){
		receiver = new ServiceBroadcastReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(Constant.WIFIACTION);
		filter.addAction(Constant.ETHACTION);
		filter.addAction(Constant.updateMyInformationAction);
		filter.addAction(Constant.refuseReceiveFileAction);
		filter.addAction(Constant.imAliveNow);
		registerReceiver(receiver, filter);
	}
	
	//�㲥������������
	private class ServiceBroadcastReceiver extends BroadcastReceiver{
		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals(Constant.WIFIACTION) || intent.getAction().equals(Constant.ETHACTION)){
				new CheckNetConnectivity().start();
			}else if(intent.getAction().equals(Constant.updateMyInformationAction)){
				getMyInfomation();
				comBridge.joinOrganization();
			}else if(intent.getAction().equals(Constant.refuseReceiveFileAction)){
				comBridge.refuseReceiveFile();
			}else if(intent.getAction().equals(Constant.imAliveNow)){
				
			}
		}
	}
	
	//������Ϣ
	public void sendMsg(int personId,String msg){
		comBridge.sendMsg(personId, msg);
	}
	//�����ļ�
	public void sendFiles(int personId,ArrayList<FileName> files){
		comBridge.sendFiles(personId, files);
	}
	//�����ļ�
	public void receiveFiles(String fileSavePath){
		comBridge.receiveFiles(fileSavePath);
	}
	//�������յ��ļ���
	public ArrayList<FileState> getReceivedFileNames(){
		return comBridge.getReceivedFileNames();
	}
	//������͵��ļ���
	public ArrayList<FileState> getBeSendFileNames(){
		return comBridge.getBeSendFileNames();
	}
	//��ʼ��������
	public void startTalk(int personId){
		comBridge.startTalk(personId);
	}
	//������������
	public void stopTalk(int personId){
		comBridge.stopTalk(personId);
	}
	//����Զ����������
	public void acceptTalk(int personId){
		comBridge.acceptTalk(personId);
	}
	
	@Override
	public void onDestroy() {
		comBridge.release();
		unregisterReceiver(receiver);
		isStopUpdateMe = true;
		System.out.println("Service on destory...");
	}
	
	//========================Э�������ͨѶģ��=======================================================
	private class CommunicationBridge extends Thread{
		private MulticastSocket multicastSocket = null;
		private byte[] recvBuffer = new byte[Constant.bufferSize];
		private int fileSenderUid = 0;//���������ļ������ߵ�id��
		private boolean isBusyNow = false;//�����Ƿ������շ��ļ�������״̬Ϊtrue���ʾ�������ڽ����շ��ļ���������ʱ��Ҫ���������ļ����û�����æָ��
		private String fileSavePath = null;//����������յ����ļ�
		private boolean isStopTalk = false;//ͨ�������־
		private ArrayList<FileName> tempFiles = null;//������ʱ������Ҫ���͵��ļ���
		private int tempUid = 0;//������ʱ������Ҫ�����ļ����û�id(�����ļ������û�id)
		private ArrayList<FileState> receivedFileNames = new ArrayList<FileState>();
		private ArrayList<FileState> beSendFileNames = new ArrayList<FileState>();
		
		private FileHandler fileHandler = null;//�ļ������̣߳������շ��ļ�
		private AudioHandler audioHandler = null;//��Ƶ����ģ�飬�����շ���Ƶ���
		
		public CommunicationBridge(){
			fileHandler = new FileHandler();
			fileHandler.start();
			
			audioHandler = new AudioHandler();
			audioHandler.start();
		}

		//���鲥�˿ڣ�׼���鲥ͨѶ
		@Override
		public void run() {
			super.run();
			try {
				multicastSocket = new MulticastSocket(Constant.PORT);
				multicastSocket.joinGroup(InetAddress.getByName(Constant.MULTICAST_IP));
				System.out.println("Socket started...");
				while (!multicastSocket.isClosed() && null!=multicastSocket) {
					for (int i=0;i<Constant.bufferSize;i++){recvBuffer[i]=0;}
		        	DatagramPacket rdp = new DatagramPacket(recvBuffer, recvBuffer.length);
		        	multicastSocket.receive(rdp);
		        	parsePackage(recvBuffer);
		        }
			} catch (Exception e) {
				try {
					if(null!=multicastSocket && !multicastSocket.isClosed()){
						multicastSocket.leaveGroup(InetAddress.getByName(Constant.MULTICAST_IP));
						multicastSocket.close();
					}
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				e.printStackTrace();
			} 
		}

		//�������յ�����ݰ�
		private void parsePackage(byte[] pkg) {
			int CMD = pkg[3];//������
			int cmdType = pkg[4];//��������
			int oprCmd = pkg[5];//��������

			//����û�ID��
			byte[] uId = new byte[4];
			System.arraycopy(pkg, 6, uId, 0, 4);
			int userId = ByteAndInt.byteArray2Int(uId);
			
			switch (CMD) {
			case Constant.CMD80:
				switch (cmdType) {
				case Constant.CMD_TYPE1:
					//������Ϣ�����Լ��������Է����ͻ�Ӧ��,���ѶԷ������û��б�
					if(userId != me.personId){
						updatePerson(userId,pkg);
						//����Ӧ���
						byte[] ipBytes = new byte[4];//������󷽵�ip��ַ
						System.arraycopy(pkg, 44, ipBytes, 0, 4);
						try {
							InetAddress targetIp = InetAddress.getByAddress(ipBytes);
							regBuffer[4] = Constant.CMD_TYPE2;//���Լ���ע����Ϣ�޸ĳ�Ӧ����Ϣ��־�����Լ�����Ϣ���͸�����
							DatagramPacket dp = new DatagramPacket(regBuffer,Constant.bufferSize,targetIp,Constant.PORT);
							multicastSocket.send(dp);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					break;
				case Constant.CMD_TYPE2:
					updatePerson(userId,pkg);
					break;
				case Constant.CMD_TYPE3:
					childrenMap.remove(userId);
					personKeys.remove(Integer.valueOf(userId));
					sendPersonHasChangedBroadcast();
					break;
				}
				break;
			case Constant.CMD81:// �յ���Ϣ
				switch (cmdType) {
				case Constant.CMD_TYPE1:
					List<Message> messages = null;
					if(msgContainer.containsKey(userId)){
						messages = msgContainer.get(userId);
					}else{
						messages = new ArrayList<Message>();
					}
					byte[] msgBytes = new byte[Constant.msgLength];
					System.arraycopy(pkg, 10, msgBytes, 0, Constant.msgLength);
					String msgStr = new String(msgBytes).trim();
					Message msg = new Message();
					msg.msg = msgStr;
					msg.receivedTime = getCurrentTime();
					messages.add(msg);
					msgContainer.put(userId, messages);
					
					Intent intent = new Intent();
					intent.setAction(Constant.hasMsgUpdatedAction);
					intent.putExtra("userId", userId);
					intent.putExtra("msgCount", messages.size());
					sendBroadcast(intent);
					break;
				case Constant.CMD_TYPE2:
					break;
				}
				break;
			case Constant.CMD82:
				switch (cmdType) {
				case Constant.CMD_TYPE1://�յ��ļ���������
					switch(oprCmd){
					case Constant.OPR_CMD1:
						//���͹㲥��֪ͨ�������ļ���Ҫ����
						if(!isBusyNow){
						//	isBusyNow = true;
							fileSenderUid = userId;//�����ļ������ߵ�id�ţ��Ա�����������߾ܾ�����ļ�ʱ����ͨ���id�ҵ������ߣ��������߷��;ܾ����ָ��
							Person person = childrenMap.get(Integer.valueOf(userId));
							Intent intent = new Intent();
							intent.putExtra("person", person);
							intent.setAction(Constant.receivedSendFileRequestAction);
							sendBroadcast(intent);
						}else{//���ǰ�����շ��ļ�����Է�����æָ��
							Person person = childrenMap.get(Integer.valueOf(userId));
							fileSendBuffer[4]=Constant.CMD_TYPE2;
							fileSendBuffer[5]=Constant.OPR_CMD4;
							byte[] meIdBytes = ByteAndInt.int2ByteArray(me.personId);
							System.arraycopy(meIdBytes, 0, fileSendBuffer, 6, 4);
							try{
								DatagramPacket dp = new DatagramPacket(fileSendBuffer,Constant.bufferSize,InetAddress.getByName(person.ipAddress),Constant.PORT);
								multicastSocket.send(dp);
							}catch(Exception e){
								e.printStackTrace();
							}
						}
						break;
					case Constant.OPR_CMD5://���նԷ����������ļ�����Ϣ
						byte[] fileNameBytes = new byte[Constant.fileNameLength];
						byte[] fileSizeByte = new byte[8];
						System.arraycopy(pkg, 10, fileNameBytes, 0, Constant.fileNameLength);
						System.arraycopy(pkg, 100, fileSizeByte, 0, 8);
						FileState fs = new FileState();
						fs.fileName = new String(fileNameBytes).trim();
						fs.fileSize = Long.valueOf(ByteAndInt.byteArrayToLong(fileSizeByte));
						receivedFileNames.add(fs);
						break;
					}
					break;
				case Constant.CMD_TYPE2:
					switch(oprCmd){
					case Constant.OPR_CMD2://�Է�ͬ������ļ�
						fileHandler.startSendFile();
						System.out.println("Start send file to remote user ...");
						break;
					case Constant.OPR_CMD3://�Է��ܾ�����ļ�
						Intent intent = new Intent();
						intent.setAction(Constant.remoteUserRefuseReceiveFileAction);
						sendBroadcast(intent);
						System.out.println("Remote user refuse to receive file ...");
						break;
					case Constant.OPR_CMD4://�Է�����æ
						System.out.println("Remote user is busy now ...");
						break;
					}
					break;
				}
				break;
			case Constant.CMD83://83�������ͨѶ���
				switch(cmdType){
				case Constant.CMD_TYPE1:
					switch(oprCmd){
					case Constant.OPR_CMD1://���յ�Զ������ͨ������
						System.out.println("Received a talk request ... ");
						isStopTalk = false;
						Person person = childrenMap.get(Integer.valueOf(userId));
						Intent intent = new Intent();
						intent.putExtra("person", person);
						intent.setAction(Constant.receivedTalkRequestAction);
						sendBroadcast(intent);
						break;
					case Constant.OPR_CMD2:
						//�յ��ر�ָ��ر�����ͨ��
						System.out.println("Received remote user stop talk cmd ... ");
						isStopTalk = true;
						Intent i = new Intent();
						i.setAction(Constant.remoteUserClosedTalkAction);
						sendBroadcast(i);
						break;
					}
					break;
				case Constant.CMD_TYPE2:
					switch(oprCmd){
					case Constant.OPR_CMD1:
						//����Ӧ�𣬿�ʼ����ͨ��
						if(!isStopTalk){
							System.out.println("Begin to talk with remote user ... ");
							Person person = childrenMap.get(Integer.valueOf(userId));
							audioHandler.audioSend(person);
						}
						break;
					}
					break;
				}
				break;
			}
		}
		
		//���»���û���Ϣ���û��б���
		private void updatePerson(int userId,byte[] pkg){
			Person person = new Person();
			getPerson(pkg,person);
			childrenMap.put(userId, person);
			if(!personKeys.contains(Integer.valueOf(userId))){
				personKeys.add(Integer.valueOf(userId));
			}
			if(!children.contains(childrenMap)){
				children.add(childrenMap);
			}
			
			Media mMedia = new Media();
        	mMedia.setPath(person.ipAddress);
        	mMedia.setName(person.personNickeName);
        	if(mMediaDao.exists(mMedia)){
        		if(mMediaDao.existsWithOherName(mMedia, person.personNickeName)){
	        		mMediaDao.removeMedia(person.ipAddress);
	        		mMediaDao.addMedia(mMedia);
	        		Log.d("phone book","add a new ip = "+person.ipAddress+" name = "+person.personNickeName);
        		}
        		
        	}else{
        		mMediaDao.addMedia(mMedia);
        		Log.d("phone book","add a new ip = "+person.ipAddress+" name = "+person.personNickeName);
        	}
			sendPersonHasChangedBroadcast();
		}
		
		//�ر�Socket����
		private void release(){
			try {
				regBuffer[4] = Constant.CMD_TYPE3;//�����������޸ĳ�ע���־�����㲥���ͣ��������û����˳�
				DatagramPacket dp = new DatagramPacket(regBuffer,Constant.bufferSize,InetAddress.getByName(Constant.MULTICAST_IP),Constant.PORT);
				multicastSocket.send(dp);
				System.out.println("Send logout cmd ...");
				
				multicastSocket.leaveGroup(InetAddress.getByName(Constant.MULTICAST_IP));
				multicastSocket.close();
				
				System.out.println("Socket has closed ...");
			} catch (Exception e) {
				e.printStackTrace();
			}finally{
				fileHandler.release();
				audioHandler.release();
			}
		}
		
		//������ݰ��ȡһ���û���Ϣ
		private void getPerson(byte[] pkg,Person person){
			
			byte[] personIdBytes = new byte[4];
			byte[] iconIdBytes = new byte[4];
			byte[] nickeNameBytes = new byte[30];
			byte[] personIpBytes = new byte[4];
			
			System.arraycopy(pkg, 6, personIdBytes, 0, 4);
			System.arraycopy(pkg, 10, iconIdBytes, 0, 4);
			System.arraycopy(pkg, 14, nickeNameBytes, 0, 30);
			System.arraycopy(pkg, 44, personIpBytes, 0, 4);
			
			person.personId = ByteAndInt.byteArray2Int(personIdBytes);
			person.personHeadIconId = ByteAndInt.byteArray2Int(iconIdBytes);
			person.personNickeName = (new String(nickeNameBytes)).trim();
			person.ipAddress = Constant.intToIp(ByteAndInt.byteArray2Int(personIpBytes));
			person.timeStamp = System.currentTimeMillis();
		}
		
		//ע���Լ���������
		public void joinOrganization(){
			try {
				if(null!=multicastSocket && !multicastSocket.isClosed()){
					regBuffer[4] = Constant.CMD_TYPE1;//�ָ���ע�������־����������ע���Լ�
					DatagramPacket dp = new DatagramPacket(regBuffer,Constant.bufferSize,InetAddress.getByName(Constant.MULTICAST_IP),Constant.PORT);
					multicastSocket.send(dp);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		//������Ϣ
		public void sendMsg(int personId,String msg){
			try {
				Person psn = childrenMap.get(personId);
				if(null!=psn){
					System.arraycopy(ByteAndInt.int2ByteArray(me.personId), 0, msgSendBuffer, 6, 4);
					int msgLength = Constant.msgLength+10;
					for(int i=10;i<msgLength;i++){msgSendBuffer[i]=0;}
					byte[] msgBytes = msg.getBytes();
					System.arraycopy(msgBytes, 0, msgSendBuffer, 10, msgBytes.length);
					DatagramPacket dp = new DatagramPacket(msgSendBuffer,Constant.bufferSize,InetAddress.getByName(psn.ipAddress),Constant.PORT);
					multicastSocket.send(dp);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		//��Է�������������ļ�ָ��
		public void sendFiles(int personId,ArrayList<FileName> files){
			if(personId>0 && null!=files && files.size()>0){
				try{
					tempUid = personId;
					tempFiles = files;
					Person person = childrenMap.get(Integer.valueOf(tempUid));
					fileSendBuffer[4]=Constant.CMD_TYPE1;
					fileSendBuffer[5]=Constant.OPR_CMD5;
					byte[] meIdBytes = ByteAndInt.int2ByteArray(me.personId);
					System.arraycopy(meIdBytes, 0, fileSendBuffer, 6, 4);
					int fileNameLength = Constant.fileNameLength+10;//���ͷ�ļ�����ļ���洢�����Ա�д�µ��ļ���
					//��Ҫ���͵������ļ����͸�Է�
					for (final FileName file : tempFiles) {
						//�ռ����Ҫ�����ļ������������
						FileState fs = new FileState(file.fileSize,0,file.getFileName());
						beSendFileNames.add(fs);
						
						byte[] fileNameBytes = file.getFileName().getBytes();
						for(int i=10;i<fileNameLength;i++)fileSendBuffer[i]=0;
						System.arraycopy(fileNameBytes, 0, fileSendBuffer, 10, fileNameBytes.length);//���ļ������ͷ��ݰ�
						System.arraycopy(ByteAndInt.longToByteArray(file.fileSize), 0, fileSendBuffer, 100, 8);
						DatagramPacket dp = new DatagramPacket(fileSendBuffer,Constant.bufferSize,InetAddress.getByName(person.ipAddress),Constant.PORT);
						multicastSocket.send(dp);
					}
					//�Է�������������ļ�ָ��
					fileSendBuffer[5]=Constant.OPR_CMD1;
					DatagramPacket dp = new DatagramPacket(fileSendBuffer,Constant.bufferSize,InetAddress.getByName(person.ipAddress),Constant.PORT);
					multicastSocket.send(dp);
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		//��Է���Ӧͬ������ļ�ָ��
		public void receiveFiles(String fileSavePath) {
			this.fileSavePath = fileSavePath;
			Person person = childrenMap.get(Integer.valueOf(fileSenderUid));
			fileSendBuffer[4]=Constant.CMD_TYPE2;
			fileSendBuffer[5]=Constant.OPR_CMD2;
			byte[] meIdBytes = ByteAndInt.int2ByteArray(me.personId);
			System.arraycopy(meIdBytes, 0, fileSendBuffer, 6, 4);
			try{
				DatagramPacket dp = new DatagramPacket(fileSendBuffer,Constant.bufferSize,InetAddress.getByName(person.ipAddress),Constant.PORT);
				multicastSocket.send(dp);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		//���ļ������߷��;ܾ�����ļ�ָ��
		public void refuseReceiveFile(){
		//	isBusyNow = false;
			Person person = childrenMap.get(Integer.valueOf(fileSenderUid));
			fileSendBuffer[4]=Constant.CMD_TYPE2;
			fileSendBuffer[5]=Constant.OPR_CMD3;
			byte[] meIdBytes = ByteAndInt.int2ByteArray(me.personId);
			System.arraycopy(meIdBytes, 0, fileSendBuffer, 6, 4);
			try{
				DatagramPacket dp = new DatagramPacket(fileSendBuffer,Constant.bufferSize,InetAddress.getByName(person.ipAddress),Constant.PORT);
				multicastSocket.send(dp);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		//���������ļ����ļ���
	    public ArrayList<FileState> getReceivedFileNames() {
			return receivedFileNames;
		}
		//��������ļ����ļ���
	    public ArrayList<FileState> getBeSendFileNames(){
	    	return beSendFileNames;
	    }
	    //��ʼ�������У���Զ������������������
	    public void startTalk(int personId){
			try {
				isStopTalk = false;
				talkCmdBuffer[4] = Constant.CMD_TYPE1;
		    	talkCmdBuffer[5] = Constant.OPR_CMD1;
				System.arraycopy(InetAddress.getByName(me.ipAddress).getAddress(), 0, talkCmdBuffer, 44, 4);
				Person person = childrenMap.get(Integer.valueOf(personId));
				DatagramPacket dp = new DatagramPacket(talkCmdBuffer,Constant.bufferSize,InetAddress.getByName(person.ipAddress),Constant.PORT);
				multicastSocket.send(dp);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
	    }
	    //������������
	    public void stopTalk(int personId){
	    	isStopTalk = true;
	    	talkCmdBuffer[4] = Constant.CMD_TYPE1;
	    	talkCmdBuffer[5] = Constant.OPR_CMD2;
	    	Person person = childrenMap.get(Integer.valueOf(personId));
	    	try {
	    		System.arraycopy(InetAddress.getByName(me.ipAddress).getAddress(), 0, talkCmdBuffer, 44, 4);
	    		DatagramPacket dp = new DatagramPacket(talkCmdBuffer,Constant.bufferSize,InetAddress.getByName(person.ipAddress),Constant.PORT);
				multicastSocket.send(dp);
			} catch (Exception e) {
				e.printStackTrace();
			}
	    }
	    //����Զ�������������󣬲���Զ�̷����������
	    public void acceptTalk(int personId){
			talkCmdBuffer[3] = Constant.CMD83;
			talkCmdBuffer[4] = Constant.CMD_TYPE2;
			talkCmdBuffer[5] = Constant.OPR_CMD1;
			try {
				//���ͽ�������ָ��
				System.arraycopy(InetAddress.getByName(me.ipAddress).getAddress(), 0, talkCmdBuffer, 44, 4);
				Person person = childrenMap.get(Integer.valueOf(personId));
				DatagramPacket dp = new DatagramPacket(talkCmdBuffer,Constant.bufferSize,InetAddress.getByName(person.ipAddress),Constant.PORT);
				multicastSocket.send(dp);
				audioHandler.audioSend(person);//ͬʱ��Է�������Ƶ���
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
	    }
	    
	    //=========================TCP�ļ�����ģ��==================================================================    
		//����Tcp������ļ��շ�ģ��
		private class FileHandler extends Thread{
			private ServerSocket sSocket = null;
			
			public FileHandler(){}
			@Override
			public void run() {
				super.run();
				try {
					sSocket = new ServerSocket(Constant.PORT);
					System.out.println("File Handler socket started ...");
					while(!sSocket.isClosed() && null!=sSocket){
						Socket socket = sSocket.accept();
						socket.setSoTimeout(5000);
						new SaveFileToDisk(socket).start();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			//������յ������
			private class SaveFileToDisk extends Thread{
				private Socket socket = null;
				public SaveFileToDisk(Socket socket){
					this.socket = socket;
				}
				@Override
				public void run() {
					super.run();
					OutputStream output = null;
					InputStream input = null;
					try {
						byte[] recvFileCmd = new byte[Constant.bufferSize];//���նԷ���һ�η���������ݣ�����ݰ��а���Ҫ���͵��ļ���
						input = socket.getInputStream();
						input.read(recvFileCmd);//��ȡ�Է������������
						int cmdType = recvFileCmd[4];//��Э����λΪ��������
						int oprCmd = recvFileCmd[5];//��������
						if(cmdType==Constant.CMD_TYPE1 && oprCmd ==Constant.OPR_CMD6){
							byte[] fileNameBytes = new byte[Constant.fileNameLength];//���յ�����ݰ�����ȡ�ļ���
							System.arraycopy(recvFileCmd, 10, fileNameBytes, 0, Constant.fileNameLength);
							StringBuffer sb = new StringBuffer();
							String fName = new String(fileNameBytes).trim(); 
							sb.append(fileSavePath).append(File.separator).append(fName);//��ϳ�������ļ���
							String fileName = sb.toString();
							File file = new File(fileName);//��ݻ�õ��ļ����ļ�
							//������ݽ��ջ�����׼�����նԷ����������ļ�����
							byte[] readBuffer = new byte[Constant.readBufferSize];
							output = new FileOutputStream(file);//���ļ������׼���ѽ��յ�������д���ļ���
							int readSize = 0;
							int length = 0;
							long count = 0;
							FileState fs = getFileStateByName(fName,receivedFileNames);
							
							while(-1 != (readSize = input.read(readBuffer))){//ѭ����ȡ����
								output.write(readBuffer,0,readSize);//�ѽ��յ�������д���ļ���
								output.flush();
								length+=readSize;
								count++;
								if(count%10==0){
									fs.currentSize = length;
									fs.percent=((int)((Float.valueOf(length)/Float.valueOf(fs.fileSize))*100));
									Intent intent = new Intent();
									intent.setAction(Constant.fileReceiveStateUpdateAction);
									sendBroadcast(intent);
								}
							}
							fs.currentSize = length;
							fs.percent=((int)((Float.valueOf(length)/Float.valueOf(fs.fileSize))*100));
							Intent intent = new Intent();
							intent.setAction(Constant.fileReceiveStateUpdateAction);
							sendBroadcast(intent);
						}else{
							Intent intent = new Intent();
							intent.putExtra("msg", getString(R.string.data_receive_error));
							intent.setAction(Constant.dataReceiveErrorAction);
							sendBroadcast(intent);
						}
					} catch (Exception e) {
						Intent intent = new Intent();
						intent.putExtra("msg", e.getMessage());
						intent.setAction(Constant.dataReceiveErrorAction);
						sendBroadcast(intent);
						e.printStackTrace();
					}finally{
						try {
							if(null!=input)input.close();
							if(null!=output)output.close();
							if(!socket.isClosed())socket.close();
						} catch (Exception e1) {
							e1.printStackTrace();
						}
					}
				}
			}
			
			//��ʼ��Է������ļ�
			public void startSendFile() {
				//��ý��շ���Ϣ
				Person person = childrenMap.get(Integer.valueOf(tempUid));
				final String userIp = person.ipAddress;
				//���ͷ��ݰ����ݰ��а���Ҫ���͵��ļ���
				final byte[] sendFileCmd = new byte[Constant.bufferSize];
				for(int i=0;i<Constant.bufferSize;i++)sendFileCmd[i]=0;
				System.arraycopy(Constant.pkgHead, 0, sendFileCmd, 0, 3);
				sendFileCmd[3] = Constant.CMD82;
				sendFileCmd[4] = Constant.CMD_TYPE1;
				sendFileCmd[5] = Constant.OPR_CMD6;
				System.arraycopy(ByteAndInt.int2ByteArray(me.personId), 0, sendFileCmd, 6, 4);
				for (final FileName file : tempFiles) {//���ö��̷߳����ļ�
					new Thread() {
						@Override
						public void run() {
							Socket socket = null;
							OutputStream output = null;
							InputStream input = null;
							try {
								socket = new Socket(userIp,Constant.PORT);
								byte[] fileNameBytes = file.getFileName().getBytes();
								int fileNameLength = Constant.fileNameLength+10;//���ͷ�ļ�����ļ���洢�����Ա�д�µ��ļ���
								for(int i=10;i<fileNameLength;i++)sendFileCmd[i]=0;
								System.arraycopy(fileNameBytes, 0, sendFileCmd, 10, fileNameBytes.length);//���ļ������ͷ��ݰ�
								System.arraycopy(ByteAndInt.longToByteArray(file.fileSize), 0, sendFileCmd, 100, 8);
								output = socket.getOutputStream();//����һ�������
								output.write(sendFileCmd);//��ͷ��ݰ��Է�
								output.flush();
								sleep(1000);//sleep 1���ӣ��ȴ�Է�������
								//������ݷ��ͻ�����
								byte[] readBuffer = new byte[Constant.readBufferSize];//�ļ���д����
								input = new FileInputStream(new File(file.fileName));//��һ���ļ�������
								int readSize = 0;
								int length = 0;
								long count = 0;
								FileState fs = getFileStateByName(file.getFileName(), beSendFileNames);
								while(-1 != (readSize = input.read(readBuffer))){//ѭ�����ļ����ݷ��͸�Է�
									output.write(readBuffer,0,readSize);//������д��������з��͸�Է�
									output.flush();
									length+=readSize;
									
									count++;
									if(count%10==0){
										fs.currentSize = length;
										fs.percent=((int)((Float.valueOf(length)/Float.valueOf(fs.fileSize))*100));
										Intent intent = new Intent();
										intent.setAction(Constant.fileSendStateUpdateAction);
										sendBroadcast(intent);
									}
								}
								fs.currentSize = length;
								fs.percent=((int)((Float.valueOf(length)/Float.valueOf(fs.fileSize))*100));
								Intent intent = new Intent();
								intent.setAction(Constant.fileSendStateUpdateAction);
								sendBroadcast(intent);
							} catch (Exception e) {
								//�����㷢���ļ����������Ϣ
								Intent intent = new Intent();
								intent.putExtra("msg", e.getMessage());
								intent.setAction(Constant.dataSendErrorAction);
								sendBroadcast(intent);
								e.printStackTrace();
							}finally{
								try {
									if(null!=output)output.close();
									if(null!=input)input.close();
									if(!socket.isClosed())socket.close();
								} catch (Exception e1) {
									e1.printStackTrace();
								}
							} 
						}
					}.start();
				}
			}
			//����ļ�����ļ�״̬�б��л�ø��ļ�״̬
			private FileState getFileStateByName(String fileName,ArrayList<FileState> fileStates){
				for (FileState fileState : fileStates) {
					if(fileState.fileName.equals(fileName)){
						return fileState;
					}
				}
				return null;
			}
			
			public void release() {
				try {
					System.out.println("File handler socket closed ...");
					if(null!=sSocket)sSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		//=========================TCP�ļ�����ģ�����============================================================== 
		
	    //=========================TCP��������ģ��==================================================================    
		//����Tcp��������ģ��
		private class AudioHandler extends Thread{
			private ServerSocket sSocket = null;
			
		//	private G711Codec codec;
			public AudioHandler(){}
			@Override
			public void run() {
				super.run();
				try {
					sSocket = new ServerSocket(Constant.AUDIO_PORT);//������Ƶ�˿�
					System.out.println("Audio Handler socket started ...");
					while(!sSocket.isClosed() && null!=sSocket){
						Socket socket = sSocket.accept();
						socket.setSoTimeout(5000);
						audioPlay(socket);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			//����������Ƶ�������߳�
			public void audioPlay(Socket socket){
				new AudioPlay(socket).start();
			}
			//����������Ƶ�������߳�
			public void audioSend(Person person){
				new AudioSend(person).start();
			}
			
			//��Ƶ���߳�
			public class AudioPlay extends Thread{
				Socket socket = null;
				public AudioPlay(Socket socket){
					this.socket = socket;
				//	android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO); 
				}
				
				@Override
				public void run() {
					super.run();
					try {
						InputStream is = socket.getInputStream();
						//�����Ƶ�������С
						int bufferSize = AudioTrack.getMinBufferSize(8000,
								AudioFormat.CHANNEL_CONFIGURATION_MONO,
								AudioFormat.ENCODING_PCM_16BIT);

						//����������
						AudioTrack player = new AudioTrack(AudioManager.STREAM_MUSIC, 
								8000,
								AudioFormat.CHANNEL_CONFIGURATION_MONO,
								AudioFormat.ENCODING_PCM_16BIT,
								bufferSize,
								AudioTrack.MODE_STREAM);

						//������������
						player.setStereoVolume(1.0f, 1.0f);
						//��ʼ��������
						player.play();
						byte[] audio = new byte[160];//��Ƶ��ȡ����
						int length = 0;
						
						while(!isStopTalk){
							length = is.read(audio);//�������ȡ��Ƶ���
							if(length>0 && length%2==0){
							//	for(int i=0;i<length;i++)audio[i]=(byte)(audio[i]*2);//��Ƶ�Ŵ�1��
								player.write(audio, 0, length);//������Ƶ���
							}
						}
						player.stop();
						is.close();
						socket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			
			//��Ƶ�����߳�
			public class AudioSend extends Thread{
				Person person = null;
				
				public AudioSend(Person person){
					this.person = person;
				//	android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO); 
				}
				@Override
				public void run() {
					super.run();
					Socket socket = null;
					OutputStream os = null;
					AudioRecord recorder = null;
					try {
						socket = new Socket(person.ipAddress, Constant.AUDIO_PORT);
						socket.setSoTimeout(5000);
						os = socket.getOutputStream();
						//���¼���������С
						int bufferSize = AudioRecord.getMinBufferSize(8000,
								AudioFormat.CHANNEL_CONFIGURATION_MONO,
								AudioFormat.ENCODING_PCM_16BIT);
						
						//���¼�������
						recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
								8000,AudioFormat.CHANNEL_CONFIGURATION_MONO,
								AudioFormat.ENCODING_PCM_16BIT,
								bufferSize*10);
						
						recorder.startRecording();//��ʼ¼��
						byte[] readBuffer = new byte[640];//¼��������
						
						int length = 0;
						
						while(!isStopTalk){
							length = recorder.read(readBuffer,0,640);//��mic��ȡ��Ƶ���
							if(length>0 && length%2==0){
								os.write(readBuffer,0,length);//д�뵽�����������Ƶ���ͨ�����緢�͸�Է�
							}
						}
						recorder.stop();
						os.close();
						socket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			
			public void release() {
				try {
					System.out.println("Audio handler socket closed ...");
					if(null!=sSocket)sSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		//=========================TCP��������ģ�����================================================================== 
	}
	//========================Э�������ͨѶģ�����=======================================================
}
