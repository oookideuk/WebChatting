package com.example.demo.domain.repository.queryDsl;

import java.util.List;

import com.example.demo.domain.entitiy.ChatMessageEntity;
import com.example.demo.dto.ChatDTO;

public interface ChatMessageRepositoryCustom {

	/**
	 * 채팅방 내 메시지 목록을 조회한다.
	 */
	List<ChatMessageEntity> getMessagesByRoom(ChatDTO.MessagesReq req);
	
}
