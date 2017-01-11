package com.ufo.NettySocketioServer.Models;

import java.util.HashSet;

/*
 * 消息
 */
public class Message {

	/**
	 * // 主键 private String SID; // 发送人ID private String SenderID; // 接收人ID
	 * private String ReceiverID; // 标题 private String Alert; // 内容 private
	 * String Body; // Base64数据流 private String DataStream; // 时间 private long
	 * Time; // 链接 private String Url;
	 * 
	 * // 是否已读 private boolean IsReaded; // 发送类型(私聊，群聊，广播) private String
	 * PushType; // 发送完成 private boolean IsPushFinish;
	 * 
	 * // 项目 private String Project; // 业务 private String Business; // 模块
	 * private String Module; // 组(群)ID private String GroupID; // 数据ID private
	 * String DataID; // 数据类型(文字、图片、文件等) private String DataType;
	 * 
	 * // 发送人名称 private String SenderRealName; // 发送人头像 private String
	 * SenderHeadPortrait;
	 * 
	 * 
	 * public String getSID() { return SID; } public void setSID(String sID) {
	 * SID = sID; } public String getSenderID() { return SenderID; } public void
	 * setSenderID(String senderID) { SenderID = senderID; } public String
	 * getReceiverID() { return ReceiverID; } public void setReceiverID(String
	 * receiverID) { ReceiverID = receiverID; } public String getAlert() {
	 * return Alert; } public void setAlert(String alert) { Alert = alert; }
	 * public String getBody() { return Body; } public void setBody(String body)
	 * { Body = body; } public String getDataStream() { return DataStream; }
	 * public void setDataStream(String dataStream) { DataStream = dataStream; }
	 * public long getTime() { return Time; } public void setTime(long time) {
	 * Time = time; } public String getUrl() { return Url; } public void
	 * setUrl(String url) { Url = url; } public boolean isIsReaded() { return
	 * IsReaded; } public void setIsReaded(boolean isReaded) { IsReaded =
	 * isReaded; } public String getPushType() { return PushType; } public void
	 * setPushType(String pushType) { PushType = pushType; } public boolean
	 * isIsPushFinish() { return IsPushFinish; } public void
	 * setIsPushFinish(boolean isPushFinish) { IsPushFinish = isPushFinish; }
	 * public String getProject() { return Project; } public void
	 * setProject(String project) { Project = project; } public String
	 * getBusiness() { return Business; } public void setBusiness(String
	 * business) { Business = business; } public String getModule() { return
	 * Module; } public void setModule(String module) { Module = module; }
	 * public String getGroupID() { return GroupID; } public void
	 * setGroupID(String groupID) { GroupID = groupID; } public String
	 * getDataID() { return DataID; } public void setDataID(String dataID) {
	 * DataID = dataID; } public String getDataType() { return DataType; }
	 * public void setDataType(String dataType) { DataType = dataType; } public
	 * String getSenderRealName() { return SenderRealName; } public void
	 * setSenderRealName(String senderRealName) { SenderRealName =
	 * senderRealName; } public String getSenderHeadPortrait() { return
	 * SenderHeadPortrait; } public void setSenderHeadPortrait(String
	 * senderHeadPortrait) { SenderHeadPortrait = senderHeadPortrait; }
	 **/

	// 主键
	private String SID;
	// 发送人ID
	private String SenderID;
	// 接收人ID
	private HashSet<String> ReceiverIDs;
	// 标题
	private String Alert;
	// 时间
	private long Time;
	// 消息类型(文字、图片、文件、链接、音频、视频、表情等)
	private String MessageType;
	// 提醒
	private Boolean IsAlert;
	// 自定义
	private Object Others;
	
	public String getSID() {
		return SID;
	}
	public void setSID(String sID) {
		SID = sID;
	}
	public String getSenderID() {
		return SenderID;
	}
	public void setSenderID(String senderID) {
		SenderID = senderID;
	}
	public HashSet<String> getReceiverIDs() {
		return ReceiverIDs;
	}
	public void setReceiverIDs(HashSet<String> receiverIDs) {
		ReceiverIDs = receiverIDs;
	}
	public String getAlert() {
		return Alert;
	}
	public void setAlert(String alert) {
		Alert = alert;
	}
	public long getTime() {
		return Time;
	}
	public void setTime(long time) {
		Time = time;
	}
	public String getMessageType() {
		return MessageType;
	}
	public void setMessageType(String messageType) {
		MessageType = messageType;
	}
	public Boolean getIsAlert() {
		return IsAlert;
	}
	public void setIsAlert(Boolean isAlert) {
		IsAlert = isAlert;
	}
	public Object getOthers() {
		return Others;
	}
	public void setOthers(Object others) {
		Others = others;
	}

}
