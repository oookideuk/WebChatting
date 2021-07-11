package com.example.demo.redis;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;

import com.example.demo.dto.ChatDTO;
import com.example.demo.service.ChatService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Redis에서 메시지가 발행(publish)되면 대기하고 있던 Redis Subscriber가 해당 메시지를 받아 처리한다.
 */
@Service
public class RedisSubscriber {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private ObjectMapper objectMapper;
	private SimpMessageSendingOperations messagingTemplate;
	private ChatService chatService;
	private String hostAddress;
	@Value("${server.port}") private int port;
	
	public RedisSubscriber(ObjectMapper objectMapper, SimpMessageSendingOperations messagingTemplate
				, ChatService chatService) {
		this.objectMapper = objectMapper;
		this.messagingTemplate = messagingTemplate;
		this.chatService = chatService;
	}
	
	@PostConstruct
	public void init() throws UnknownHostException {
		this.hostAddress = InetAddress.getLocalHost().getHostAddress();
	}

	/**
	 * 채팅방 메시지를 전송한다.
	 */
	public void sendChatRoomMessage(String publishMessage) {
		logger.debug("sendChatRoomMessage[{}]", publishMessage);
		try {
			ChatDTO.MessageRes chatMessage = objectMapper.readValue(publishMessage, ChatDTO.MessageRes.class);
			// 채팅방을 구독한 클라이언트에게 메시지를 전달한다.
			messagingTemplate.convertAndSend("/topic/v1/chat/rooms/" + chatMessage.getRoomId(), chatMessage);
		}catch(Exception e) {
			logger.error("error[{}]", e);
		}
	}
	
	/**
	 * 채팅방 웹소켓 세션을 닫는다.
	 */
	public void disconnectChatRoom(String publishMessage) {
		try {
			List<ChatDTO.WebSocketSessionCloseReq> reqs = objectMapper.readValue(publishMessage, new TypeReference<List<ChatDTO.WebSocketSessionCloseReq>>(){});
			for(ChatDTO.WebSocketSessionCloseReq req : reqs) {
				//해당 서버의 WebSocketSession을 닫는다.
				if(this.hostAddress.equals(req.getHostAddress()) && this.port == req.getPort()) {
					chatService.closeWebSocketSession(req.getSessionId(), new CloseStatus(1000, "LEAVE"));
				}
			}
		} catch (Exception e) {
			logger.error("disconnectChatRoom[{}]", e);
		}
	}
}
