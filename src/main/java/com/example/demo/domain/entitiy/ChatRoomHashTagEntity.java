package com.example.demo.domain.entitiy;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter @ToString(exclude = {"room"})
@NoArgsConstructor
@Entity
@Table(name = "chat_room_hash_tag")
public class ChatRoomHashTagEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long hashTagId;
	private String hashTag;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "room_id")
	private ChatRoomEntity room;
	
	@Builder
	public ChatRoomHashTagEntity(long hashTagId, String hashTag, ChatRoomEntity room) {
		this.hashTagId = hashTagId;
		this.hashTag = hashTag;
		this.room = room;
	}
	
	
}
