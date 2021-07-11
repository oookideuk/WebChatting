package com.example.demo.domain.repository;

import java.util.List;
import java.util.Optional;

import javax.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.entitiy.ChatOnlineParticipantEntity;
import com.example.demo.domain.entitiy.ChatOnlineParticipantPK;
import com.example.demo.domain.entitiy.ChatRoomEntity;


@Repository
public interface ChatOnlineParticipantRepository extends JpaRepository<ChatOnlineParticipantEntity, ChatOnlineParticipantPK> {
	@Query("select p from ChatOnlineParticipantEntity p where p.room.roomId = :roomId and p.participantId = :participantId")
	List<ChatOnlineParticipantEntity> findByRoomIdAndParticipantId(@Param("roomId") String roomId, @Param("participantId") String participantId);
	
	boolean existsByRoom(ChatRoomEntity room);
	
	@Query("select p from ChatOnlineParticipantEntity p where p.room.roomId = :roomId")
	List<ChatOnlineParticipantEntity> findByRoomId(@Param("roomId") String roomId);
	
	//@Lock(LockModeType.PESSIMISTIC_WRITE)
	//@Lock(LockModeType.PESSIMISTIC_FORCE_INCREMENT)
	@Lock(LockModeType.READ)
	@Query("select op from ChatOnlineParticipantEntity op where op.pk.sessionId = :#{#pk.sessionId} and op.pk.hostAddress = :#{#pk.hostAddress} and op.pk.port = :#{#pk.port}")
	Optional<ChatOnlineParticipantEntity> findByIdForUpdate(@Param("pk") ChatOnlineParticipantPK pk);
}
