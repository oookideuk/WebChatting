package com.example.demo.service;

import java.io.IOException;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import com.example.demo.domain.entitiy.ChatMessageEntity;
import com.example.demo.dto.ChatDTO;
import com.example.demo.dto.ChatDTO.DownloadReq;
import com.example.demo.dto.ChatDTO.LeaveReq;
import com.example.demo.dto.ChatDTO.MessageReq;
import com.example.demo.dto.ChatDTO.ParticipantsReq;
import com.example.demo.dto.ChatDTO.RoomSearchReq;
import com.example.demo.dto.ChatDTO.RoomSearchRes;
import com.example.demo.dto.ChatDTO.UploadReq;

public interface ChatService {
	
	/**
	 * 채팅방을 생성한다.
	 */
	ChatDTO.CreateRes createRoom(ChatDTO.CreateReq createReq);
	
	/**
	 * 채팅방에 참가한다.
	 */
	void enterRoom(ChatDTO.EnterReq enterReq);
	
	/**
	 * 채팅방을 나간다.
	 */
	void leaveRoom(LeaveReq leaveReq);
	
	/**
	 * online participant 정보를 삭제한다.
	 */
	void deleteOnlineParticipant(String sessionId, String hostAddress, int port);
	
	/**
	 * 메시지를 저장하고 전송한다. 
	 */
	ChatMessageEntity saveAndSendMessage(MessageReq messageReq);

	/**
	 * 파일을 업로드한다.
	 * @throws IOException 
	 */
	ChatDTO.UploadRes uploadFile(UploadReq uploadReq) throws IOException;
	
	/**
	 * 파일을 다운로드한다.
	 * @throws IOException 
	 */
	byte[] downloadFile(DownloadReq downloadReq, HttpHeaders headers) throws IOException;
	
	/**
	 * 채팅방 목록을 검색한다.
	 */
	RoomSearchRes searchRooms(RoomSearchReq searchReq, Pageable pageable);
	
	/**
	 * 채팅방 내 메시지 목록을 가져온다.
	 */
	ChatDTO.MessagesRes getMessagesByRoom(ChatDTO.MessagesReq req);
	
	/**
	 * 채팅방 내 참가자 목록을 가져온다.
	 */
	ChatDTO.ParticipantsRes getParticipantsByRoom(ParticipantsReq req);

	/**
	 * 채팅방 정보를 조회한다.
	 */
	ChatDTO.Room findRoomById(String roomId);
	
	/**
	 * destination정보에서 roomId 추출한다
	 */
	String getRoomIdFromDestination(String destination);

	/**
	 * WebSocketSession을 추가한다.
	 */
	void addWebSocketSession(WebSocketSession session);

	/**
	 * WebSocketSession을 닫는다.
	 */
	void closeWebSocketSession(String sessionId);
	void closeWebSocketSession(String sessionId, CloseStatus status);
	
	/**
	 * 웹 소켓 세션이 있는지 확인한다.
	 */
	boolean existsWebSocketSession(String sessionId);
	
	/**
	 * 채팅 방 오프라인 시간을 업데이트한다.
	 */
	void updateRoomOfflineDate(String sessionId, String hostAddress, int port);

	/**
	 * 참가자가 채팅방에 존재하는지 확인한다.
	 */
	boolean existsParticipant(String roomId, String participantId);

	/**
	 * 채팅방에서 마지막으로 읽은 메시지를 가져온다.
	 */
	ChatDTO.MessagesRes getLastReadMessage(String roomId, String participantId);

	/**
	 * 구독 성공 메시지를 보낸다.
	 */
	void sendSubscribeMessage(String roomId, String participantId);


	

	


	

	

	

	
}
