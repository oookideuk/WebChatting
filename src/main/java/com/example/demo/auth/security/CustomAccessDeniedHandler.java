package com.example.demo.auth.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.example.demo.exception.ErrorCode;

/**
 * 권한 없음
 * ajax O : 전역 예외로 처리한다.
 * ajax X : 403 error 페이지로 이동한다.
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	HandlerExceptionResolver handlerExceptionResolver;
	
	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
		String memberId = request.getUserPrincipal().getName();
		String uri = request.getRequestURI();
		logger.debug("memberId[{}] URI[{}] 권한 없음", memberId, uri);
		
		if(isAjaxRequest(request)) {
			handlerExceptionResolver.resolveException(request, response, null, accessDeniedException);
			return;
		}
		
		response.sendError(HttpStatus.FORBIDDEN.value(), ErrorCode.FORBIDDEN.getMessage());
	}

	
	/**
	 * ajax 요청인지 확인한다.
	 */
	private boolean isAjaxRequest(HttpServletRequest request) {
		String accept = request.getHeader("accept");
		String xRequested = request.getHeader("X-Requested-With");
		if(xRequested != null && xRequested.toLowerCase().equals("xmlhttprequest") ||
				accept != null && accept.toLowerCase().startsWith("application/json")){
			logger.debug("accept[{}] X-Requested-With[{}]", accept, xRequested);
			logger.debug("AJAX 요청");
			return true;
		}
		return false;
	}
}
