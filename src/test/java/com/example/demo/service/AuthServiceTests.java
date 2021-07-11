package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.demo.auth.jwt.JwtTokenProvider;
import com.example.demo.auth.security.CustomUserDetailsService;
import com.example.demo.domain.Role;
import com.example.demo.domain.entitiy.MemberEntity;
import com.example.demo.domain.repository.JwtTokenRepository;
import com.example.demo.domain.repository.MemberRepository;
import com.example.demo.dto.AuthDTO;
import com.example.demo.service.impl.AuthServiceImpl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@SpringBootTest
public class AuthServiceTests {
	@Autowired private AuthServiceImpl authService;
	@Autowired private JwtTokenRepository jwtRepo;
	@Autowired private MemberRepository memberRepo;
	@Autowired private PasswordEncoder passwordEncoder;
	@Autowired private JwtTokenProvider jwtTokenProvider;
	@Value("${app.jwt.secret}") private String secret;
	@Value("${app.jwt.access.cookie.name}") private String accessCookieName;
	@Value("${app.jwt.refresh.cookie.name}") private String refreshCookieName;
	private String memberIdAAA = "aaa";
	
	@BeforeEach
	void setup() {
		if(!memberRepo.findById(memberIdAAA).isPresent()) {
			MemberEntity member = MemberEntity.builder()
				.memberId(memberIdAAA)
				.password(passwordEncoder.encode("aaa111"))
				.name("테스트aaa")
				.role(Role.ROLE_MEMBER)
				.email("test.com")
				.emailAuthKey("test")
				.emailAuthFlag(1).build();
			memberRepo.save(member);
		}
	}
	
	@Test
	void loginTest() {
		AuthDTO.LoginReq loginReq = AuthDTO.LoginReq.builder().memberId(memberIdAAA).password("aaa111").build();
		
		ResponseEntity<AuthDTO.JwtAuthRes> responseEntity = authService.login(loginReq);
		AuthDTO.JwtAuthRes jwtAuthRes = responseEntity.getBody();
		String accessToken = jwtAuthRes.getAccessToken();
		String refreshToken = jwtAuthRes.getRefreshToken();	
		Claims accessClaims = Jwts.parser().setSigningKey(secret).parseClaimsJws(accessToken).getBody();
		Claims refreshClaims = Jwts.parser().setSigningKey(secret).parseClaimsJws(refreshToken).getBody();
		
		assertEquals(memberIdAAA, accessClaims.get("memberId"));
		assertEquals(memberIdAAA, refreshClaims.get("memberId"));
		//jwt_token 테이블에 데이터가 입력 됐는지 확인한다.
		assertEquals(true , jwtRepo.findById(jwtTokenProvider.getTokenId(accessToken)).isPresent());
	}
	
	@Test
	void logoutTest() {
		AuthDTO.LoginReq loginReq = AuthDTO.LoginReq.builder().memberId(memberIdAAA).password("aaa111").build();
		ResponseEntity<AuthDTO.JwtAuthRes> responseEntity = authService.login(loginReq);
		String accessToken = responseEntity.getBody().getAccessToken();
		
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer " + accessToken);
		MockHttpServletResponse response = new MockHttpServletResponse();
		
		String memberId = authService.logout(request, response);
		assertEquals(memberIdAAA, memberId);
		//jwt_token 테이블에서 데이터가 삭제 됐는지 확인한다.
		assertEquals(false, jwtRepo.findById(jwtTokenProvider.getTokenId(accessToken)).isPresent());
	}
	
	@Test
	void refreshTest(){
		AuthDTO.LoginReq loginReq = AuthDTO.LoginReq.builder().memberId(memberIdAAA).password("aaa111").build();
		ResponseEntity<AuthDTO.JwtAuthRes> responseEntity = authService.login(loginReq);
		String refreshToken = responseEntity.getBody().getRefreshToken();
		Cookie[] cookies = new Cookie[] {
				new Cookie(refreshCookieName, refreshToken)
		};
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setCookies(cookies);
		
		responseEntity = authService.refresh(request);
		AuthDTO.JwtAuthRes jwtAuthRes = responseEntity.getBody();
		String newAccessToken = jwtAuthRes.getAccessToken();
		String newRefreshToken = jwtAuthRes.getRefreshToken();	
		Claims accessClaims = Jwts.parser().setSigningKey(secret).parseClaimsJws(newAccessToken).getBody();
		Claims refreshClaims = Jwts.parser().setSigningKey(secret).parseClaimsJws(newRefreshToken).getBody();
		
		assertEquals("aaa", accessClaims.get("memberId"));
		assertEquals("aaa", refreshClaims.get("memberId"));
	}
	
	@Test
	void isExpirationDateAdjacentTest() {
		JwtTokenRepository mockJwtTokenRepository = mock(JwtTokenRepository.class);
		MemberRepository mockMemberRepository = mock(MemberRepository.class);
		BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
		CustomUserDetailsService customUserDetailsService = new CustomUserDetailsService(mockMemberRepository);
		JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(customUserDetailsService);
		ReflectionTestUtils.setField(jwtTokenProvider, "secret", secret);
		AuthServiceImpl authSvc = new AuthServiceImpl(mockJwtTokenRepository, customUserDetailsService, passwordEncoder, jwtTokenProvider);
		
		AuthDTO.LoginReq loginReq = AuthDTO.LoginReq.builder().memberId(memberIdAAA).password("aaa111").build();
		ResponseEntity<AuthDTO.JwtAuthRes> responseEntity = authService.login(loginReq);
		String accessToken = responseEntity.getBody().getAccessToken();
		String refreshToken = responseEntity.getBody().getRefreshToken();
		
		//accessToken < 14일(기준일) < refreshToken
		assertEquals(true, ReflectionTestUtils.invokeMethod(authSvc, "isExpirationDateAdjacent", accessToken));
		assertEquals(false, ReflectionTestUtils.invokeMethod(authSvc, "isExpirationDateAdjacent", refreshToken));
	}
}
