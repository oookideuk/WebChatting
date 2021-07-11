package com.example.demo.domain.entitiy;

import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter @ToString(exclude = {"room"})
@NoArgsConstructor
@Entity
@Table(name = "chat_file")
public class ChatFileEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long fileId;
	private String type;
	private String storedFileName;
	private String originalFileName;
	private String uploadPath;
	private long size;
	@CreationTimestamp
	private LocalDateTime createdDate;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name ="room_id")
	private ChatRoomEntity room;
	private String participantId;
	
	@Builder
	public ChatFileEntity(long fileId, String type, String storedFileName, String originalFileName, String uploadPath,
			long size, LocalDateTime createdDate, ChatRoomEntity room, String participantId) {
		this.fileId = fileId;
		this.type = type;
		this.storedFileName = storedFileName;
		this.originalFileName = originalFileName;
		this.uploadPath = uploadPath;
		this.size = size;
		this.createdDate = createdDate;
		this.room = room;
		this.participantId = participantId;
	}
}
