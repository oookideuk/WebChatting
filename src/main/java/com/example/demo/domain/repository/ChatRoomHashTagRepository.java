package com.example.demo.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.entitiy.ChatRoomHashTagEntity;

@Repository
public interface ChatRoomHashTagRepository extends JpaRepository<ChatRoomHashTagEntity, Long> {

}
