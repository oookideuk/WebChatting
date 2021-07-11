package com.example.demo.domain;

public class SearchType {
	
	public enum Room{
		TITLE
		/** 참가한 채팅방 목록 */
		, PARTICIPANT
		;
	}
	
	public enum Message{
		/** 해당 날짜 이전 메시지 목록 
		 *	날짜 기준 내림차순으로 정렬 후 가져온다. 
		 */
		LOEDATE
		/** 해당 날짜 이후 메시지 목록 
		 *	날짜 기준 오름차순으로 정렬 후 가져온다.
		 */
		,GOEDATE
	}
}