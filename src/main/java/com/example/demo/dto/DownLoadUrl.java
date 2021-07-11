package com.example.demo.dto;

public class DownLoadUrl {
	public static String makeProfilePictureUrl(long fileId) {
		//return "http://127.0.0.1:8080/v1/file/profilePicture/"+fileId;
		return "http://172.30.1.3:8080/v1/file/profilePicture/"+fileId;
	}
	
	public static String makeChatFileUrl(String fileName, long fileId) {
		//return "http://127.0.0.1:8080/v1/chat/download/"+fileName+"?id="+fileId;
		return "http://172.30.1.3:8080/v1/chat/download/"+fileName+"?id="+fileId;
	}
}
