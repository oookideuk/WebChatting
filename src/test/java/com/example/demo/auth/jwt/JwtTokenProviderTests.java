package com.example.demo.auth.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.example.demo.auth.security.CustomUser;
import com.example.demo.domain.Role;
import com.example.demo.domain.entitiy.MemberEntity;
import com.example.demo.exception.InvalidJwtException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class JwtTokenProviderTests {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private JwtTokenProvider jwtTokenProvider; 
	public static final String AUTHORIZATION_HEADER = "Authorization";
	@Value("${app.jwt.secret}") private String secret;
	@Value("${app.jwt.access.type}") private String accessType;
	@Value("${app.jwt.access.valid-second}") private long accessValidSecond;
	@Value("${app.jwt.access.cookie.name}") private String accessCookieName;
	@Value("${app.jwt.refresh.type}") private String refreshType;
	@Value("${app.jwt.refresh.valid-second}") private long refreshValidSecond;
	@Value("${app.jwt.refresh.cookie.name}") private String refreshCookieName;
	
	//secret=jwttest, memberId=aaa, jwi=aaa, name=가나다abc123, exp=948316196966
	private static final String VALID_ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJhYWEiLCJ0eXBlIjoiYWNjZXNzIiwibWVtYmVySWQiOiJhYWEiLCJuYW1lIjoi6rCA64KY64ukYWJjMTIzIiwicmVnaXN0ZXJEYXRlIjoiMjAyMC0xMi0xMVQwNjo0OToyNi44OTQiLCJpYXQiOjE2MDc2MzY5NjYsImV4cCI6OTQ4MzE2MTk2OTY2fQ.4VirpCJIp4vKk1XtokI_cYGvXCsz5DdLM6hGxF17XTs";
	//secret=jwttest, memberId=aaa, jwi=aaa, name=가나다abc123, exp=1607637066
	private static final String EXPIRED_ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJhYWEiLCJ0eXBlIjoiYWNjZXNzIiwibWVtYmVySWQiOiJhYWEiLCJuYW1lIjoi6rCA64KY64ukYWJjMTIzIiwicmVnaXN0ZXJEYXRlIjoiMjAyMC0xMi0xMVQwNjo1MDowNi44NyIsImlhdCI6MTYwNzYzNzAwNiwiZXhwIjoxNjA3NjM3MDY2fQ.1MHvRybBViwD4EB2eCrKuCQofJJ2dsVeox3bQh9q55s";
	//secret=jwttest, memberId=dlksnadks, jwi=dlksnadks, name=가나다abc123, exp=948316197133
	private static final String INVALID_MEMBER_ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJkbGtzbmFka3MiLCJ0eXBlIjoiYWNjZXNzIiwibWVtYmVySWQiOiJkbGtzbmFka3MiLCJuYW1lIjoi6rCA64KY64ukYWJjMTIzIiwicmVnaXN0ZXJEYXRlIjoiMjAyMC0xMi0xMVQwNjo1MjoxMy45MDciLCJpYXQiOjE2MDc2MzcxMzMsImV4cCI6OTQ4MzE2MTk3MTMzfQ.nb6clKQ_CJNryXvMLKrr7t9mN31ZdCFL8LEif4lhRfk";
	//secret=abcdef, memberId=aaa, jwi=aaa, name=가나다abc123, exp=948316197088
	private static final String INVALID_SECRET_ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJhYWEiLCJ0eXBlIjoiYWNjZXNzIiwibWVtYmVySWQiOiJhYWEiLCJuYW1lIjoi6rCA64KY64ukYWJjMTIzIiwicmVnaXN0ZXJEYXRlIjoiMjAyMC0xMi0xMVQwNjo1MToyOC4zNCIsImlhdCI6MTYwNzYzNzA4OCwiZXhwIjo5NDgzMTYxOTcwODh9.KyX8TC86ehR1Xv9bsvLwm3WS2bHNJT-rmi2EsyCy3Z0";

	@Test
	void generateTokenTest() {
		Claims decodedClaims;
		MemberEntity member = MemberEntity.builder()
				.memberId("aaa")
				.password("1234")
				.role(Role.ROLE_MEMBER).build();
		CustomUser user = new CustomUser(member);
		String jti = UUID.randomUUID().toString();
		
		//access token test
		Claims accessClaims = jwtTokenProvider.generateAccessClaims(jti, user);
		String accessToken = jwtTokenProvider.generateToken(accessClaims);
		decodedClaims = Jwts.parser().setSigningKey(secret).parseClaimsJws(accessToken).getBody();
		assertEquals("aaa", decodedClaims.get("memberId"));
		assertEquals(accessType, decodedClaims.get("type"));
		logger.debug("{}", decodedClaims);
		
		//refresh token test
		Claims refreshClaims = jwtTokenProvider.generateRefreshClaims(jti, user);
		String refreshToken = jwtTokenProvider.generateToken(refreshClaims);
		decodedClaims = Jwts.parser().setSigningKey(secret).parseClaimsJws(refreshToken).getBody();
		assertEquals("aaa", decodedClaims.get("memberId"));
		assertEquals(refreshType, decodedClaims.get("type"));
		logger.debug("{}", decodedClaims);
		
		logger.debug("accressToken[{}]", accessToken);
		logger.debug("refreshToken[{}]", refreshToken);
	}
	
	@Test
	void generateAccessClaimsTest() {
		MemberEntity member = MemberEntity.builder()
				.memberId("aaa")
				.password("1234")
				.role(Role.ROLE_MEMBER).build();
		CustomUser user = new CustomUser(member);
		String jti = UUID.randomUUID().toString();
		
		Claims claims = jwtTokenProvider.generateAccessClaims(jti, user);
		assertEquals(accessType, claims.get("type"));
		assertEquals("aaa", claims.get("memberId"));
	}
	
	@Test void generateRefreshClaimsTest(){
		MemberEntity member = MemberEntity.builder()
				.memberId("aaa")
				.password("1234")
				.role(Role.ROLE_MEMBER).build();
		CustomUser user = new CustomUser(member);
		String jti = UUID.randomUUID().toString();
		
		Claims claims = jwtTokenProvider.generateRefreshClaims(jti, user);
		assertEquals(refreshType, claims.get("type"));
		assertEquals("aaa", claims.get("memberId"));
	}
	
	@Test
	void isAccessTokenTest() {
		MemberEntity member = MemberEntity.builder()
				.memberId("aaa")
				.password("1234")
				.role(Role.ROLE_MEMBER).build();
		CustomUser user = new CustomUser(member);
		String jti = UUID.randomUUID().toString();
		Claims accessClaims = jwtTokenProvider.generateAccessClaims(jti, user);
		String accessToken = jwtTokenProvider.generateToken(accessClaims);
		Claims refreshClaims = jwtTokenProvider.generateRefreshClaims(jti, user);
		String refreshToken = jwtTokenProvider.generateToken(refreshClaims);
		
		assertEquals(true, jwtTokenProvider.isAccessToken(accessToken));
		assertEquals(false, jwtTokenProvider.isAccessToken(refreshToken));
	}
	
	@Test
	void isRefreshTokenTest() {
		MemberEntity member = MemberEntity.builder()
				.memberId("aaa")
				.password("1234")
				.role(Role.ROLE_MEMBER).build();
		CustomUser user = new CustomUser(member);
		String jti = UUID.randomUUID().toString();
		Claims accessClaims = jwtTokenProvider.generateAccessClaims(jti, user);
		String accessToken = jwtTokenProvider.generateToken(accessClaims);
		Claims refreshClaims = jwtTokenProvider.generateRefreshClaims(jti, user);
		String refreshToken = jwtTokenProvider.generateToken(refreshClaims);
		
		assertEquals(true, jwtTokenProvider.isRefreshToken(refreshToken));
		assertEquals(false, jwtTokenProvider.isRefreshToken(accessToken));
	}
	
	@Test
	void getAuthenticationTest() {
		Authentication authectication = jwtTokenProvider.getAuthentication(VALID_ACCESS_TOKEN);
		CustomUser user = (CustomUser) authectication.getPrincipal();
		assertEquals(true, user.getMemberId().equals("aaa"));
		logger.debug("{}", authectication);
		
		//토큰은 있지만 비회원인 경우
		assertThrows(UsernameNotFoundException.class, () -> jwtTokenProvider.getAuthentication(INVALID_MEMBER_ACCESS_TOKEN));
	}
	
	@Test
	void validateTokenTest() {
		assertEquals(true, jwtTokenProvider.validateToken(VALID_ACCESS_TOKEN));
		
		//expried token
		assertThrows(InvalidJwtException.class, () -> jwtTokenProvider.validateToken(EXPIRED_ACCESS_TOKEN));
		
		//invalid secretKey
		assertThrows(InvalidJwtException.class, () -> jwtTokenProvider.validateToken(INVALID_SECRET_ACCESS_TOKEN));
	
		//token is null or empty
		assertThrows(InvalidJwtException.class, () -> jwtTokenProvider.validateToken(null));
		assertThrows(InvalidJwtException.class, () -> jwtTokenProvider.validateToken(""));
		
		//token is invalid value.
		assertThrows(InvalidJwtException.class, () -> jwtTokenProvider.validateToken("asdass"));
	}
	
	@Test
	void getExpirationDateTest() {
		assertEquals(LocalDateTime.parse("+32020-12-11T06:49:26"), jwtTokenProvider.getExpirationDate(VALID_ACCESS_TOKEN));
		
		//만료된 토큰
		assertThrows(InvalidJwtException.class, () -> jwtTokenProvider.getExpirationDate(EXPIRED_ACCESS_TOKEN));
	}
	
	@Test
	void getTokenFromHeaderTest() {
		MockHttpServletRequest request = null;
		
		String token = "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJhYWEiLCJtZW1iZXJJZCI6ImFhYSIsIm5hbWUiOiLqsIDrgpjri6RhYmMxMjMiLCJyZWdpc3RlckRhdGUiOiIyMDIwLTEyLTA5VDIxOjQ0OjI3LjkwNSIsImF1dGhvcml0aWVzIjpbeyJhdXRob3JpdHkiOiJST0xFX01FTUJFUiJ9XSwiaWF0IjoxNjA3NTE3ODY3LCJleHAiOjQ3NjMxOTE0Njd9.cRvaIJEFe04s6ayOQ8CWKtGXdKGRDFZHOqHBlAUJKM0";
		request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer "+token);
		assertEquals(token, jwtTokenProvider.getTokenFromHeader(request));
		
		//잘못된 Authorization 값
		request = new MockHttpServletRequest();
		request.addHeader(AUTHORIZATION_HEADER, "abc");
		assertEquals(null, jwtTokenProvider.getTokenFromHeader(request));
		
		//Authorization 헤더 없음
		request = new MockHttpServletRequest();
		assertEquals(null, jwtTokenProvider.getTokenFromHeader(request));
		
		//request == null
		//assertThrows(NullPointerException.class, () -> jwtTokenProvider.getTokenFromHeader(null));
		
	}
	
}
