package com.example.demo.exception;

/**
 * 패스워드 불일치
 */
public class PasswordMismathException extends RuntimeException {
	private static final long serialVersionUID = -9023789203973357806L;
	public PasswordMismathException(String message) {
		super(message);
	}
}
