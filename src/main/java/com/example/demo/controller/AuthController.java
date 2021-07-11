package com.example.demo.controller;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.AuthDTO;
import com.example.demo.service.AuthService;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private AuthService authService;
	
	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	/**
	 * 로그인 한다.
	 */
	@PostMapping("/login")
	public ResponseEntity<AuthDTO.JwtAuthRes> login(@Valid AuthDTO.LoginReq loginReq){
		return authService.login(loginReq);
	}
	
	/**
	 * 로그아웃 한다.
	 */
	@PostMapping("/logout")
	public ResponseEntity<String> logout(HttpServletRequest request, HttpServletResponse response){
		String memberId = authService.logout(request, response);
		return new ResponseEntity<String>(memberId, HttpStatus.OK);
	}
	
	/**
	 * Access Token 만료시 Refresh 한다.
	 * Refresh Token 만료 인접시 새로운 Refresh Token 발급한다. 
	 */
	@PostMapping("refresh")
	public ResponseEntity<AuthDTO.JwtAuthRes> refresh(HttpServletRequest request){
		return authService.refresh(request);
	}
	
	/**
	 * 로그인한 회원 아이디를 가져온다.
	 */
	@GetMapping("/me")
	public ResponseEntity<String> me(Principal principal){
		return new ResponseEntity<String>(principal.getName(), HttpStatus.OK);
	}
}
