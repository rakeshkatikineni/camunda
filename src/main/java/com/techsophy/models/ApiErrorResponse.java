package com.techsophy.models;

import org.springframework.http.HttpStatus;

public class ApiErrorResponse implements IApiResponse{
	private Object errors;
    private Boolean success;
    private String message;
    private HttpStatus statusCode;
    
    
    public HttpStatus getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(HttpStatus statusCode) {
		this.statusCode = statusCode;
	}

	public ApiErrorResponse() {
    }

    public ApiErrorResponse(Object errors, Boolean success, String message,HttpStatus statusCode) {
        this.errors = errors;
        this.success = success;
        this.message = message;
        this.statusCode=statusCode;
    }

    public Object getErrors() {
        return this.errors;
    }

    public void setErrors(Object errors) {
        this.errors = errors;
    }

    public Boolean getSuccess() {
        return this.success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
