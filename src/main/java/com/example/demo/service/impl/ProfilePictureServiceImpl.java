package com.example.demo.service.impl;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityNotFoundException;

import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.common.FileUtil;
import com.example.demo.domain.entitiy.MemberEntity;
import com.example.demo.domain.entitiy.ProfilePictureEntity;
import com.example.demo.domain.repository.ProfilePictureRepository;
import com.example.demo.dto.ProfilePictureDTO;
import com.example.demo.exception.FileUploadException;
import com.example.demo.exception.NotImageFileException;
import com.example.demo.exception.ProfilePictureCountExceedException;
import com.example.demo.exception.ProfilePictureNotFoundException;
import com.example.demo.service.ProfilePictureService;

import ch.qos.logback.classic.Logger;

@Service
public class ProfilePictureServiceImpl implements ProfilePictureService {
	private final Logger logger = (Logger) LoggerFactory.getLogger(this.getClass());
	private ProfilePictureRepository profilePictureRepository;
	
	public ProfilePictureServiceImpl(ProfilePictureRepository profilePictureRepository) {
		this.profilePictureRepository = profilePictureRepository;
	}
	
	@Override
	@Transactional(rollbackFor = Exception.class)
	public ProfilePictureDTO.Response saveProfilePicture(ProfilePictureDTO.UploadReq uploadReq) throws IOException {
		//파일없음
		if(uploadReq.getFile() == null || uploadReq.getFile().getSize() <= 0) {
			throw new EntityNotFoundException("파일 없음");
		}
		//파일의 기본 정보를 세팅한다.
		uploadReq.setFileInformation();
		
		//프로필 사진이 등록되어 있으면 예외 발생시킨다.
		if(this.existsByMemberId(uploadReq.getMemberId())) {
			throw new ProfilePictureCountExceedException();
		}
		//이미지 파일이 아닐 경우 예외 발생시킨다.
		if(!FileUtil.isImage(uploadReq.getType())) {
			throw new NotImageFileException();
		}
		
		try {
			ProfilePictureEntity profilePictureEntity = uploadReq.toEntity();
			ProfilePictureEntity savedEntity = profilePictureRepository.save(profilePictureEntity); //파일 정보를 DB에 저장한다.
			this.saveProfilePictureFile(uploadReq); //실제 파일을 저장한다.
			return ProfilePictureDTO.Response.of(savedEntity);
		}catch(Exception e) {
			logger.debug("프로필 사진 저장 실패 message[{}]", e.getMessage());
			/*프로필 사진 저장에 실패 했기 때문에 서버에 저장된 파일을 삭제한다.
			프로필 사진 저장 후 썸네일 만들때 실패하면 필요할 듯.*/
			Path profilePicturePath = Paths.get(uploadReq.getUploadPath().toString(), uploadReq.getStoredFileName());
			try {
				this.deleteProfilePictureFile(profilePicturePath);
			}catch(IOException ioE) {
				logger.debug("파일 삭제 실패 message[{}] path[{}]", e.getMessage(), profilePicturePath);
			}
			throw new FileUploadException(e);
		}
	}
	
	/**
	 * 프로필 사진 파일을 저장한다.
	 */
	private void saveProfilePictureFile(ProfilePictureDTO.UploadReq uploadReq) throws IOException {
		Path uploadPath = uploadReq.getUploadPath();
		String storedFileName = uploadReq.getStoredFileName();
		FileUtil.makeDir(uploadPath); //디렉토리를 생성한다.
		FileUtil.saveFile(uploadReq); //파일을 저장한다.
		
		Path savedFilePath = Paths.get(uploadPath.toString(), storedFileName);
		if(FileUtil.isImage(savedFilePath)) { //이미지 파일일 경우 썸네일을 생성한다.
			FileUtil.makeThumbnail(savedFilePath);
		}
	}
	
	@Override
	public ProfilePictureDTO.Response findProfilePictureByMemberId(String memberId) throws IOException{
		//등록된 프로필 사진이 없을 경우 예외 발생.
		if(!this.existsByMemberId(memberId)) {
			throw new ProfilePictureNotFoundException();
		}
		MemberEntity member = MemberEntity.builder().memberId(memberId).build();
		//프로필 사진은 하나만 등록 가능하지만 두 개 이상 저장되어있을 경우 하나만 반환한다.
		List<ProfilePictureEntity> profilePictures = profilePictureRepository.findByMember(member);
		return ProfilePictureDTO.Response.of(profilePictures.get(0));
	}
	
	@Override
	public byte[] findProfilePictureImage(HttpHeaders headers, long fileId) throws IOException {
		Optional<ProfilePictureEntity> entityOpt = profilePictureRepository.findById(fileId);
		ProfilePictureEntity entity = entityOpt.orElseThrow(()-> new EntityNotFoundException("profilePictureEntity with fileId : " + fileId));
		
		FileUtil.setHeadersForImage(headers, entity.getOriginalFileName());
		return FileUtil.fileToByteArray(Paths.get(entity.getUploadPath(), entity.getStoredFileName()));
	}
	
	@Override
	@Transactional(rollbackFor = Exception.class)
	/* TODO 파일 트랜잭션
	 * 1.파일 삭제 성공 후 DB 정보 삭제 실패시 파일 복구 필요함.
	 * 2.DB 정보 삭제 성공, 원본 파일 삭제 성공 후 썸네일 삭제 실패 시 DB 정보 롤백 됨. 원본 파일 복구 필요함. */
	public void deleteProfilePictureByMemberId(String memberId) {
		//등록된 프로필 사진이 없을 경우 예외 발생.
		if(!this.existsByMemberId(memberId)) {
			throw new ProfilePictureNotFoundException();
		}
		
		MemberEntity member = MemberEntity.builder().memberId(memberId).build();
		//프로필 사진은 하나만 등록 가능하지만 두 개 이상 등록되어있을 경우 전부 삭제한다.
		List<ProfilePictureEntity> profilePictures = profilePictureRepository.findByMember(member);
		for(int i=0; i<profilePictures.size(); i++) {
			ProfilePictureEntity entity = profilePictures.get(i);
			try {
				//서버에 저장된 파일을 삭제한다.
				Path profilePicturePath = Paths.get(entity.getUploadPath(), entity.getStoredFileName());
				this.deleteProfilePictureFile(profilePicturePath);
			} catch(IOException e) {
				logger.debug("파일 삭제 실패[{}]", entity.getUploadPath() +"/"+ entity.getStoredFileName());
			}
			//DB에서 프로필 사진 정보를 삭제한다.
			profilePictureRepository.deleteByMember(member);
		}
	}
	
	@Override
	public void deleteProfilePictureFile(Path profilePicturePath) throws IOException {
		FileUtil.deleteFile(profilePicturePath); // 프로필 사진을 삭제한다.
		if(FileUtil.isImage(profilePicturePath)) { //이미지 파일일 경우 썸네일을 삭제한다.
			String thumbnailName = FileUtil.getThumbnaleName(profilePicturePath.getFileName().toString());
			String parentPath = profilePicturePath.getParent().toString();
			Path thumbnailPath = Paths.get(parentPath, thumbnailName);
			FileUtil.deleteFile(thumbnailPath);
		}
	}
	
	@Override
	public boolean existsByMemberId(String memberId) {
		MemberEntity member = MemberEntity.builder().memberId(memberId).build();
		return profilePictureRepository.existsByMember(member);
	}
}
