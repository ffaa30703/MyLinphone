package com.wits.intercom.util;

import android.util.Log;

public class AnalysisIPAddrFromNum {
	private final static String  TAG = AnalysisIPAddrFromNum.class.getSimpleName();

	/**
	 * 根据输入的号码按照协议解析出IP地址
	 * 
	 * @param sipString
	 * @return
	 */
	public static  String  getProtoIP(String sipString) {
		String ipString = "";
		if (sipString.trim().length() == 5) {
			String thirdPartString = "";
			String forthPartString = "";
			String blockNumString = sipString.substring(0, 2); //第几幢
			String roomNumString = sipString.substring(2, sipString.length());//第几幢房间号 
			
			ipString = "10." + Integer.parseInt(blockNumString.trim()) + ".";
			int temNum = Integer.parseInt(roomNumString.trim());
			
			//输入的房间号
			Logger.i(TAG, "sip:-------" + blockNumString + "---"
					+ roomNumString );
			
			if (Integer.parseInt(blockNumString) == 99) {
				if (temNum >= 901 && temNum <= 919) {
					thirdPartString = "26";
					forthPartString = (temNum - 900) + "";
				} else if (temNum >= 921 && temNum <= 959) {
					thirdPartString = "27";
					forthPartString = (temNum - 920) + "";
				} else if (temNum >= 961 && temNum <= 979) {
					thirdPartString = "28";
					forthPartString = (temNum - 960) + "";
				} else if (temNum >= 981 && temNum <= 989) {
					thirdPartString = "29";
					forthPartString = (temNum - 980) + "";
				}
			} else {
				if (temNum >= 1 && temNum <= 254) {
					thirdPartString = "1";
					forthPartString = temNum + "";
				} else if (temNum >= 255 && temNum <= 508) {
					thirdPartString = "2";
					forthPartString = (temNum - 254) + "";
				} else if (temNum >= 509 && temNum <= 762) {
					thirdPartString = "3";
					forthPartString = (temNum - 254 * 2) + "";
				} else if (temNum >= 763 && temNum <= 799) {
					thirdPartString = "4";
					forthPartString = (temNum - 254 * 3) + "";
				} else if (temNum >= 801 && temNum <= 819) {
					thirdPartString = "21";
					forthPartString = (temNum - 800) + "";
				} else if (temNum >= 821 && temNum <= 839) {
					thirdPartString = "22";
					forthPartString = (temNum - 820) + "";
				} else if (temNum >= 841 && temNum <= 849) {
					thirdPartString = "23";
					forthPartString = (temNum - 840) + "";
				} else if (temNum >= 851 && temNum <= 859) {
					thirdPartString = "24";
					forthPartString = (temNum - 850) + "";
				} else if (temNum >= 861 && temNum <= 879) {
					thirdPartString = "25";
					forthPartString = (temNum - 860) + "";
				} else {
					return null;
				}
			}
			ipString = ipString + "" + thirdPartString + "." + forthPartString;
			//通过协议转换映射 IP地址
			Logger.i(TAG, "ipString:" + ipString);
		}else {
			//拔号输入无效
			return ipString;
		}
		return ipString;
	}
	
	
	
}
