package com.ufo.NettySocketioServer.Models;

import java.util.HashSet;

/*
 * 消息
 */
public class Message {

	// 主键
	private String SID;
	// 发送人ID
	private String SenderID;
	// 发送人设备编号
	private String SenderDeviceToken;
	// 接收人ID
	private HashSet<String> ReceiverIDs;
	// 标题
	private String Title;
	// 内容
	private String Body;
	// 时间
	private long Time;
	// 消息类型(文字、图片、文件、链接、音频、视频、表情等)
	private String MessageType;
	// 提醒
	private Boolean IsAlert;
	// 针对ios10和androidN的快捷回复功能，Category字段表示快捷方式分类
	private String Category = "custom";
	// 自定义内容类型
	private String OthersType;
	// 自定义内容
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
	
	public String getSenderDeviceToken() {
		return SenderDeviceToken;
	}

	public void setSenderDeviceToken(String senderDeviceToken) {
		SenderDeviceToken = senderDeviceToken;
	}

	public HashSet<String> getReceiverIDs() {
		return ReceiverIDs;
	}

	public void setReceiverIDs(HashSet<String> receiverIDs) {
		ReceiverIDs = receiverIDs;
	}

	public String getTitle() {
		return Title;
	}

	public void setTitle(String title) {
		Title = title;
	}

	public String getBody() {
		return Body;
	}

	public void setBody(String body) {
		Body = body;
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

	public String getCategory() {
		return Category;
	}

	public void setCategory(String category) {
		Category = category;
	}

	public String getOthersType() {
		return OthersType;
	}

	public void setOthersType(String othersType) {
		OthersType = othersType;
	}

	public Object getOthers() {
		return Others;
	}

	public void setOthers(Object others) {
		Others = others;
	}
	

}
