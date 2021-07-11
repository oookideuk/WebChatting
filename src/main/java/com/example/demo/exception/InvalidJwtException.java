package com.example.demo.exception;

public class InvalidJwtException extends RuntimeException {
	private static final long serialVersionUID = -2688540275677261277L;

	public InvalidJwtException() {
		super();
	}

	public InvalidJwtException(Exception e) {
		super(e);
	}

}
