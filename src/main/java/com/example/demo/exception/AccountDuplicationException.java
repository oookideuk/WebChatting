package com.example.demo.exception;

import lombok.Getter;
import lombok.ToString;

/**
 * 중복된 아이디
 */
@Getter
@ToString
public class AccountDuplicationException extends RuntimeException {
	private static final long serialVersionUID = -4316913117598827482L;
	private String memberId;
	
	public AccountDuplicationException(String memberId) {
		super(memberId);
		this.memberId = memberId;
	}
}
