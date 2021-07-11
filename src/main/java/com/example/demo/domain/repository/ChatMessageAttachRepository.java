package com.example.demo.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.entitiy.ChatMessageAttachEntity;

@Repository
public interface ChatMessageAttachRepository extends JpaRepository<ChatMessageAttachEntity, Long> {

}
