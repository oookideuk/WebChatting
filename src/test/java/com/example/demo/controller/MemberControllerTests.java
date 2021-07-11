package com.example.demo.controller;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import com.example.demo.domain.Role;
import com.example.demo.domain.entitiy.MemberEntity;
import com.example.demo.domain.repository.MemberRepository;
import com.example.demo.dto.AuthDTO;
import com.example.demo.dto.AuthDTO.LoginReq;
import com.example.demo.exception.ErrorCode;
import com.example.demo.exception.ErrorResponse;
import com.example.demo.service.AuthService;
import com.example.demo.service.MemberService;
import com.fasterxml.jackson.databind.ObjectMapper;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
public class MemberControllerTests {
	private MockMvc mvc;
	@Autowired 	private ObjectMapper objectMapper;
	@Value("${spring.mail.username}") private String email;
	@Autowired AuthService authService;
	@Autowired MemberService memberService;
	@Autowired MemberRepository memberRepo;
	@Autowired PasswordEncoder passwordEncoder;
	private String memberIdAAA = "aaa";
	
	String accessToken;
	String refreshToken;
	
	@BeforeEach
	void setup(WebApplicationContext webApplicationContex) {
		mvc = MockMvcBuilders.webAppContextSetup(webApplicationContex)
				//한글깨짐 방지
				.addFilters(new CharacterEncodingFilter("UTF-8", true))
				.apply(SecurityMockMvcConfigurers.springSecurity())
				.build();
		this.setToken();
	}
	
	private void setToken() {
		Optional<MemberEntity> member = memberRepo.findById(memberIdAAA);
		if(member.isPresent()) {
			memberService.enableEmailAuthFlag(memberIdAAA, member.get().getEmailAuthKey());
			LoginReq loginReq = AuthDTO.LoginReq.builder().memberId(memberIdAAA).password("aaa111").build();
			AuthDTO.JwtAuthRes jwtAuthRes = authService.login(loginReq).getBody();
			accessToken = jwtAuthRes.getAccessToken();
			refreshToken = jwtAuthRes.getRefreshToken();
		}
	}
	
	/**
	 * 회원가입 테스트
	 */
	@Order(1)
	@Test
	void signupTest1() throws Exception {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("memberId", "ccc");
		params.add("password", "aaa111");
		params.add("name", "가나다");
		params.add("email", email);
		
		mvc.perform(post("/v1/members")
				.params(params))
				.andDo(print());
	}
	
	/**
	 * 이미 가입한 회원
	 */
	@Order(2)
	@Test
	void signupTest2() throws Exception {
		ErrorResponse errorResponse = new ErrorResponse(ErrorCode.ACCOUNT_DUPLICATION);
		String errorResponseJson = objectMapper.writeValueAsString(errorResponse);
		
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("memberId", memberIdAAA);
		params.add("password", "aaa111");
		params.add("name", "aaa");
		params.add("email", email);
		
		mvc.perform(post("/v1/members")
				.params(params))
			.andExpect(content().json(errorResponseJson))
			.andDo(print());
	}
	
	/**
	 * 회원정보를 가져온다.
	 */
	@Order(2)
	@Test
	void findMember1() throws Exception {
		mvc.perform(get("/v1/members/aaa")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.memberId", is(memberIdAAA)))
			.andDo(print());
	}
	
	/**
	 * 회원정보를 가져온다.
	 * 실패 - 가입된 회원 아님
	 */
	@Order(2)
	@Test
	void findMember2() throws Exception {
		mvc.perform(get("/v1/members/sadlksnaskdlmn")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isBadRequest())
			.andDo(print());
	}
	
	/**
	 * 회원정보를 수정한다.
	 */
	@Order(2)
	@Test
	void updatePersonalInfomationTest1() throws Exception {
		mvc.perform(put("/v1/members/me")
				.header("Authorization", "Bearer " + accessToken)
				.param("name", "가나다a12s라"))
			.andExpect(jsonPath("$.name").value("가나다a12s라"))
			.andDo(print());
	}
	
	/**
	 * 회원정보를 수정한다.
	 * 실패 - validation
	 */
	@Order(2)
	@Test
	void updatePersonalInfomationTest2() throws Exception {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("name", "a");
		
		mvc.perform(put("/v1/members/me")
				.header("Authorization", "Bearer " + accessToken)
				.params(params))
			.andExpect(jsonPath("$.code", is("INVALID_INPUT_VALUE")))
			.andExpect(jsonPath("$.errors[0].field", is("name")))
			.andDo(print());
	}
	
	/**
	 * 패스워드 수정 테스트
	 */
	@Order(2)
	@Test
	void updatePasswordTest1() throws Exception {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("password", "aaa111");
		params.add("passwordChange", "bbb111");
		
		mvc.perform(put("/v1/members/me/password")
				.header("Authorization", "Bearer " + accessToken)
				.params(params))
			.andExpect(content().string("패스워드 수정 성공"))
			.andExpect(status().isOk())
			.andDo(print());
		
		//패스워드 원복
		params.set("password", "bbb111");
		params.set("passwordChange", "aaa111");
		mvc.perform(put("/v1/members/me/password")
				.header("Authorization", "Bearer " + accessToken)
				.params(params))
			.andExpect(content().string("패스워드 수정 성공"))
			.andExpect(status().isOk());
	}
	
	/**
	 * 패스워드 수정 테스트
	 * 실패 - 패스워드 불일치
	 */
	@Order(2)
	@Test
	void updatePasswordTest2() throws Exception {
		ErrorResponse errorResponse = new ErrorResponse(ErrorCode.PASSWORD_MITMATCH);
		String errorResponseJson = objectMapper.writeValueAsString(errorResponse);
		
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("password", "sajdknbask2");
		params.add("passwordChange", "bbb111");
		
		mvc.perform(put("/v1/members/me/password")
				.header("Authorization", "Bearer " + accessToken)
				.params(params))
			.andExpect(content().json(errorResponseJson))
			.andExpect(status().isBadRequest())
			.andDo(print());
	}
	/**
	 * 패스워드 수정 테스트
	 * 실패 - validation
	 */
	@Order(2)
	@Test
	void updatePasswordTest3() throws Exception {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("password", "1");
		params.add("passwordChange", "bbb111");
		
		mvc.perform(put("/v1/members/me/password")
				.header("Authorization", "Bearer " + accessToken)
				.params(params))
			.andExpect(jsonPath("$.errors[0].field", is("password")))
			.andExpect(status().isBadRequest())
			.andDo(print());
	}
	
	/**
	 * 이메일 인증 플래그 활성화 테스트
	 */
	@Order(2)
	@Test
	void enableEmailAuthFlagTest() throws Exception{
		String memberId = "emailAuthTest";
		MemberEntity member = MemberEntity.builder()
				.memberId(memberId)
				.password(passwordEncoder.encode("aaa111"))
				.name("test")
				.role(Role.ROLE_MEMBER)
				.emailAuthKey("emailAuthTest")
				.email("email.com").build();
		member = memberRepo.save(member);
		
		mvc.perform(post("/v1/members/me/email/auth")
				.param("memberId", memberId)
				.param("emailAuthKey", member.getEmailAuthKey()))
			.andDo(print());
		
		assertEquals(1, memberRepo.findById(memberId).get().getEmailAuthFlag());
		
		//원복
		memberRepo.deleteById(memberId);
		assertEquals(false, memberRepo.findById(memberId).isPresent());
	}
	
	/**
	 * 회원탈퇴 한다.
	 */
	@Order(99)
	@Test
	void deleteMember1() throws Exception {
		mvc.perform(delete("/v1/members/me")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andDo(print());
	}
}
