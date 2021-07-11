package com.example.demo.exception;

/**
 * DB에 토큰 정보가 없으면 발생한다.
 */
public class TokenNotFoundException extends RuntimeException {
	private static final long serialVersionUID = 5827217673594007543L;

	public TokenNotFoundException(String message) {
		super(message);
	}

}
