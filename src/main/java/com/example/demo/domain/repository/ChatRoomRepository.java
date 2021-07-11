package com.example.demo.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.entitiy.ChatRoomEntity;
import com.example.demo.domain.repository.queryDsl.ChatRoomRepositoryCustom;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoomEntity, String>, ChatRoomRepositoryCustom {
	
}
