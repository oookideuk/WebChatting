package com.example.demo.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.entitiy.ChatParticipantEntity;
import com.example.demo.domain.entitiy.ChatParticipantPK;

@Repository
public interface ChatParticipantRepository extends JpaRepository<ChatParticipantEntity, ChatParticipantPK> {
	
}
