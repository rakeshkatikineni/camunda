package com.techsophy.models;

import org.springframework.http.HttpStatus;

public class ApiSuccessResponse implements IApiResponse{
	private Object data;
    private Boolean success;
    private String message;
    private HttpStatus statusCode;
    public ApiSuccessResponse(Object data, Boolean success, String message,HttpStatus statusCode) {
        this.data = data;
        this.success = success;
        this.message = message;
        this.statusCode=statusCode;
    }
    
    
	public HttpStatus getStatusCode() {
		return statusCode;
	}


	public void setStatusCode(HttpStatus statusCode) {
		this.statusCode = statusCode;
	}


	public Object getData() {
		return data;
	}
	public void setData(Object data) {
		this.data = data;
	}
	public Boolean getSuccess() {
		return success;
	}
	public void setSuccess(Boolean success) {
		this.success = success;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}

}
