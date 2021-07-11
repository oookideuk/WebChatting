package com.example.demo.domain;

public class OrderType {
	/** 채팅방 정렬 기준 */
	public enum Room{
		/** 채팅방 생성일 */
		CREATED_DATE
		/** 채팅방 내 최신 메시지 생성일 */
		, MESSAGE
		/** 채팅 방 제목*/
		, TITLE
		;	
	}
}
