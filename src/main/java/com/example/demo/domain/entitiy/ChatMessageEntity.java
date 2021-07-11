package com.example.demo.domain.entitiy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

import com.example.demo.domain.ChatMessageType;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter @ToString(exclude = {"room", "attachs"})
@NoArgsConstructor
@Entity
@Table(name = "chat_message")
public class ChatMessageEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long messageId;
	@Enumerated(EnumType.STRING)
	private ChatMessageType type;
	private String message;
	@CreationTimestamp
	private LocalDateTime createdDate;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "room_id")
	private ChatRoomEntity room;
	private String sender;
	
	@OneToMany(mappedBy = "message", cascade = CascadeType.ALL)
	private List<ChatMessageAttachEntity> attachs = new ArrayList<>();
	
	@Builder
	public ChatMessageEntity(long messageId, ChatMessageType type, String message, LocalDateTime createdDate,
			ChatRoomEntity room, String sender) {
		this.messageId = messageId;
		this.type = type;
		this.message = message;
		this.createdDate = createdDate;
		this.room = room;
		this.sender = sender;
	}
	
	public void setMessageAttach(String attachUrl) {
		ChatMessageAttachEntity attach = ChatMessageAttachEntity.builder()
				.url(attachUrl)
				.message(this)
				.build();
		attachs.add(attach);
	}
	
}
