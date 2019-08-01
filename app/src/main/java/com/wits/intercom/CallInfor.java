package com.wits.intercom;

public class CallInfor {

	public final static int DOOR_KEEPER = 1;
	public final static int GUARD = 2;
	public final static int NEIGHBOER = 3;
	public final static int SITEMANAGER = 4;
	public final static int SITESERVICE = 5;
	public final static int ELEVATOR = 6;
	public final static int BLOCKGUARD = 7;
	public final static int DOORPANNEL = 8;
	public final static int IPCAMERA = 9;

	private static CallInfor instance;

	private CallInfor() {
	};

	public static CallInfor getInstance() {
		if (instance == null) {
			instance = new CallInfor();
		}
		return instance;
	}

	public static String getStringByRole(int role) {
		String roleString = "";
		switch (role) {
		case DOOR_KEEPER:
            roleString = "DOORKEEPER";
			break;
		case GUARD:
			roleString = "GUARD";
			break;
		case NEIGHBOER:
			roleString = "NEIGHBOR";
			break;
		case SITEMANAGER:
			roleString = "SITE GUARD";
			break;
		case SITESERVICE:
			roleString = "SITE SERVICE";
			break;
		case ELEVATOR:
			roleString = "ELEVATOR";
			break;
		case BLOCKGUARD:
			roleString = "BLOCK GUARD";
			break;
		case DOORPANNEL:
			roleString = "DOOR PANNEL";
			break;
		case IPCAMERA:
			roleString = "IP CAMERA";
			break;
		default:
			break;
		}
		return roleString;
	}

	/**
	 * role=1 doorkeeper role=2 guard role=3 neighbor role=4 sitemanager role=5
	 * siteservice role=6 elevator role=7 blockguard
	 */
	public static int role;
	public static String sipip;
	public static void setSipip(String toIp){
		sipip = toIp;
	}
	public static String getSipip(){
		return sipip;
	}
	public static void setRole(int ro){
		role = ro;
	}
	public static int getRole(){
		return role;
	}

}
