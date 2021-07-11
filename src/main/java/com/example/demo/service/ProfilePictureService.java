package com.example.demo.service;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.http.HttpHeaders;

import com.example.demo.dto.ProfilePictureDTO;

public interface ProfilePictureService {
	/**
	 * 프로필 사진 정보를 DB에 저장한다.
	 */
	ProfilePictureDTO.Response saveProfilePicture(ProfilePictureDTO.UploadReq profilePictureReq) throws IOException;
	
	/**
	 * 회원의 프로필 사진을 가져온다.
	 */
	ProfilePictureDTO.Response findProfilePictureByMemberId(String memberId) throws IOException;
	
	/**
	 * 프로필 사진을 가져온다.
	 * 웹 브라우저에서 이미지를 나타내는게 가능하며 '새 탭에서 이미지 열기' 가능하다.
	 */
	byte[] findProfilePictureImage(HttpHeaders headers, long fileId) throws IOException;
	
	/**
	 * 회원의 프로필 사진 정보 및 파일을 삭제한다.
	 */
	void deleteProfilePictureByMemberId(String memberId) throws IOException;
	
	/**
	 * 프로필 사진 파일을 삭제한다.
	 */
	void deleteProfilePictureFile(Path profilePicturePath) throws IOException;

	/**
	 * 회원의 등록된 프로필 사진이 있는지 확인한다.
	 */
	boolean existsByMemberId(String memberId);	
}
