package com.example.demo.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.domain.ChatMessageType;
import com.example.demo.domain.SearchType;
import com.example.demo.domain.entitiy.ChatMessageEntity;
import com.example.demo.domain.entitiy.ChatParticipantEntity;
import com.example.demo.domain.entitiy.ChatRoomEntity;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

public class ChatDTO {	
	@Getter @Setter @ToString
	@Builder @NoArgsConstructor @AllArgsConstructor
	public static class CreateReq implements Serializable{
		private static final long serialVersionUID = -7247955529609950024L;
		@NotBlank
		private String title;
		
		public ChatRoomEntity toEntity() {
			return ChatRoomEntity.builder()
					.roomId(UUID.randomUUID().toString())
					.title(this.title)
					.build();
		}
	}
	
	@Getter @Setter @ToString
	@Builder
	public static class CreateRes implements Serializable{
		private static final long serialVersionUID = -5562171913673363739L;
		private String roomId; // ID
		private String title; // 제목
		
		public static ChatDTO.CreateRes of(ChatRoomEntity entity){
			return ChatDTO.CreateRes.builder()
					.roomId(entity.getRoomId())
					.title(entity.getTitle())
					.build();
		}
	}
	
	@Getter @Setter @ToString
	@Builder @NoArgsConstructor @AllArgsConstructor
	public static class EnterReq implements Serializable{
		private static final long serialVersionUID = -2668259509351366144L;
		private String roomId;
		private String participantId;
		private String participantName;
		private String sessionId;
		private String hostAddress;
		private int port;
	}
	
	@Getter @Setter @ToString
	@Builder @NoArgsConstructor @AllArgsConstructor
	public static class LeaveReq implements Serializable{
		private static final long serialVersionUID = -3114757269565751339L;
		private String roomId;
		private String participantId;
		private String participantName;
	}
	
	@Getter @Setter @ToString
	@Builder @NoArgsConstructor @AllArgsConstructor
	public static class MessageReq implements Serializable{
		private static final long serialVersionUID = -2620432232948545040L;
		@NotBlank
		private String roomId;
		private long messageId;
		@NotNull
		private ChatMessageType type;
		private String sender;
		private String name;
		@NotBlank
		private String message;
		private String attachUrl;
		private LocalDateTime createdDate;
		
		public ChatDTO.MessageRes toMessageRes(){
			return ChatDTO.MessageRes.builder()
					.roomId(this.roomId)
					.type(this.type)
					.sender(this.sender)
					.name(this.name)
					.message(this.message)
					.attachUrl(this.attachUrl)
					.createdDate(this.createdDate)
					.messageId(this.messageId)
					.build();
		}
		
		public ChatMessageEntity toChatMessageEntity() {
			return ChatMessageEntity.builder()
					.room(ChatRoomEntity.builder().roomId(this.roomId).build())
					.type(this.type)
					.sender(this.sender)
					.message(this.message)
					.build();
		}
	}
	
	@Getter @Setter @ToString
	@Builder @NoArgsConstructor @AllArgsConstructor
	public static class MessageRes implements Serializable {
		private static final long serialVersionUID = 6873987975263941866L;
		private String roomId;
		private long messageId;
		private ChatMessageType type;	// TEXT, IMAGE, FILE, ENTER, LEAVE
		private String sender;
		private String name;
		private String message;
		private String attachUrl;
		//LocalDateTime을 Json String으로 변환할때 형식을 유지하도록 한다.
		@JsonSerialize(using = LocalDateTimeSerializer.class)
		private LocalDateTime createdDate;
	}
	
	@Getter @Setter @ToString
	@Builder @NoArgsConstructor @AllArgsConstructor
	public static class UploadReq implements Serializable{
		private static final long serialVersionUID = -8750217839814036087L;
		private String roomId;
		private String uploaderId;
		private MultipartFile uploadFile;
	}
	
	@Getter @Setter @ToString
	@Builder @NoArgsConstructor @AllArgsConstructor
	public static class UploadRes implements Serializable{
		private static final long serialVersionUID = 7876355812574574429L;
		private String url;
	}
	
	@Getter @Setter @ToString
	@Builder @NoArgsConstructor @AllArgsConstructor
	public static class DownloadReq implements Serializable{
		private static final long serialVersionUID = -2990909682596580223L;
		private String fileName;
		private long id;
	}
	
	@Getter @Setter @ToString
	@Builder @NoArgsConstructor @AllArgsConstructor
	public static class RoomSearchReq implements Serializable{
		private static final long serialVersionUID = 8511053482170415479L;
		private SearchType.Room searchType;
		private String keyword;
		private String participantId;
	}
	
	@Getter @Setter @ToString
	@Builder @NoArgsConstructor @AllArgsConstructor
	public static class RoomSearchRes implements Serializable{
		private static final long serialVersionUID = 3939300392877765250L;
		private List<ChatDTO.Room> rooms;
		private int page;
		private int size;
		private int totlaPage;
	}
	
	@Getter @Setter @ToString
	@Builder @NoArgsConstructor @AllArgsConstructor
	public static class MessagesReq implements Serializable {
		private static final long serialVersionUID = 607208535908124365L;
		private String roomId;
		private String participantId;
		//String to LocalDateTime convert
		@DateTimeFormat(iso = ISO.DATE_TIME)
		private LocalDateTime date;
		private SearchType.Message type;		
		private Long size;	// null일 경우 전부 가져온다.
	}
	
	@Getter @Setter @ToString
	@Builder @NoArgsConstructor @AllArgsConstructor
	public static class MessagesRes implements Serializable{
		private static final long serialVersionUID = 1676829234526794648L;
		private String roomId;
		private List<ChatDTO.MessageRes> messages;
	}
	
	@Getter @Setter @ToString
	@Builder @NoArgsConstructor @AllArgsConstructor
	public static class ParticipantsReq implements Serializable{
		private static final long serialVersionUID = -6458098567779723326L;
		private String roomId;
		private String participantId;
	}
	
	@Getter @Setter @ToString
	@Builder @NoArgsConstructor @AllArgsConstructor
	public static class ParticipantsRes implements Serializable{
		private static final long serialVersionUID = 6592357441902375766L;
		private String roomId;
		private List<ChatDTO.Participant> participants;
	}
	
	@Getter @Setter @ToString
	@Builder @NoArgsConstructor @AllArgsConstructor
	public static class WebSocketSessionCloseReq implements Serializable{
		private static final long serialVersionUID = 7420986318147970318L;
		private String sessionId;
		private String hostAddress;
		private int port;
	}
	
	@Getter @Setter @ToString
	@Builder @NoArgsConstructor @AllArgsConstructor
	public static class Room implements Serializable{
		private static final long serialVersionUID = 7435993641428898530L;
		private String roomId;
		private String title;
		private LocalDateTime createdDate;
		private String lastMessage;
		private ChatMessageType lastMessageType;
		private LocalDateTime lastMessageDate;
		private int unreadMessageCount;
		
		public static ChatDTO.Room of(ChatRoomEntity room) {
			ChatDTO.Room retRoom = ChatDTO.Room.builder()
					.roomId(room.getRoomId())
					.title(room.getTitle())
					.createdDate(room.getCreatedDate()).build();
			return retRoom;
		}
		
		//참가한 채팅 방의 마지막 메시지 정보를 추가한다.
		public void addLastMessageInfo(List<ChatMessageEntity> messages) {
			if(messages.size() > 0) {
				ChatMessageEntity lastMessage = messages.get(0);	//ChatRoomEntity에서 메시지 생성시간 내림차순으로 정렬되어있음.
				this.lastMessage = lastMessage.getMessage();
				this.lastMessageType = lastMessage.getType();
				this.lastMessageDate = lastMessage.getCreatedDate();
			}
		}
	}
	
	@Getter @Setter @ToString
	@Builder @NoArgsConstructor @AllArgsConstructor
	public static class Participant implements Serializable{
		private static final long serialVersionUID = 149520931817219695L;
		private String roomId;
		private String participantId;
		private String participantName;
		private LocalDateTime entryDate;
		private String profilePictureUrl;
		
		public static ChatDTO.Participant of(ChatParticipantEntity participant, String participantName
				, String profilePictureUrl){
			return ChatDTO.Participant.builder()
					.roomId(participant.getPk().getRoomId())
					.participantId(participant.getPk().getParticipantId())
					.participantName(participantName)
					.entryDate(participant.getEntryDate())
					.profilePictureUrl(profilePictureUrl).build();
		}
	}
}
