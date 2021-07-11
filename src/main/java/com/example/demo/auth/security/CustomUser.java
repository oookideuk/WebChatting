package com.example.demo.auth.security;

import java.time.LocalDateTime;

import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;

import com.example.demo.domain.Role;
import com.example.demo.domain.entitiy.MemberEntity;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString
public class CustomUser extends User {
	private static final long serialVersionUID = 9052325901332428235L;
	private String memberId;
	private String name;
	private Role role;
	private String email;
	LocalDateTime registerDate;
	private int emailAuthFlag;	// 0: 미인증, 1: 인증
	
	public CustomUser(MemberEntity member) {
		super(member.getMemberId(), member.getPassword(), AuthorityUtils.createAuthorityList(member.getRole().getValue()));
		this.emailAuthFlag = member.getEmailAuthFlag();
		this.memberId = member.getMemberId();
		this.name = member.getName();
		this.role = member.getRole();
		this.email = member.getEmail();
		this.registerDate = member.getRegisterDate();
	}
}
