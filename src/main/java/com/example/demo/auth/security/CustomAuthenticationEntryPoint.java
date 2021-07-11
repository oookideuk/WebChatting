package com.example.demo.auth.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * 미인증
 */
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private HandlerExceptionResolver handlerExceptionResolver;
	
	public CustomAuthenticationEntryPoint(HandlerExceptionResolver handlerExceptionResolver) {
		this.handlerExceptionResolver = handlerExceptionResolver;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
		logger.debug("로그인 필요");
		//rest api 호출이면 json 에러 메시지를 반환해야한다.
		if(isAjaxRequest(request) || isApiCall(request)) {
			handlerExceptionResolver.resolveException(request, response, null, authException);
			return;
		}
		
		handlerExceptionResolver.resolveException(request, response, null, authException);
		return;
	}
	
	/**
	 * ajax 요청인지 확인한다.
	 */
	private boolean isAjaxRequest(HttpServletRequest request) {
		String accept = request.getHeader("accept");
		String xRequested = request.getHeader("X-Requested-With");
		if(xRequested != null && xRequested.toLowerCase().equals("xmlhttprequest")
				|| accept != null && accept.toLowerCase().startsWith("application/json")){
			return true;
		}
		return false;
	}
	
	private boolean isApiCall(HttpServletRequest request) {
		String uri = request.getRequestURI();
		if(uri.startsWith("/v1")) {
			return true;
		}
		return false;
	}
}
