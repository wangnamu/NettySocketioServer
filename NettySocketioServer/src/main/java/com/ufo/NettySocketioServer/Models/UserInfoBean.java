package com.ufo.NettySocketioServer.Models;

import java.util.HashMap;
import java.util.Map;

public class UserInfoBean {

	// 设备Token(主键)
	private String DeviceToken;
	// 用户ID
	private String UserID;
	// 用户名
	private String UserName;
	// 真实姓名
	private String NickName;
	// 最近一次登录时间
	private long LoginTime = 0;
	// 设备类型
	private String DeviceType;
	// SessionID
	private String SessionID;
	// 未读信息数量
	private int UnReadCount = 0;
	//用户已经登录，但Socket未连接
	private Boolean IsSocketConnected = false;
	//服务器Token
	private String ServerToken;

	
	public String getDeviceToken() {
		return DeviceToken;
	}

	public void setDeviceToken(String deviceToken) {
		DeviceToken = deviceToken;
	}
	
	public String getUserID() {
		return UserID;
	}

	public void setUserID(String userID) {
		UserID = userID;
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

	

	public String getSessionID() {
		return SessionID;
	}

	public void setSessionID(String sessionID) {
		SessionID = sessionID;
	}

	public int getUnReadCount() {
		return UnReadCount;
	}

	public void setUnReadCount(int unReadCount) {
		UnReadCount = unReadCount;
	}

	public String getDeviceType() {
		return DeviceType;
	}

	public void setDeviceType(String deviceType) {
		DeviceType = deviceType;
	}
	
	public Boolean getIsSocketConnected() {
		return IsSocketConnected;
	}

	public void setIsSocketConnected(Boolean isSocketConnected) {
		IsSocketConnected = isSocketConnected;
	}

	public String getServerToken() {
		return ServerToken;
	}

	public void setServerToken(String serverToken) {
		ServerToken = serverToken;
	}


	public Map<String, String> toMap() {

		Map<String, String> map = new HashMap<>();
		map.put("UserID", UserID);
		map.put("UserName", UserName);
		map.put("NickName", NickName);
		map.put("LoginTime", String.valueOf(LoginTime));
		map.put("DeviceToken", DeviceToken);
		map.put("DeviceType", DeviceType);
		map.put("SessionID", SessionID);
		map.put("UnReadCount", String.valueOf(UnReadCount));
		map.put("IsSocketConnected", String.valueOf(IsSocketConnected));
		map.put("ServerToken", ServerToken);
		
		return map;
	}

	public UserInfoBean fromMap(Map<String, String> map) {

		this.setUserID(map.get("UserID"));
		this.setUserName(map.get("UserName"));
		this.setNickName(map.get("NickName"));
		this.setLoginTime(Long.valueOf(map.get("LoginTime")));
		this.setSessionID(map.get("SessionID"));
		this.setDeviceToken(map.get("DeviceToken"));
		this.setDeviceType(map.get("DeviceType"));
		this.setUnReadCount(Integer.valueOf(map.get("UnReadCount")));
		this.setIsSocketConnected(Boolean.valueOf(map.get("IsSocketConnected")));
		this.setServerToken(map.get("ServerToken"));
		
		return this;
	}

	public static UserInfoBean translateFromUserInfo(UserInfo userInfo, String sessionID,String serverToken) {

		UserInfoBean userInfoBean = new UserInfoBean();
		userInfoBean.setUserID(userInfo.getSID());
		userInfoBean.setUserName(userInfo.getUserName());
		userInfoBean.setNickName(userInfo.getNickName());
		userInfoBean.setLoginTime(userInfo.getLoginTime());
		userInfoBean.setSessionID(sessionID);
		userInfoBean.setDeviceToken(userInfo.getDeviceToken());
		userInfoBean.setDeviceType(userInfo.getDeviceType());
		userInfoBean.setUnReadCount(0);
		userInfoBean.setIsSocketConnected(true);
		userInfoBean.setServerToken(serverToken);
		
		return userInfoBean;
		
	}

}
