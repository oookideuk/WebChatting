package com.example.demo.dto;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;

import org.springframework.web.multipart.MultipartFile;

import com.example.demo.common.UploadFile;
import com.example.demo.domain.entitiy.MemberEntity;
import com.example.demo.domain.entitiy.ProfilePictureEntity;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

public class ProfilePictureDTO {
	
	@Getter
	@Setter
	@ToString(callSuper = true)
	public static class UploadReq extends UploadFile {
		private static final long serialVersionUID = -1826273615387168865L;
		private String memberId;
		
		public UploadReq() {
			super();
		}
		
		@Builder
		public UploadReq(MultipartFile multipartFile, String memberId) {
			super(multipartFile);
			this.memberId = memberId;
		}	
		
		@Override
		public void setFileInformation() {
			super.setFileInformation();
		}
		
		public ProfilePictureEntity toEntity() {
			return ProfilePictureEntity.builder()
					.originalFileName(this.originalFileName)
					.storedFileName(this.storedFileName)
					.size(this.size)
					.uploadPath(this.uploadPath.toString())
					.member(MemberEntity.builder().memberId(this.memberId).build())
					.build();
		}
		
		
	}
	
	@Getter
	@ToString
	@Builder
	@EqualsAndHashCode
	public static class Response implements Serializable {
		private static final long serialVersionUID = -8753822105058797482L;
		private long fileId;
		private String originalFileName;
		private long size;
		private LocalDateTime registerDate;
		private String memberId;
		private String src;
		
		public static ProfilePictureDTO.Response of(ProfilePictureEntity entity) throws IOException {
			//등록된 프로필 사진이 없을 경우 null을 반환한다.
			if(entity == null) {return null;}
			return Response.builder()
					.fileId(entity.getFileId())
					.originalFileName(entity.getOriginalFileName())
					.size(entity.getSize())
					.registerDate(entity.getRegisterDate())
					.memberId(entity.getMember().getMemberId())
					.src(DownLoadUrl.makeProfilePictureUrl(entity.getFileId()))
					.build();
		}
	}
}
