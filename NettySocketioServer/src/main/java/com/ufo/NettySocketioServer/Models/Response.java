package com.ufo.NettySocketioServer.Models;

public class Response {
	
	private Boolean IsSuccess;
	private String Message;
	
	public Boolean getIsSuccess() {
		return IsSuccess;
	}
	public void setIsSuccess(Boolean isSuccess) {
		IsSuccess = isSuccess;
	}
	public String getMessage() {
		return Message;
	}
	public void setMessage(String message) {
		Message = message;
	}
	
}
