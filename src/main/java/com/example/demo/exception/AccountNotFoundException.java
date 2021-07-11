package com.example.demo.exception;

import lombok.Getter;
import lombok.ToString;

/**
 * 회원을 찾을 수 없음.
 */
@Getter
@ToString
public class AccountNotFoundException extends RuntimeException {
	private static final long serialVersionUID = 1999343683107858097L;
	private String memberId;
	
	public AccountNotFoundException(String memberId) {
		super(memberId);
		this.memberId = memberId;
	}
}
