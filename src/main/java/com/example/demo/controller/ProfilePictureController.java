package com.example.demo.controller;

import java.io.IOException;

import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.auth.security.CustomUser;
import com.example.demo.dto.ProfilePictureDTO;
import com.example.demo.service.MemberService;
import com.example.demo.service.ProfilePictureService;

import ch.qos.logback.classic.Logger;

@RestController
@RequestMapping("/v1")
public class ProfilePictureController {
	private final Logger logger = (Logger) LoggerFactory.getLogger(this.getClass());
	private ProfilePictureService profilePictureService;
	private MemberService memberService;
	
	public ProfilePictureController(ProfilePictureService profilePictureService, MemberService memberService) {
		this.profilePictureService = profilePictureService;
		this.memberService = memberService;
	}
	
	/**
	 * 프로필 사진을 등록한다.
	 */
	@PostMapping("/profilePicture")
	public ResponseEntity<ProfilePictureDTO.Response> saveProfilePicture(ProfilePictureDTO.UploadReq uploadReq, Authentication auth) throws IOException{
		CustomUser user = (CustomUser) auth.getPrincipal();
		uploadReq.setMemberId(user.getMemberId());
		
		//프로필 사진을 등록한다.
		ProfilePictureDTO.Response profileRes = profilePictureService.saveProfilePicture(uploadReq);
		return new ResponseEntity<ProfilePictureDTO.Response>(profileRes, HttpStatus.OK);
	}
	
	/**
	 * 프로필 사진 정보를 조회한다.
	 */
	@GetMapping("/profilePicture/{memberId}")
	public ResponseEntity<ProfilePictureDTO.Response> findProfilePicture(@PathVariable String memberId) throws IOException{
		//회원가입 되어있는지 확인한다.
		memberService.checkRegisteredMember(memberId);
		//프로필 사진을 조회한다.
		ProfilePictureDTO.Response profileRes = profilePictureService.findProfilePictureByMemberId(memberId);
		return new ResponseEntity<ProfilePictureDTO.Response>(profileRes, HttpStatus.OK);	
	}
	
	/**
	 * 프로필 사진을 가져온다.
	 */
	@GetMapping("/file/profilePicture/{fileId}")
	public ResponseEntity<byte[]> findProfilePictureImage(@PathVariable Long fileId) throws IOException{
		HttpHeaders headers = new HttpHeaders();
		byte[] profilePictureBytes = profilePictureService.findProfilePictureImage(headers, fileId);
		return new ResponseEntity<byte[]>(profilePictureBytes, headers, HttpStatus.OK);
	}
	
	/**
	 * 프로필 사진을 삭제한다.
	 */
	@DeleteMapping("/profilePicture")
	public ResponseEntity<String> deleteProfilePicture(Authentication auth) throws IOException{
		CustomUser user = (CustomUser) auth.getPrincipal();
		//프로필 사진을 삭제한다.
		profilePictureService.deleteProfilePictureByMemberId(user.getMemberId());
		return new ResponseEntity<String>("삭제 성공", HttpStatus.OK);
	}
	
}
