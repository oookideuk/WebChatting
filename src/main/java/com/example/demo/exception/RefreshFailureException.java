package com.example.demo.exception;

public class RefreshFailureException extends RuntimeException {
	private static final long serialVersionUID = 8726886424142310887L;
	
	public RefreshFailureException() {
		super();
	}
	
	public RefreshFailureException(Exception e) {
		super(e);
	}
}
