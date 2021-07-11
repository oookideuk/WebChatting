package com.example.demo.controller;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import com.example.demo.domain.Role;
import com.example.demo.domain.entitiy.MemberEntity;
import com.example.demo.domain.repository.MemberRepository;
import com.example.demo.dto.AuthDTO;
import com.example.demo.exception.ErrorCode;
import com.example.demo.exception.ErrorResponse;
import com.example.demo.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@ActiveProfiles("local")
@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTests {
	private MockMvc mvc;
	@Autowired private ObjectMapper objectMapper;
	@Autowired private MemberRepository memberRepo;
	@Autowired private PasswordEncoder passwordEncoder;
	@Autowired private AuthService authService;
	@Value("${app.jwt.secret}") private String secret;
	@Value("${app.jwt.access.type}") private String accessType;
	@Value("${app.jwt.refresh.type}") private String refreshType;
	@Value("${app.jwt.access.cookie.name}") private String accessCookieName;
	@Value("${app.jwt.refresh.cookie.name}") private String refreshCookieName;
	private String memberIdAAA = "aaa";
	private String memberIdBBB = "bbb";
	@BeforeEach
	void setup(WebApplicationContext webApplicationContex) {
		mvc = MockMvcBuilders.webAppContextSetup(webApplicationContex)
				//한글깨짐 방지
				.addFilters(new CharacterEncodingFilter("UTF-8", true))
				.apply(SecurityMockMvcConfigurers.springSecurity())
				.build();
		
		this.saveMember(memberIdAAA);
	}
	
	private void saveMember(String memberId) {
		if(!memberRepo.findById(memberId).isPresent()) {
			MemberEntity member = MemberEntity.builder()
				.memberId(memberId)
				.password(passwordEncoder.encode("aaa111"))
				.name("테스트aaa")
				.role(Role.ROLE_MEMBER)
				.email("test.com")
				.emailAuthKey("test")
				.emailAuthFlag(1).build();
			memberRepo.save(member);
		}
	}
	
	/**
	 * 로그인 테스트
	 * 성공
	 */
	@Test
	void loginTest1() throws Exception {
		MvcResult mvcResult = mvc.perform(post("/v1/auth/login")
				.param("memberId", memberIdAAA)
				.param("password", "aaa111"))
			.andExpect(status().isOk())
			.andExpect(cookie().exists(accessCookieName))
			.andExpect(cookie().exists(refreshCookieName))
			.andDo(print())
			.andReturn();
		mvcResult.getResponse().getCookies();
		String contentStr = mvcResult.getResponse().getContentAsString();
		@SuppressWarnings("unchecked")
		Map<String, String> contentMap = objectMapper.readValue(contentStr, Map.class);
		
		//access token test
		String accessToken = contentMap.get("accessToken");
		Claims accessCalims = Jwts.parser().setSigningKey(secret).parseClaimsJws(accessToken).getBody();
		assertEquals(memberIdAAA, accessCalims.get("memberId"));
		assertEquals(accessType, accessCalims.get("type"));
		
		//refresh token test
		String refreshToken = contentMap.get("refreshToken");
		Claims refreshClaims = Jwts.parser().setSigningKey(secret).parseClaimsJws(refreshToken).getBody();
		assertEquals(memberIdAAA, refreshClaims.get("memberId"));
		assertEquals(refreshType, refreshClaims.get("type"));
	}
	
	/**
	 * 로그인 테스트
	 * 실패 - 계정 없음
	 */
	@Test
	void loginTest2() throws Exception {
		ErrorResponse errorResponse = new ErrorResponse(ErrorCode.INVALID_LOGIN_INPUT);
		String errorResponseJson = objectMapper.writeValueAsString(errorResponse);
		
		mvc.perform(formLogin("/v1/auth/login")
				.user("memberId", "aslkfnasl")
				.password("aaa111")
				.acceptMediaType(MediaType.APPLICATION_JSON))
			.andExpect(content().json(errorResponseJson))
			.andDo(print());
	}
	
	/**
	 * 로그인 테스트
	 * 실패 - 패스워드 불일치
	 */
	@Test
	void loginTest3() throws Exception {
		ErrorResponse errorResponse = new ErrorResponse(ErrorCode.INVALID_LOGIN_INPUT);
		String errorResponseJson = objectMapper.writeValueAsString(errorResponse);
		
		mvc.perform(formLogin("/v1/auth/login")
				.user("memberId", memberIdAAA)
				.password("aslkdsl123s")
				.acceptMediaType(MediaType.APPLICATION_JSON))
			.andExpect(content().json(errorResponseJson))
			.andDo(print());
	}
	
	/**
	 * 로그인 테스트
	 * 실패 - 이메일 미인증
	 */
	@Test
	void loginTest4() throws Exception {
		ErrorResponse errorResponse = new ErrorResponse(ErrorCode.ACCOUNT_DISABLED);
		String errorResponseJson = objectMapper.writeValueAsString(errorResponse);
		
		MemberEntity member = MemberEntity.builder()
				.memberId(memberIdBBB)
				.password(passwordEncoder.encode("aaa111"))
				.name("테스트aaa")
				.role(Role.ROLE_MEMBER)
				.email("test.com")
				.emailAuthKey("test")
				.emailAuthFlag(0).build();
		memberRepo.save(member);
		
		mvc.perform(formLogin("/v1/auth/login")
				.user("memberId", memberIdBBB)
				.password("aaa111")
				.acceptMediaType(MediaType.APPLICATION_JSON))
			.andExpect(content().json(errorResponseJson))
			.andDo(print());
	}
	
	/**
	 * 로그인 테스트
	 * 실패 - 입력 값 검증
	 */
	@Test
	void loginTest5() throws Exception {
		mvc.perform(formLogin("/v1/auth/login")
				.user("memberId", "aa")
				.password("aaa111")
				.acceptMediaType(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.code", is("INVALID_INPUT_VALUE")))
			.andExpect(jsonPath("$.errors[0].field", is("memberId")))
			.andExpect(jsonPath("$.errors[0].value", is("aa")))
			.andDo(print());
	}
	
	/**
	 * 로그아웃 테스트
	 * 성공
	 */
	@Test
	void logoutTest1() throws Exception {
		AuthDTO.LoginReq loginReq = AuthDTO.LoginReq.builder().memberId(memberIdAAA).password("aaa111").build();
		ResponseEntity<AuthDTO.JwtAuthRes> responseEntity = authService.login(loginReq);
		String accessToken = responseEntity.getBody().getAccessToken();
		String refreshToken = responseEntity.getBody().getRefreshToken();
		
		mvc.perform(post("/v1/auth/logout")
				.cookie(new Cookie(accessCookieName, accessToken))
				.cookie(new Cookie(refreshCookieName, refreshToken))
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(cookie().maxAge(accessCookieName, 0))
			.andExpect(cookie().value(accessCookieName, ""))
			.andExpect(cookie().maxAge(refreshCookieName, 0))
			.andExpect(cookie().value(refreshCookieName, ""))
		 	.andDo(print());
	}
	
	/**
	 * 로그아웃 테스트
	 * 실패 - 액세스 토큰 없음 - UNAUTHORIZED 반환
	 */
	@Test
	void logoutTest2() throws Exception{
		ErrorResponse errorResponse = new ErrorResponse(ErrorCode.UNAUTHORIZED);
		String errorResponseJson = objectMapper.writeValueAsString(errorResponse);
		
		mvc.perform(post("/v1/auth/logout"))
			.andExpect(content().json(errorResponseJson))
		 	.andDo(print());
	}
	
	/**
	 * 로그아웃 테스트
	 * 실패 - DB에 토큰 정보 없음 - TokenNotFoundException 발생
	 */
	@Test
	void logoutTest3() throws Exception{
		//2385년 만료 토큰, memberId = aaa
		String accessToken = "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJlZjk4MzM3ZC04ZGI0LTRkNGMtYTYyNy0zMTk2Njc3NjRkOGYiLCJpYXQiOjE2MDgxMjY5ODksImV4cCI6MTMxMjYzNzAxODksIm1lbWJlcklkIjoiYWFhIiwidHlwZSI6ImFjY2VzcyJ9.OqgI2GHsouR1j0NF9jb7KDVY6PapOBmdezV1vWAllJo";
		ErrorResponse errorResponse = new ErrorResponse(ErrorCode.TOKEN_NOT_FOUND);
		String errorResponseJson = objectMapper.writeValueAsString(errorResponse);
		
		mvc.perform(post("/v1/auth/logout")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(content().json(errorResponseJson))
		 	.andDo(print());
	}
	
	/**
	 * 리프레시 테스트
	 * 성공
	 */
	@Test
	void refreshTest1() throws Exception {
		AuthDTO.LoginReq loginReq = AuthDTO.LoginReq.builder().memberId(memberIdAAA).password("aaa111").build();
		ResponseEntity<AuthDTO.JwtAuthRes> responseEntity = authService.login(loginReq);
		String accessToken = responseEntity.getBody().getAccessToken();
		String refreshToken = responseEntity.getBody().getRefreshToken();
		
		mvc.perform(post("/v1/auth/refresh")
				.cookie(new Cookie(accessCookieName, accessToken))
				.cookie(new Cookie(refreshCookieName, refreshToken))
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(cookie().exists(accessCookieName))
			//만료일 < 기준일 일 때 통과한다.
			//.andExpect(cookie().exists(refreshCookieName))
			.andDo(print());
	}
	
	/**
	 * 리프레시 테스트
	 * 실패 - 쿠키에 리프레시 토큰 없음
	 */
	@Test
	void refreshTest2() throws Exception {
		ErrorResponse errorResponse = new ErrorResponse(ErrorCode.REFRESH_FAILURE);
		String errorResponseJson = objectMapper.writeValueAsString(errorResponse);
		
		mvc.perform(post("/v1/auth/refresh"))
			.andExpect(content().json(errorResponseJson))
			.andDo(print());
	}
	
	/**
	 * 리프레시 테스트
	 * 실패 - 만료된 리프레시 토큰
	 */
	@Test
	void refreshTest3() throws Exception {
		String expiredRefreshToken  = "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiIyZDc4NDBjYi1jMjBiLTRmNGUtOWIyMi1jYzYwZDgwNjUxYzAiLCJpYXQiOjE2MDgxMjc2ODMsImV4cCI6MTYwODEyNzY5MywibWVtYmVySWQiOiJhYWEiLCJ0eXBlIjoicmVmcmVzaCJ9.BCoSBDbbu3nkkUGSfM27SayipH_JAq0K3Dt8MF1QCAs";
		ErrorResponse errorResponse = new ErrorResponse(ErrorCode.REFRESH_FAILURE);
		String errorResponseJson = objectMapper.writeValueAsString(errorResponse);
		
		mvc.perform(post("/v1/auth/refresh")
				.cookie(new Cookie(refreshCookieName, expiredRefreshToken)))
			.andExpect(content().json(errorResponseJson))
			.andDo(print());
	}
	
	/**
	 * 리프레시 테스트
	 * 실패 - DB에 토큰 정보 없음
	 */
	@Test
	void refreshTest4() throws Exception {
		//2385년 만료 토큰, memberId = aaa
		String refreshToken = "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJjMWU5YjVmMC00ODgzLTQ3M2YtYTljYy02NWIyZTY3ZjI1NjYiLCJpYXQiOjE2MDgxMjc4NDMsImV4cCI6MTMxMjYzNzEwNDMsIm1lbWJlcklkIjoiYWFhIiwidHlwZSI6InJlZnJlc2gifQ.QZIdR1JpkA8DyZPfUUS7FM2mkA3bPrdlDrbFPNXr_iU";
		ErrorResponse errorResponse = new ErrorResponse(ErrorCode.REFRESH_FAILURE);
		String errorResponseJson = objectMapper.writeValueAsString(errorResponse);
		
		mvc.perform(post("/v1/auth/refresh")
				.cookie(new Cookie(refreshCookieName, refreshToken)))
			.andExpect(content().json(errorResponseJson))
			.andDo(print());
	}
	
	@Test
	void meTest1() throws Exception{
		AuthDTO.LoginReq loginReq = AuthDTO.LoginReq.builder().memberId(memberIdAAA).password("aaa111").build();
		ResponseEntity<AuthDTO.JwtAuthRes> responseEntity = authService.login(loginReq);
		String accessToken = responseEntity.getBody().getAccessToken();
		
		mvc.perform(get("/v1/auth/me")
				.cookie(new Cookie(accessCookieName, accessToken))
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(content().string("aaa"))
		 	.andDo(print());
	}
	
}
