package com.ufo.NettySocketioServer.Models;

public class Notify {
	
	private String UserID;
	private String SourceDeviceType;
	private Object Others;
	
	public String getUserID() {
		return UserID;
	}
	public void setUserID(String userID) {
		UserID = userID;
	}
	public String getSourceDeviceType() {
		return SourceDeviceType;
	}
	public void setSourceDeviceType(String sourceDeviceType) {
		SourceDeviceType = sourceDeviceType;
	}
	public Object getOthers() {
		return Others;
	}
	public void setOthers(Object others) {
		Others = others;
	}
	
}
