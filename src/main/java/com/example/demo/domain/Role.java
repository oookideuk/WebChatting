package com.example.demo.domain;

import lombok.Getter;

@Getter
public enum Role {
	ROLE_MEMBER("ROLE_MEMBER")
	, ROLE_ADMIN("ROLE_ADMIN");

	private String value;
	private Role(String value) {
		this.value = value;
	}
}
