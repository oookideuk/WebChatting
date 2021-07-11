package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.demo.domain.repository.ProfilePictureRepository;
import com.example.demo.dto.ProfilePictureDTO;
import com.example.demo.exception.ProfilePictureNotFoundException;
import com.example.demo.service.impl.ProfilePictureServiceImpl;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
public class ProfilePictureServiceTests {
	@Autowired private ProfilePictureService profilePictureService;
	@Autowired private ProfilePictureRepository profilePictureRepository;
	
	@Order(1)
	@Test
	void saveProfilePictureTest() throws Exception {
		FileInputStream image = FileUtils.openInputStream(Paths.get("src/main/resources/static/img/basicProfilePicture.png").toAbsolutePath().toFile());
		MockMultipartFile multipartFile = new MockMultipartFile("profilcPicture", "basicImage.png", MediaType.IMAGE_PNG_VALUE, image);
		ProfilePictureDTO.UploadReq profilePictureReq = ProfilePictureDTO.UploadReq.builder()
				.multipartFile(multipartFile)
				.memberId("aaa")
				.build();
		profilePictureReq.setFileInformation();
		 profilePictureService.saveProfilePicture(profilePictureReq);
		assertEquals(true, profilePictureReq.getUploadPath().toFile().exists());
	}
	
	@Order(2)
	@Test
	void saveProfilePuctreFileTest() throws IOException {
		FileInputStream image = FileUtils.openInputStream(Paths.get("src/main/resources/static/img/basicProfilePicture.png").toAbsolutePath().toFile());
		MockMultipartFile multipartFile = new MockMultipartFile("profilcPicture", "basicImage.png", MediaType.IMAGE_PNG_VALUE, image);
		ProfilePictureDTO.UploadReq uploadReq = ProfilePictureDTO.UploadReq.builder()
				.multipartFile(multipartFile)
				.build();
		uploadReq.setFileInformation();
		ReflectionTestUtils.invokeMethod(new ProfilePictureServiceImpl(profilePictureRepository), "saveProfilePictureFile", uploadReq);
		assertEquals(true, uploadReq.getUploadPath().toFile().exists());
	}
	
	@Order(2)
	@Test
	void findProfilePictureByMemberIdTest() throws IOException {
		//프로필 사진 있음
		assertEquals(true, profilePictureService.findProfilePictureByMemberId("aaa").getMemberId().equals("aaa"));
		//프로필 사진 없음 
		assertThrows(ProfilePictureNotFoundException.class, () -> profilePictureService.findProfilePictureByMemberId("asmlkdasm"), "profilePictureEntity with memberId");
	}
	
	@Order(2)
	@Test
	void existsByMemberIdTest() {
		//프로필 사진 있음
		assertEquals(true, profilePictureService.existsByMemberId("aaa"));
		//프로필 사진 없음
		assertEquals(false, profilePictureService.existsByMemberId("asndlkadslkn"));
	}
	
	@Order(99)
	@Test
	void deleteProfilePictureByMemberIdTest() throws IOException {
		//프로필 사진 있음
		profilePictureService.deleteProfilePictureByMemberId("aaa");
		//프로필 사진 없음
		assertThrows(ProfilePictureNotFoundException.class, () -> profilePictureService.deleteProfilePictureByMemberId("asmlkdasm"), "profilePictureEntity with memberId");
	}
	
}
