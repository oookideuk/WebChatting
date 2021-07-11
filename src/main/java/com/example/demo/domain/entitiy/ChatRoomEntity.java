package com.example.demo.domain.entitiy;

import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter @ToString(exclude = {"hashTags", "messages","participants", "onlineParticipants", "chatFiles"})
@NoArgsConstructor
@Entity
@Table(name = "chat_room")
public class ChatRoomEntity {
	@Id
	@Column(name = "room_id")
	private String roomId;	//PK
	private String title;	//제목
	@CreationTimestamp
	private LocalDateTime createdDate;	//생성일
	
	@OneToMany(mappedBy = "room", cascade = CascadeType.ALL)
	private List<ChatRoomHashTagEntity> hashTags;
	@OrderBy(value = "created_date DESC")
	@OneToMany(mappedBy = "room", cascade = CascadeType.REMOVE)
	private List<ChatMessageEntity> messages;
	@OneToMany(mappedBy = "pk.roomId", cascade = CascadeType.REMOVE)
	private List<ChatParticipantEntity> participants;
	@OneToMany(mappedBy = "room")
	private List<ChatOnlineParticipantEntity> onlineParticipants;
	@OneToMany(mappedBy = "room", cascade = CascadeType.REMOVE)
	private List<ChatFileEntity> chatFiles;
	
	@Builder
	public ChatRoomEntity(String roomId, String title) {
		this.roomId = roomId;
		this.title = title;
	}
}
