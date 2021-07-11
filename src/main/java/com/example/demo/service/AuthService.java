package com.example.demo.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.ResponseEntity;

import com.example.demo.dto.AuthDTO;
import com.example.demo.dto.AuthDTO.JwtAuthRes;
import com.example.demo.dto.AuthDTO.LoginReq;

public interface AuthService {

	/**
	 * 로그인 한다.
	 */
	ResponseEntity<AuthDTO.JwtAuthRes> login(LoginReq loginReq);

	/**
	 * 로그아웃 한다.
	 */
	String logout(HttpServletRequest request, HttpServletResponse response);

	/**
	 * 새로운 액세스 토큰을 발급한다.
	 */
	ResponseEntity<JwtAuthRes> refresh(HttpServletRequest request);

}
