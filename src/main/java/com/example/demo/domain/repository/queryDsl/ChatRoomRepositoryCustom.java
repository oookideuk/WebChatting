package com.example.demo.domain.repository.queryDsl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.demo.domain.entitiy.ChatRoomEntity;
import com.example.demo.dto.ChatDTO;

public interface ChatRoomRepositoryCustom {

	Page<ChatRoomEntity> search(ChatDTO.RoomSearchReq searchReq, Pageable pageable);

	
}
