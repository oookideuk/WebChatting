package com.example.demo.controller;

import java.io.IOException;
import java.security.Principal;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.auth.security.CustomUser;
import com.example.demo.domain.OrderType;
import com.example.demo.dto.ChatDTO;
import com.example.demo.service.ChatService;

/**
 * 채팅방의 생성 및 조회는 Rest api로 구현한다.
 */
@RestController
@RequestMapping("/v1")
public class ChatController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private ChatService chatService;	
	
	public ChatController(ChatService chatService) {
		this.chatService = chatService;
	}

	/**
	 * 채팅방을 생성한다.
	 */
	@PostMapping("/chat/rooms")
	public ChatDTO.CreateRes createRoom(@Valid ChatDTO.CreateReq createReq, Principal principal) {
		ChatDTO.CreateRes room = chatService.createRoom(createReq);
		
		return room;
	}
	
	/**
	 * 채팅방에서 나간다.
	 */
	@PostMapping("/chat/rooms/{roomId}/leave")
	public void leaveRoom(ChatDTO.LeaveReq leaveReq, Authentication authentication) {
		CustomUser customUser = (CustomUser) authentication.getPrincipal();
		leaveReq.setParticipantId(customUser.getMemberId());
		leaveReq.setParticipantName(customUser.getName());
		
		chatService.leaveRoom(leaveReq);
	}
	
	/**
	 * 메시지를 전달한다.
	 */
	@PostMapping("/chat/rooms/{roomId}/messages/send")
	public void sendMessage(@Valid ChatDTO.MessageReq messageReq, Authentication authentication) {
		CustomUser customUser = (CustomUser) authentication.getPrincipal();
		messageReq.setSender(customUser.getMemberId());
		messageReq.setName(customUser.getName());
		
		chatService.saveAndSendMessage(messageReq);
	}
	
	/**
	 * 파일을 업로드한다.
	 */
	@PostMapping("/chat/upload")
	public ResponseEntity<ChatDTO.UploadRes> uploadFile(ChatDTO.UploadReq uploadReq, Authentication authentication) throws IOException {
		CustomUser customUser = (CustomUser) authentication.getPrincipal();
		uploadReq.setUploaderId(customUser.getMemberId());
		logger.debug("uploadReq [{}]", uploadReq);
		
		ChatDTO.UploadRes uploadRes = chatService.uploadFile(uploadReq);
		
		return new ResponseEntity<ChatDTO.UploadRes>(uploadRes, HttpStatus.OK);
	}
	
	/**
	 * 파일을 다운로드한다.
	 */
	@GetMapping("/chat/download/{fileName}")
	public ResponseEntity<byte[]> donwloadFile(ChatDTO.DownloadReq downloadReq) throws IOException {
		logger.debug("downloadReq[{}]", downloadReq);
		
		HttpHeaders headers = new HttpHeaders();
		byte[] fileBytes = chatService.downloadFile(downloadReq, headers);
		
		return new ResponseEntity<byte[]>(fileBytes, headers, HttpStatus.OK);
	}
	
	/**
	 * 채팅방 리스트를 가져온다.
	 * 내가 참여한 채팅방 리스트를 가져온다 : SearchType.Room = PARTICIPNAT
	 */
	@GetMapping({"/chat/rooms", "/chat/participants/me/rooms"})
	public ResponseEntity<ChatDTO.RoomSearchRes> searchRooms(ChatDTO.RoomSearchReq searchReq, Pageable pageable, Authentication authentication){
		CustomUser customUser = (CustomUser) authentication.getPrincipal();
		searchReq.setParticipantId(customUser.getMemberId());
		ChatDTO.RoomSearchRes searchRes = chatService.searchRooms(searchReq, pageable);
		return new ResponseEntity<ChatDTO.RoomSearchRes>(searchRes, HttpStatus.OK);
	}
	
	/**
	 * 채팅방 메시지 목록을 가져온다.
	 */
	@GetMapping("/chat/rooms/{roomId}/messages")
	public ResponseEntity<ChatDTO.MessagesRes> getMessagesbyRoom(ChatDTO.MessagesReq req, Authentication authentication) {
		CustomUser customUser = (CustomUser) authentication.getPrincipal();
		req.setParticipantId(customUser.getMemberId());
		
		ChatDTO.MessagesRes res = chatService.getMessagesByRoom(req);
		return new ResponseEntity<ChatDTO.MessagesRes>(res, HttpStatus.OK);
	}
	
	/**
	 * 로그인한 회원이 채팅방에서 마지막으로 읽은 메시지를 가져온다. 
	 */
	@GetMapping("/chat/rooms/{roomId}/messages/lastRead")
	public ResponseEntity<ChatDTO.MessagesRes> getLastReadMessage(@PathVariable String roomId, Authentication auth){
		CustomUser customUser = (CustomUser) auth.getPrincipal();
		String participantId = customUser.getMemberId();
		
		ChatDTO.MessagesRes messagesRes = chatService.getLastReadMessage(roomId, participantId);
		return new ResponseEntity<ChatDTO.MessagesRes>(messagesRes, HttpStatus.OK);
	}
	
	/**
	 * 채팅방에 참여중인 회원 목록을 가져온다.
	 */
	@GetMapping("/chat/rooms/{roomId}/participants")
	public ResponseEntity<ChatDTO.ParticipantsRes> getParticipantsByRoom(ChatDTO.ParticipantsReq req, Authentication auth) {
		CustomUser customUser = (CustomUser) auth.getPrincipal();
		req.setParticipantId(customUser.getMemberId());
		
		ChatDTO.ParticipantsRes res = chatService.getParticipantsByRoom(req);
		return new ResponseEntity<ChatDTO.ParticipantsRes>(res, HttpStatus.OK);
	}
	
	/**
	 * 채팅방 정보를 가져온다.
	 */
	@GetMapping("/chat/rooms/{roomId}")
	public ChatDTO.Room findRoomById(@PathVariable String roomId) {
		return chatService.findRoomById(roomId);
	}
	
	/**
	 * 참가자가 채팅방에 존재하는지 확인한다.
	 */
	@GetMapping("/chat/rooms/{roomId}/participant/exist")
	public boolean existsParticipant(@PathVariable String roomId, Authentication auth) {
		CustomUser user = (CustomUser) auth.getPrincipal();
		return chatService.existsParticipant(roomId, user.getMemberId());
	}
}
