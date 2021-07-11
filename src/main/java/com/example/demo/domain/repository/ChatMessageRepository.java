package com.example.demo.domain.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.entitiy.ChatMessageEntity;
import com.example.demo.domain.repository.queryDsl.ChatMessageRepositoryCustom;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long>, ChatMessageRepositoryCustom {
	@Query("select m from ChatMessageEntity m where m.room.roomId = :roomId")
	List<ChatMessageEntity> findByRoomId(@Param("roomId") String roomId);
	
	@Query("select count(*) from ChatMessageEntity m where m.room.roomId = :roomId and m.createdDate > :date")
	int countByRoomIdAndcreatedDate(@Param("roomId") String roomId, 
			@Param("date") LocalDateTime date);
}
