package com.example.demo.domain.entitiy;

import java.time.LocalDateTime;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter @ToString(exclude = {"room"})
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "chat_participant")
public class ChatParticipantEntity {
	@EmbeddedId
	private ChatParticipantPK pk;
	@CreationTimestamp
	private LocalDateTime entryDate;
	@CreationTimestamp
	private LocalDateTime offlineDate; 
	
	@MapsId("roomId")	//ChatParticipantPK.roomdId와 매핑
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "room_id", referencedColumnName = "room_id") //ChatRoomEntity.roomId에 column(name="room_id") 추가하면 referencedColumnName = "room_id"로 작성, 추가하지 않으면 referencedColumnName = "roomId"로 작성
	private ChatRoomEntity room;
	
	public void updateOfflineDate(LocalDateTime offlineDate) {
		this.offlineDate = offlineDate;
	}
}
