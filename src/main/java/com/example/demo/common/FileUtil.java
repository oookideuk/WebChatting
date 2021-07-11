package com.example.demo.common;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;
import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

public class FileUtil {
	private final static Logger logger = LoggerFactory.getLogger(FileUtil.class);
	private final static String basePath = "etc/upload_file";
	
	/**
	 * 파일을 저장한다.
	 */
	public static void saveFile(UploadFile uploadFile) throws IOException {
		if(uploadFile == null || uploadFile.getSize() == 0) {
			throw new IllegalArgumentException();
		}
		
		InputStream uploadFileInputStream = uploadFile.getFile().getInputStream(); 
		String storedFileName = uploadFile.getStoredFileName();
		Path uploadPath = uploadFile.getUploadPath();
		Path path = Paths.get(uploadPath.toString(), storedFileName);
		
		Files.copy(uploadFileInputStream, path, StandardCopyOption.REPLACE_EXISTING);
	}
	
	/**
	 * originalFileName을 기반으로 서버에 저장할 파일명을 생성한다.
	 */
	public static String getStoredFileName(String originalFileName) {
		if(originalFileName == null || originalFileName.equals("")) {
			throw new IllegalArgumentException();
		}
		return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")) + "_" + UUID.randomUUID().toString() + "_" + originalFileName;
	}
	
	/**
	 * 파일을 업로드할 절대경로를 생성한다.
	 */
	public static Path getAbsoluteUploadPath() {
		String year = Integer.toString(LocalDate.now().getYear());
		String month = Integer.toString(LocalDate.now().getMonthValue());
		String day = Integer.toString(LocalDate.now().getDayOfMonth());
		Path path = Paths.get(basePath,year,month,day).toAbsolutePath();
		return path;
	}
	
	/**
	 * 디렉토리를 생성한다.
	 */
	public static void makeDir(Path path) throws IOException {
		if(path == null) {
			throw new IllegalArgumentException();
		}
		
		if(!Files.exists(path)) { //디렉토리가 없을 경우 생성한다.
			Files.createDirectories(path);
		}
	}
	
	/**
	 * 이미지 파일인지 확인한다.
	 */
	public static boolean isImage(Path path) throws IOException {
		String type = FileUtil.getContentType(path);
		return FileUtil.isImage(type);
	}
	
	public static boolean isImage(String type) {
		if(type.startsWith("image")) {
			return true;
		}
		return false;
	}
	
	/**
	 * 파일의 contentType을 가져온다.
	 */
	public static String getContentType(Path path) throws IOException {
		String contentType = Files.probeContentType(path);
		return contentType;
	}
	
	/**
	 * 썸네일을 생성한다.
	 */
	public static void makeThumbnail(Path Path)  throws IOException {
		if(Path == null || !Path.toFile().exists() || !isImage(Path)) {
			throw new IllegalArgumentException();
		}
		
		String fileName = Path.getFileName().toString();
		String thumbnailName = FileUtil.getThumbnaleName(fileName);
		String extension = FilenameUtils.getExtension(fileName);
		
		// 이미지 읽기 버퍼. 이미지 파일이 아닐경우 null을 반환한다.
		BufferedImage srcImg = ImageIO.read(Path.toFile());
		// 원본 이미지의 비율을 유지하면서 높이를 50px로 하여 썸네일을 만든다.
		BufferedImage destImg = Scalr.resize(srcImg, Scalr.Method.AUTOMATIC, Scalr.Mode.FIT_TO_HEIGHT, 50);
		
		Path thumbPath = Files.createFile(Paths.get(Path.getParent().toString(), thumbnailName));
		ImageIO.write(destImg, extension, thumbPath.toFile());
	}
	
	/**
	 * 썸네일 이름을 생성한다.
	 */
	public static String getThumbnaleName(String fileName) {
		if(fileName == null || fileName.equals("")) {
			throw new IllegalArgumentException();
		}
		return "t_" + fileName;
	}
	
	/**
	 * 파일을 삭제한다.
	 */
	public static void deleteFile(Path path) throws IOException {
		if(path == null) {
			throw new IllegalArgumentException();
		}
		if(path.toFile().exists()) {
			Files.delete(path);
			logger.debug("deleted file name[{}]", path.getFileName());
		}
	}
	
	/**
	 * 파일 -> byte[]
	 */
	public static byte[] fileToByteArray(Path path) throws IOException{
		if(path == null || !path.toFile().exists()) {
			throw new IllegalArgumentException("파일이 존재하지 않음.");
		}
		
	   	return Files.readAllBytes(path);
	}
	
	/**
	 * 이미지 파일을 위한 응답 헤더를 설정한다.
	 * 웹 브라우저에서 이미지를 나타내는게 가능하며 '새 탭에서 이미지 열기' 가능하다.
	 */
	public static void setHeadersForImage(HttpHeaders headers, String fileName) throws IOException {
		String contentType = FileUtil.getContentType(Paths.get(fileName));
		headers.set(HttpHeaders.CONTENT_TYPE, contentType);
		headers.setExpires(-1);
		headers.set("Content-Transfer-Encoding", "binary");
		headers.setPragma("no-cache;");
		
		String docName = URLEncoder.encode(fileName,"UTF-8").replace("+", "%20");
		headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + docName + "\"; filename*=UTF-8''" + docName);
	}
	
	/**
	 * 파일 다운로드를 위한 응답 헤더 설정를 설정한다.
	 * 웹 브라우저에서 이미지를 나타내는게 가능하지만 '새 탭에서 이미지 열기' 클릭시 이미지가 다운로드 된다.
	 */
	public static void setHeadersForDownload(HttpHeaders headers, String fileName) throws UnsupportedEncodingException {  
		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		headers.setExpires(-1);
		headers.set("Content-Transfer-Encoding", "binary");
		headers.setPragma("no-cache;");
		
		String docName = URLEncoder.encode(fileName,"UTF-8").replace("+", "%20");
		headers.set(HttpHeaders.CONTENT_DISPOSITION, "attach; filename=\"" + docName + "\"; filename*=UTF-8''" + docName);
	}
}


