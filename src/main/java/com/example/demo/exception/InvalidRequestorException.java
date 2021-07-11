package com.example.demo.exception;

import lombok.Getter;
import lombok.ToString;

/**
 * api 요청자와 로그인한 회원이 다름
 */
@Getter
@ToString
public class InvalidRequestorException extends RuntimeException {
	private static final long serialVersionUID = 3402761568355368427L;
	private String requestor;
	private String memberId;
	
	public InvalidRequestorException(String requestor, String memberId) {
		super(requestor + "  " + memberId);
		this.requestor = requestor;
		this.memberId = memberId;
	}
}
