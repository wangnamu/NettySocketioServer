package com.ufo.NettySocketioServer.Models;

import java.util.HashMap;

/*
 * 用户信息
 */
public class UserInfo {
	// 主键
	private String SID;
	// 用户名
	private String UserName;
	// 真实姓名
	private String NickName;
	// 最近一次登录时间
	private long LoginTime;
	// 设备类型
	private String DeviceType;
	// 设备证书
	private String DeviceToken;
	// 是否在线
	private boolean IsOnline = false;
	// 所属项目
	private String Project;
	// SessionID
	private String SessionID;

	public String getSID() {
		return SID;
	}

	public void setSID(String sID) {
		SID = sID;
	}

	public String getUserName() {
		return UserName;
	}

	public void setUserName(String userName) {
		UserName = userName;
	}

	public String getNickName() {
		return NickName;
	}

	public void setNickName(String nickName) {
		NickName = nickName;
	}

	public long getLoginTime() {
		return LoginTime;
	}

	public void setLoginTime(long loginTime) {
		LoginTime = loginTime;
	}

	public String getDeviceType() {
		return DeviceType;
	}

	public void setDeviceType(String deviceType) {
		DeviceType = deviceType;
	}

	public String getDeviceToken() {
		return DeviceToken;
	}

	public void setDeviceToken(String deviceToken) {
		DeviceToken = deviceToken;
	}

	public boolean isIsOnline() {
		return IsOnline;
	}

	public void setIsOnline(boolean isOnline) {
		IsOnline = isOnline;
	}

	public String getProject() {
		return Project;
	}

	public void setProject(String project) {
		Project = project;
	}

	public String getSessionID() {
		return SessionID;
	}

	public void setSessionID(String sessionID) {
		SessionID = sessionID;
	}

	public HashMap<String, String> toHashMap() {
		HashMap<String, String> hashMap = new HashMap<>();
		hashMap.put("SID", SID);
		hashMap.put("UserName", UserName);
		hashMap.put("NickName", NickName);
		hashMap.put("LoginTime", String.valueOf(LoginTime));
		hashMap.put("DeviceType", DeviceType);
		hashMap.put("DeviceToken", DeviceToken);
		hashMap.put("IsOnline", String.valueOf(IsOnline));
		hashMap.put("Project", Project);
		hashMap.put("SessionID", SessionID);
		return hashMap;
	}

	public UserInfo fromHashMap(HashMap<String, String> hashMap) {
		UserInfo userInfo = new UserInfo();
		userInfo.setSID(hashMap.get("SID"));
		userInfo.setUserName(hashMap.get("UserName"));
		userInfo.setNickName(hashMap.get("NickName"));
		userInfo.setLoginTime(Long.valueOf(hashMap.get("LoginTime")));
		userInfo.setDeviceType(hashMap.get("DeviceType"));
		userInfo.setDeviceToken(hashMap.get("DeviceToken"));
		userInfo.setIsOnline(Boolean.valueOf(hashMap.get("IsOnline")));
		userInfo.setProject(hashMap.get("Project"));
		userInfo.setSessionID(hashMap.get("SessionID"));
		return userInfo;
	}
}
