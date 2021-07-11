package com.example.demo.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

@SpringBootTest
public class FileUtilTests {
	private final static Logger logger = LoggerFactory.getLogger(FileUtil.class);
	
	@Test
	void saveFileTest() throws IOException {
		//fileName, originalFileName, MIME.type, content
		MockMultipartFile multipartFile = new MockMultipartFile("fileName.txt", "originalFileName.txt", MediaType.TEXT_PLAIN_VALUE, "abc123".getBytes());
		UploadFile uploadFile = new UploadFile();
		uploadFile.setFile(multipartFile);
		uploadFile.setFileInformation();
		
		FileUtil.makeDir(uploadFile.getUploadPath());
		FileUtil.saveFile(uploadFile);
		
		Path savedPath = Paths.get(uploadFile.getUploadPath().toString(), uploadFile.getStoredFileName());
		logger.debug("path[{}]", savedPath);
		assertEquals(true, savedPath.toFile().exists());
	}
	
	@Test
	void getStoredFileNameTest() {
		String originalFileName = FilenameUtils.getBaseName("abc.txt");
		String StoredFileName = FileUtil.getStoredFileName(originalFileName);
		logger.debug("storedFileName[{}]", StoredFileName);
	}
	
	@Test
	void getAbsoluteUploadPathTest() {
		Path uploadPath = FileUtil.getAbsoluteUploadPath();
		logger.debug("uploadPath[{}]", uploadPath.toString());
		
	}
	
	@Test
	void makeDirTest() throws IOException{
		Path uploadPath = FileUtil.getAbsoluteUploadPath();
		
		FileUtil.makeDir(uploadPath);
		//path == null 예외 테스트
		Assertions.assertThrows(IllegalArgumentException.class, () -> FileUtil.makeDir(null));
	}
	
	@Test
	void isImageTest() throws IOException {
		assertEquals(true, FileUtil.isImage(Paths.get("abc.jpeg")));
		assertEquals(true, FileUtil.isImage(Paths.get("abc.jpg")));
		assertEquals(true, FileUtil.isImage(Paths.get("abc.png")));
		assertEquals(true, FileUtil.isImage(Paths.get("abc.gif")));
		assertEquals(false, FileUtil.isImage(Paths.get("abc.txt")));
	}
	
	@Test
	void getContentTypeTest() throws IOException {
		assertEquals("image/jpeg", FileUtil.getContentType(Paths.get("abc.jpeg")));
		assertEquals("image/jpeg", FileUtil.getContentType(Paths.get("abc.jpg")));
		assertEquals("image/png", FileUtil.getContentType(Paths.get("abc.png")));
		assertEquals("image/gif", FileUtil.getContentType(Paths.get("abc.gif")));
		assertEquals("text/plain", FileUtil.getContentType(Paths.get("abc.txt")));
	}
	
	@Test
	void makeThumbnailTest() throws IOException {
		String basePath = "src/main/resources/static/img/";
		String fileName = "basicProfilePicture.png";
		Path path = Paths.get(basePath + fileName).toAbsolutePath();
		
		//썸네일 생성
		FileUtil.makeThumbnail(path);
		//파일이 이미 존재할 경우
		Assertions.assertThrows(IOException.class, () -> FileUtil.makeThumbnail(path));
		//파일이 존재 하지 않을 경우
		Path path2 = Paths.get(basePath + "sdakj.png").toAbsolutePath();
		Assertions.assertThrows(IllegalArgumentException.class, () -> FileUtil.makeThumbnail(path2));
		//이미지 파일이 아닐경우
		Path path3 = Paths.get(basePath + "txt.txt").toAbsolutePath();
		Assertions.assertThrows(IllegalArgumentException.class, () -> FileUtil.makeThumbnail(path3));
		//이미지 파일이 아닐경우. 확장자만 이미지.
		Path path4 = Paths.get(basePath + "txt.png").toAbsolutePath();
		Assertions.assertThrows(IllegalArgumentException.class, () -> FileUtil.makeThumbnail(path4));
		
		//썸네일 삭제
		String thumbnailName = FileUtil.getThumbnaleName(fileName);
		Path thumbnailPath = Paths.get(basePath, thumbnailName).toAbsolutePath();
		thumbnailPath.toFile().delete();
	}
	
	@Test
	void getThumbnaleNameTest() {
		assertEquals("t_abc.jpg", FileUtil.getThumbnaleName("abc.jpg"));
	}
	
	
	@Test
	void deleteFileTest() throws IOException {
		String fileName = "20201108091237399_1a10d143-64fa-4f04-960d-77dbfba6e9d9_image.png"; 
		Path uploadPath = FileUtil.getAbsoluteUploadPath();
		Path path = Paths.get(uploadPath.toString(), fileName);
		FileUtil.deleteFile(path);
	}
}
