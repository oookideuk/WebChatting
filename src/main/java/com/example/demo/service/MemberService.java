package com.example.demo.service;

import java.io.IOException;

import javax.mail.MessagingException;

import com.example.demo.dto.MemberDTO;

public interface MemberService {
	
	/**
	 * 회원가입을 한다.
	 */
	MemberDTO.Response signup(MemberDTO.SignupReq signupReq) throws MessagingException, IOException;
	
	/**
	 * 회원정보를 가져온다.
	 */
	MemberDTO.Response findMember(String memberId);
	
	/**
	 * 회원 탈퇴 한다.
	 */
	void deleteMember(String memberId) throws IOException;
	
	/**
	 * 회원정보를 수정한다.
	 */
	MemberDTO.Response updatePersonalInformation(MemberDTO.PersonalInfomationModificationReq req);
	
	/**
	 * 비밀번호를 수정한다.
	 */
	void updatePassword(MemberDTO.passwordModificationReq req);
	
	/**
	 * 이메일 플래그를 활성화한다.
	 */
	void enableEmailAuthFlag(String memberId, String emailAuthKey);
	
	/**
	 * 가입된 아이디 인지 확인하며 가입 안 되어 있다면 AccountNotFoundException을 발생시킨다.
	 */
	void checkRegisteredMember(String memberId);
	
	/**
	 * 가입된 아이디 인지 확인하며 가입되어 있다면 AccountDuplicationException을 발생시킨다.
	 */
	void checkDuplicatedMember(String memberId);
	
	/**
	 * 가입된 회원인지 확인한다.
	 */
	boolean existsByMemberId(String memberId);
}
