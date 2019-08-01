package org.linphone.ui.witsui;

public class ClientTime {
	
	public static ClientTime mClientTime=null;
	
	private ClientTime(){}
	
	public static ClientTime getInstance(){
		if(mClientTime==null){
			mClientTime=new ClientTime();
		}
		return mClientTime;
	}
	public int hour;
	public int minunt;
}
