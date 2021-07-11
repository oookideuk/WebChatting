package com.example.demo.auth.jwt;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

public class JwtAuthenticationFilter extends GenericFilterBean {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private JwtTokenProvider jwtTokenProvider;
	
	public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
		this.jwtTokenProvider = jwtTokenProvider;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		try {
			//Request 헤더에서 JWT를 가져온다.
			String jwt = jwtTokenProvider.getTokenFromHeader((HttpServletRequest) request);
			HttpServletRequest httpRequest = (HttpServletRequest) request;
			logger.debug("METHOD[{}] URI[{}]", httpRequest.getMethod(), httpRequest.getRequestURI());
			
			//유효한 Access Token일 경우 SecurityContextHolder에 Authentication을 등록한다.
			if(StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt) && jwtTokenProvider.isAccessToken(jwt)) {
				Authentication authentication = jwtTokenProvider.getAuthentication(jwt);
				SecurityContextHolder.getContext().setAuthentication(authentication);
				logger.debug("set user authentication in security context. member[{}]", authentication.getPrincipal());
			}
		}catch(Exception e) {
			logger.debug("Could not set user authentication in security context[{}]", e.getMessage());
			
		}
		
		chain.doFilter(request, response);
	}

}
