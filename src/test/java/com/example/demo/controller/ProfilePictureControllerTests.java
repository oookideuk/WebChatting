package com.example.demo.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.FileInputStream;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import com.example.demo.dto.AuthDTO;
import com.example.demo.dto.AuthDTO.JwtAuthRes;
import com.example.demo.dto.ProfilePictureDTO;
import com.example.demo.exception.ErrorCode;
import com.example.demo.exception.ErrorResponse;
import com.example.demo.service.AuthService;
import com.example.demo.service.ProfilePictureService;
import com.fasterxml.jackson.databind.ObjectMapper;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ProfilePictureControllerTests {
	private MockMvc mvc;
	@Autowired private ObjectMapper objectMapper;
	@Autowired ProfilePictureService profilePictureService;
	@Autowired AuthService authService;
	private String memberIdAAA = "aaa";
	private String accessTokenAAA;
	
	@BeforeEach
	void setup(WebApplicationContext webApplicationContex) {
		mvc = MockMvcBuilders.webAppContextSetup(webApplicationContex)
				//한글깨짐 방지
				.addFilters(new CharacterEncodingFilter("UTF-8", true))
				.apply(SecurityMockMvcConfigurers.springSecurity())
				.build();
		
		//로그인
		AuthDTO.LoginReq loginReq = AuthDTO.LoginReq.builder().memberId(memberIdAAA).password("aaa111").build();
		JwtAuthRes authRes = authService.login(loginReq).getBody();
		accessTokenAAA = authRes.getAccessToken();
	}
	
	/**
	 * 프로필 사진 등록 테스트
	 */
	@Order(1)
	@Test
	void saveProfilePictureTest1() throws Exception {
		FileInputStream image = FileUtils.openInputStream(Paths.get("src/main/resources/static/img/basicProfilePicture.png").toAbsolutePath().toFile());
		//fileName, originalFileName, MIME.type, content
		MockMultipartFile profilePicture = new MockMultipartFile("file", "test2.png", MediaType.IMAGE_PNG_VALUE, image);
		
		mvc.perform(multipart("/v1/profilePicture")
				.file(profilePicture)
				.header("Authorization", "Bearer " + accessTokenAAA))
			.andDo(print());
	}
	
	/**
	 * 프로필 사진 등록 테스트
	 * 실패 - 파일 없음
	 */
	@Order(2)
	@Test
	void saveProfilePictureTest3() throws Exception {
		ErrorResponse errorResponse = new ErrorResponse(ErrorCode.INVALID_INPUT_VALUE);
		String errorResponseJson = objectMapper.writeValueAsString(errorResponse);
		
		mvc.perform(multipart("/v1/profilePicture")
				.header("Authorization", "Bearer " + accessTokenAAA))
			.andExpect(content().json(errorResponseJson))
			.andDo(print());
	}
	
	/**
	 * 프로필 사진 등록 테스트
	 * 실패 - 이미지 파일 아님.
	 */
	@Order(2)
	@Test
	void saveProfilePictureTest11() throws Exception {
		ErrorResponse errorResponse = new ErrorResponse(ErrorCode.NOT_IMAGE_FILE);
		String errorResponseJson = objectMapper.writeValueAsString(errorResponse);
		
		FileInputStream image = FileUtils.openInputStream(Paths.get("src/main/resources/static/img/txt.txt").toAbsolutePath().toFile());
		//fileName, originalFileName, MIME.type, content
		MockMultipartFile profilePicture = new MockMultipartFile("file", "txt.txt", MediaType.TEXT_PLAIN_VALUE, image);
		
		mvc.perform(multipart("/v1/profilePicture")
				.file(profilePicture)
				.header("Authorization", "Bearer " + accessTokenAAA))
			.andExpect(content().json(errorResponseJson))
			.andDo(print());
	}
	
	/**
	 * 프로필 사진 등록 테스트.
	 * 실패 - 이름만 이미지 파일.
	 */
	@Order(2)
	@Test
	void saveProfilePictureTest111() throws Exception {
		ErrorResponse errorResponse = new ErrorResponse(ErrorCode.FILE_UPLOAD_EXCEPTION);
		String errorResponseJson = objectMapper.writeValueAsString(errorResponse);
		
		FileInputStream image = FileUtils.openInputStream(Paths.get("src/main/resources/static/img/txt.txt").toAbsolutePath().toFile());
		//fileName, originalFileName, MIME.type, content
		MockMultipartFile profilePicture = new MockMultipartFile("file", "txt.png", MediaType.IMAGE_PNG_VALUE, image);
		
		mvc.perform(multipart("/v1/profilePicture")
				.file(profilePicture)
				.header("Authorization", "Bearer " + accessTokenAAA))
			.andExpect(content().json(errorResponseJson))
			.andDo(print());
	}
	
	/**
	 * 프로필 사진 조회 테스트
	 * 성공
	 */
	@Order(2)
	@Test
	void findProfilePictureTest1() throws Exception {
		mvc.perform(get("/v1/profilePicture/aaa")
				.header("Authorization", "Bearer " + accessTokenAAA))
			.andExpect(status().isOk())
			.andDo(print());
	}
	
	/**
	 * 프로필 사진 조회 테스트
	 * 실패 - 가입된 회원, 등록된 프로필 사진 없음
	 */
	@Order(2)
	@Test
	void findProfilePictureTest3() throws Exception {
		ErrorResponse errorResponse = new ErrorResponse(ErrorCode.PROFILE_PICTURE_NOT_FOUND);
		String errorResponseJson = objectMapper.writeValueAsString(errorResponse);
		
		mvc.perform(get("/v1/profilePicture/aaa")
				.header("Authorization", "Bearer " + accessTokenAAA))
			.andExpect(status().isBadRequest())
			.andExpect(content().json(errorResponseJson))
			.andDo(print());
	}
	
	/**
	 * 프로필 사진 조회 테스트
	 * 실패 - 가입된 회원 아님
	 */
	@Order(2)
	@Test
	void findProfilePictureTest2() throws Exception {
		ErrorResponse errorResponse = new ErrorResponse(ErrorCode.ACCOUNT_NOT_FOUND);
		String errorResponseJson = objectMapper.writeValueAsString(errorResponse);
		
		mvc.perform(get("/v1/profilePicture/asndlasndkl")
				.header("Authorization", "Bearer " + accessTokenAAA))
			.andExpect(status().isBadRequest())
			.andExpect(content().json(errorResponseJson))
			.andDo(print());
	}
	
	/**
	 * 프로필 사진 이미지 조회 테스트
	 * 성공
	 */
	@Order(2)
	@Test
	void findPriflePictureImageTest1() throws Exception {
		ProfilePictureDTO.Response res = profilePictureService.findProfilePictureByMemberId("aaa");
		long fileId = res.getFileId();
		
		 RequestBuilder reqBuilder = MockMvcRequestBuilders.get("/v1/file/profilePicture/" + fileId)
				 						.header("Authorization", "Bearer " + accessTokenAAA);
		 mvc.perform(reqBuilder)
		 	.andExpect(content().contentType(MediaType.IMAGE_PNG))
		 	.andDo(print());
	}
	
	/**
	 * 프로필 사진 삭제 테스트
	 * 성공
	 */
	@Order(99)
	@Test
	void deleteProfilePictureTest1() throws Exception {
		mvc.perform(delete("/v1/profilePicture")
				.header("Authorization", "Bearer " + accessTokenAAA))
			.andExpect(status().isOk())
			.andDo(print());
	}
	
	/**
	 * 프로필 사진 삭제 테스트
	 * 실패 - 등록된 프로필 사진 없음.
	 */
	@Order(99)
	@Test
	void deleteProfilePictureTest3() throws Exception {
		ErrorResponse errorResponse = new ErrorResponse(ErrorCode.PROFILE_PICTURE_NOT_FOUND);
		String errorResponseJson = objectMapper.writeValueAsString(errorResponse);
		
		mvc.perform(delete("/v1/profilePicture")
				.header("Authorization", "Bearer " + accessTokenAAA))
				.andExpect(status().isBadRequest())
				.andExpect(content().json(errorResponseJson))
				.andDo(print());
	}
}
