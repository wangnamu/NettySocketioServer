package com.ufo.NettySocketioServer.Models;

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
	// 设备证书
	private String DeviceToken;
	// 设备类型
	private String DeviceType;
	// 检查登录状态
	private Boolean CheckStatus = false;


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

	public String getDeviceType() {
		return DeviceType;
	}

	public void setDeviceType(String deviceType) {
		DeviceType = deviceType;
	}

	public Boolean getCheckStatus() {
		return CheckStatus;
	}

	public void setCheckStatus(Boolean checkStatus) {
		CheckStatus = checkStatus;
	}
}
