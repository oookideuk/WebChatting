package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import javax.persistence.EntityNotFoundException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.demo.common.CustomMailSender;
import com.example.demo.domain.Role;
import com.example.demo.domain.repository.MemberRepository;
import com.example.demo.dto.MemberDTO;
import com.example.demo.exception.PasswordMismathException;
import com.example.demo.service.impl.MemberServiceImpl;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("local")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class MemberServiceTests {
	@Autowired private MemberService memberService;
	@Autowired private MemberRepository memberRepository;
	@Autowired private PasswordEncoder passwordEncoder;
	@Autowired private ProfilePictureService profilePictureService;
	@Autowired private CustomMailSender mailSender;
	@Value("${spring.mail.username}") private String email;
	
	@Order(1)
	@Test
	void signupTest() throws Exception {
		MemberDTO.SignupReq signupReq = MemberDTO.SignupReq.builder()
				.memberId("aaa")
				.password("aaa111")
				.email(email)
				.name("다라마")
				.role(Role.ROLE_MEMBER)
				.emailAuthKey("1234")
				.build();
		
		memberService.signup(signupReq);
	}
	
	@Order(2)
	@Test
	void saveMemberTest() {
		MemberDTO.SignupReq signupReq = MemberDTO.SignupReq.builder()
				.memberId("aaa")
				.password("aaa111")
				.email("aaa@aaa.com")
				.name("다라마")
				.role(Role.ROLE_MEMBER)
				.emailAuthKey("1234")
				.build();
		ReflectionTestUtils.invokeMethod(new MemberServiceImpl(memberRepository, passwordEncoder, profilePictureService, mailSender), "saveMember", signupReq);
	}
	
	@Order(3)
	@Test
	void sendAuthMailTest() {
		MemberDTO.SignupReq signupReq = MemberDTO.SignupReq.builder()
				.memberId("aaa")
				.email(email)
				.emailAuthKey("1234가")
				.build();
		
		ReflectionTestUtils.invokeMethod(new MemberServiceImpl(memberRepository, passwordEncoder, profilePictureService, mailSender), "sendAuthMail", signupReq);
	}
	
	@Order(4)
	@Test
	void findMemberTest() throws Exception {
		//가입된 회원
		assertEquals("aaa", memberService.findMember("aaa").getMemberId());
		//가입 안 된 회원
		assertThrows(EntityNotFoundException.class, () -> memberService.findMember("asldknasdlknasnl"), "memberEntity with memberId : " + "asldknasdlknasnl");
	}
	
	@Order(5)
	@Test
	void existsByMemberIdTest() {
		//가입된 회원
		assertEquals(true, memberService.existsByMemberId("aaa"));
		//가입 안 된 회원
		assertEquals(false, memberService.existsByMemberId("asdnlasndl"));
	}
	
	@Order(6)
	@Test
	void updatePersonalInfomationTest() throws IOException {
		MemberDTO.PersonalInfomationModificationReq req = null;
		MemberDTO.Response memberRes = null;
		//가입된 회원
		req = MemberDTO.PersonalInfomationModificationReq.builder()
				.memberId("aaa")
				.name("다라마바사아바1")
				.build();
		memberRes = memberService.updatePersonalInformation(req);
		assertEquals("다라마바사아바1", memberRes.getName());
		
		//가입 안 된 회원
		final MemberDTO.PersonalInfomationModificationReq req2 = MemberDTO.PersonalInfomationModificationReq.builder()
				.memberId("askfnalskfnaslkfnl")
				.name("다라마바사아")
				.build();
		Assertions.assertThrows(EntityNotFoundException.class, () -> memberService.updatePersonalInformation(req2), "memberEntity with memberId askfnalskfnaslkfnl");
	}
	
	@Order(7)
	@Test
	void updatePasswordTest() {
		//성공
		MemberDTO.passwordModificationReq req = MemberDTO.passwordModificationReq.builder()
				.memberId("aaa")
				.password("aaa111")
				.passwordChange("bbb111").build();
		memberService.updatePassword(req);
		assertEquals(true, passwordEncoder.matches("bbb111", memberRepository.findById("aaa").get().getPassword()));
		
		//패스워드 불일치
		req.setPassword("aslkd123aa");
		Assertions.assertThrows(PasswordMismathException.class, ()->memberService.updatePassword(req));		
		
		//가입 안 된 회원
		final MemberDTO.passwordModificationReq req2 = MemberDTO.passwordModificationReq.builder()
				.memberId("askfnalskfnaslkfnl")
				.password("aaa111")
				.passwordChange("abc123").build();
		Assertions.assertThrows(EntityNotFoundException.class, () -> memberService.updatePassword(req2), "memberEntity with memberId askfnalskfnaslkfnl");
		
		//패스워드 원복
		req.setPassword("bbb111");
		req.setPasswordChange("aaa111");
		memberService.updatePassword(req);
		assertEquals(true, passwordEncoder.matches("aaa111", memberRepository.findById("aaa").get().getPassword()));
	}
	
	@Order(8)
	@Test
	void deleteMemberTest() throws IOException {
		//가입된 회원
		memberService.deleteMember("aaa");
		//가입 안 된 회원
		assertThrows(EntityNotFoundException.class, ()-> memberService.deleteMember("asjkdnas"));
	}
}
