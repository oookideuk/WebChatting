package com.example.demo.controller;

import java.io.IOException;

import javax.mail.MessagingException;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import com.example.demo.auth.security.CustomUser;
import com.example.demo.dto.MemberDTO;
import com.example.demo.service.MemberService;

@RestController
@RequestMapping("/v1")
public class MemberController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private MemberService memberService;
	
	public MemberController(MemberService memberService) {
		this.memberService = memberService;
	}
	
	/**
	 * 회원가입을 한다. 
	 */
	@PostMapping("/members")
	public ResponseEntity<MemberDTO.Response> signup(@Valid MemberDTO.SignupReq signupReq) throws MessagingException, IOException {
		//중복된 아이디인지 확인한다.
		memberService.checkDuplicatedMember(signupReq.getMemberId());
		//회원가입한다.
		MemberDTO.Response memberRes = memberService.signup(signupReq);
		return new ResponseEntity<MemberDTO.Response>(memberRes, HttpStatus.OK);
	}
	
	/**
	 * 회원정보를 가져온다.
	 */
	@GetMapping("/members/{memberId}")
	public ResponseEntity<MemberDTO.Response> findMember(@PathVariable String memberId) throws IOException {
		//memberId가 가입 안 되어 있다면 AccountNotFoundException을 발생시킨다.
		memberService.checkRegisteredMember(memberId);
		
		MemberDTO.Response memberRes = memberService.findMember(memberId);
		return new ResponseEntity<MemberDTO.Response>(memberRes, HttpStatus.OK);
	}
	
	/**
	 * 회원 탈퇴 한다. 
	 */
	@DeleteMapping("/members/me")
	public ResponseEntity<String> deleteMember(Authentication auth) throws IOException{		
		CustomUser user = (CustomUser) auth.getPrincipal();
		
		//회원탈퇴 한다.
		memberService.deleteMember(user.getMemberId());
		return new ResponseEntity<String>("탈퇴 성공", HttpStatus.OK);
	}
	
	/**
	 * 회원정보를 수정한다.
	 * 프로필 사진은 개별적으로 수정한다.
	 */
	@PutMapping("/members/me")
	public ResponseEntity<MemberDTO.Response> updatePersonalInfomation(@Valid MemberDTO.PersonalInfomationModificationReq req, Authentication auth) {
		CustomUser user = (CustomUser) auth.getPrincipal();
		req.setMemberId(user.getMemberId());
		//회원정보를 수정한다.
		MemberDTO.Response memberRes = memberService.updatePersonalInformation(req);
		return new ResponseEntity<MemberDTO.Response>(memberRes, HttpStatus.OK);
	}
	
	/**
	 * 비밀번호를 수정한다.
	 */
	@PutMapping("/members/me/password")
	public ResponseEntity<String> updatePassword(@Valid MemberDTO.passwordModificationReq req, Authentication auth){
		CustomUser user = (CustomUser) auth.getPrincipal();
		req.setMemberId(user.getMemberId());
		//패스워드를 수정한다.
		memberService.updatePassword(req);
		return new ResponseEntity<String>("패스워드 수정 성공", HttpStatus.OK);
	}
	
	/**
	 * 이메일 인증 한다.
	 */
	@GetMapping("/members/me/email/auth")
	public RedirectView enableEmailAuthFlag(String memberId, String emailAuthKey) {
		logger.debug("+++++++++++++++++++++++++++++++++++++++++++");
		logger.debug("memberId [{}] emailKey [{}]", memberId, emailAuthKey);
		memberService.enableEmailAuthFlag(memberId, emailAuthKey);
		return new RedirectView("/login");
	}
	
}
