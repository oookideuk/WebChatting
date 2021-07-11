package com.example.demo.domain;

public enum ChatMessageType {
	ENTER // 채팅방 참가
	, LEAVE // 채팅방 나가
	, SUBSCRIBE // 구독완료
	, TEXT // 문자
	, IMAGE // 이미지
	, FILE // 파일
	;
	
	public static boolean isBinaryTypeMessage(ChatMessageType type) {
		if(type.equals(ChatMessageType.IMAGE) || type.equals(ChatMessageType.FILE)) {
			return true;
		}else {
			return false;
		}
	}
}
