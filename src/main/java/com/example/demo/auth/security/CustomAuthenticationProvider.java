package com.example.demo.auth.security;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {
	@Autowired
	CustomUserDetailsService userDetailService;
	@Autowired
	PasswordEncoder passwordEncoder;
	
	
	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		String username = authentication.getName();
		String password = (String) authentication.getCredentials();
		Collection<? extends GrantedAuthority> authorities;
		CustomUser user = (CustomUser) userDetailService.loadUserByUsername(username);
		authorities = user.getAuthorities();
		
		if(!passwordEncoder.matches(password, user.getPassword())){ //패스워드 불일치
			throw new BadCredentialsException(username + " 패스워드 불일치");
		}else if(user.getEmailAuthFlag() == 0){ //이메일 미인증
			throw new DisabledException(username + " 이메일 미인증");
		}
		
		return new UsernamePasswordAuthenticationToken(user, password, authorities);
	}
	
	@Override
	public boolean supports(Class<?> authentication) {
		return authentication.equals(UsernamePasswordAuthenticationToken.class);
	}
	
	
}
