package com.example.demo.common;

import java.io.Serializable;
import java.nio.file.Path;

import org.springframework.web.multipart.MultipartFile;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class UploadFile implements Serializable{
	private static final long serialVersionUID = 5715663612317754778L;
	protected MultipartFile file;
	protected String originalFileName;
	protected String storedFileName;
	protected Path uploadPath;
	protected long size;
	private String type;
	
	protected UploadFile() {
		
	}
	
	public UploadFile(MultipartFile file) {
		this.file = file;
	}
	
	/**
	 * 파일의 기본 정보를 세팅한다.
	 */
	public void setFileInformation() {
		this.originalFileName = file.getOriginalFilename();
		this.storedFileName = FileUtil.getStoredFileName(this.originalFileName);
		this.uploadPath = FileUtil.getAbsoluteUploadPath();
		this.size = file.getSize();
		this.type = file.getContentType();
	}
}
