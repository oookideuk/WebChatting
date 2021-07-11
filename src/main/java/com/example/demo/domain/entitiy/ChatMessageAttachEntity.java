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

@Getter @ToString(exclude = {"message"})
@NoArgsConstructor
@Entity
@Table(name = "chat_message_attach")
public class ChatMessageAttachEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long messageAttachId;
	private String url;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "message_id")
	private ChatMessageEntity message;
	
	@Builder
	public ChatMessageAttachEntity(long messageAttachId, String url, ChatMessageEntity message) {
		this.messageAttachId = messageAttachId;
		this.url = url;
		this.message = message;
	}
	
	
}
