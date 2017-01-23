package com.ufo.NettySocketioServer.Models;

import java.util.HashMap;

public class UserInfoBean {

	// 主键
	private String SID;
	// 用户名
	private String UserName;
	// 真实姓名
	private String NickName;
	// 最近一次登录时间
	private long LoginTime;
	// 设备证书
	private String DeviceToken;
	// 所属项目
	private String Project;
	// SessionIDIOS
	private String SessionIDIOS;
	// SessionIDANDROID
	private String SessionIDANDROID;
	// SessionIDPC
	private String SessionIDPC;

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

	public String getDeviceToken() {
		return DeviceToken;
	}

	public void setDeviceToken(String deviceToken) {
		DeviceToken = deviceToken;
	}

	public String getProject() {
		return Project;
	}

	public void setProject(String project) {
		Project = project;
	}

	public void setSessionIDIOS(String sessionIDIOS) {
		SessionIDIOS = sessionIDIOS;
	}

	public String getSessionIDIOS() {
		return SessionIDIOS;
	}

	public String getSessionIDANDROID() {
		return SessionIDANDROID;
	}

	public void setSessionIDANDROID(String sessionIDANDROID) {
		SessionIDANDROID = sessionIDANDROID;
	}

	public String getSessionIDPC() {
		return SessionIDPC;
	}

	public void setSessionIDPC(String sessionIDPC) {
		SessionIDPC = sessionIDPC;
	}

	public HashMap<String, String> toHashMap() {

		HashMap<String, String> hashMap = new HashMap<>();
		hashMap.put("SID", SID);
		hashMap.put("UserName", UserName);
		hashMap.put("NickName", NickName);
		hashMap.put("LoginTime", String.valueOf(LoginTime));
		hashMap.put("DeviceToken", DeviceToken);
		hashMap.put("Project", Project);
		hashMap.put("SessionIDIOS", SessionIDIOS);
		hashMap.put("SessionIDANDROID", SessionIDANDROID);
		hashMap.put("SessionIDPC", SessionIDPC);

		return hashMap;
	}

	public UserInfoBean fromHashMap(HashMap<String, String> hashMap) {

		UserInfoBean userInfoBean = new UserInfoBean();
		userInfoBean.setSID(hashMap.get("SID"));
		userInfoBean.setUserName(hashMap.get("UserName"));
		userInfoBean.setNickName(hashMap.get("NickName"));
		userInfoBean.setLoginTime(Long.valueOf(hashMap.get("LoginTime")));
		userInfoBean.setDeviceToken(hashMap.get("DeviceToken"));
		userInfoBean.setProject(hashMap.get("Project"));
		userInfoBean.setSessionIDIOS(hashMap.get("SessionIDIOS"));
		userInfoBean.setSessionIDANDROID(hashMap.get("SessionIDANDROID"));
		userInfoBean.setSessionIDPC(hashMap.get("SessionIDPC"));

		return userInfoBean;
	}

	public static UserInfoBean translateFromUserInfo(UserInfo userInfo, String currentSessionID, String sessionIDIOS,
			String sessionIDANDROID, String sessionIDPC) {

		UserInfoBean userInfoBean = new UserInfoBean();
		userInfoBean.setSID(userInfo.getSID());
		userInfoBean.setUserName(userInfo.getUserName());
		userInfoBean.setNickName(userInfo.getNickName());
		userInfoBean.setLoginTime(userInfo.getLoginTime());
		userInfoBean.setProject(userInfo.getProject());
		userInfoBean.setDeviceToken(userInfo.getDeviceToken());

		if (userInfo.getDeviceType().equals(DeviceTypeEnum.ANDROID)) {
			userInfoBean.setSessionIDANDROID(currentSessionID);
			if (sessionIDPC != null) {
				userInfoBean.setSessionIDPC(sessionIDPC);
			}
		} else if (userInfo.getDeviceType().equals(DeviceTypeEnum.IOS)) {
			userInfoBean.setSessionIDIOS(currentSessionID);
			if (sessionIDPC != null) {
				userInfoBean.setSessionIDPC(sessionIDPC);
			}
		} else {
			userInfoBean.setSessionIDPC(currentSessionID);
			if (sessionIDANDROID != null) {
				userInfoBean.setSessionIDANDROID(sessionIDANDROID);
			}
			if (sessionIDIOS != null) {
				userInfoBean.setSessionIDIOS(sessionIDIOS);
			}
		}

		return userInfoBean;
	}

}
