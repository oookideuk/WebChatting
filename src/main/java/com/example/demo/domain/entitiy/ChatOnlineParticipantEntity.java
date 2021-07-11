package com.example.demo.domain.entitiy;

import java.time.LocalDateTime;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter @ToString(exclude = {"room"})
@Builder @NoArgsConstructor
@Entity
@Table(name = "chat_online_participant")
public class ChatOnlineParticipantEntity {
	@EmbeddedId
	private ChatOnlineParticipantPK pk;
	@CreationTimestamp
	private LocalDateTime entryDate;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name ="room_id")
	private ChatRoomEntity room;
	private String participantId;
	
	@Builder
	public ChatOnlineParticipantEntity(ChatOnlineParticipantPK pk, LocalDateTime entryDate, ChatRoomEntity room,
			String participantId) {
		this.pk = pk;
		this.entryDate = entryDate;
		this.room = room;
		this.participantId = participantId;
	}
	
	
}
