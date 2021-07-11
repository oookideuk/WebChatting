package com.example.demo.chat.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler;

import com.example.demo.service.ChatService;

public class CustomSubProtocolWebSocketHandler extends SubProtocolWebSocketHandler {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private ChatService chatService;
	
	public CustomSubProtocolWebSocketHandler(MessageChannel clientInboundChannel, SubscribableChannel clientOutboundChannel) {
		super(clientInboundChannel, clientOutboundChannel);
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		chatService.addWebSocketSession(session);
		super.afterConnectionEstablished(session);
	}
}
