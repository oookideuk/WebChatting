package com.example.demo.chat.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

import com.example.demo.exception.ErrorCode;
import com.example.demo.exception.InvalidJwtException;
import com.example.demo.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class CustomStompSubProtocolErrorHandler extends StompSubProtocolErrorHandler {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private ObjectMapper objectMapper = new ObjectMapper();
	private ChatService chatService;

	public CustomStompSubProtocolErrorHandler(ChatService chatService) {
		super();
		this.chatService = chatService;
	}

	@Override
	public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> clientMessage, Throwable ex) {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);

		accessor.setMessage(ex.getMessage());
		accessor.setLeaveMutable(true);
		StompHeaderAccessor clientHeaderAccessor = null;
		if (clientMessage != null) {
			clientHeaderAccessor = MessageHeaderAccessor.getAccessor(clientMessage, StompHeaderAccessor.class);
			if (clientHeaderAccessor != null) {
				String receiptId = clientHeaderAccessor.getReceipt();
				if (receiptId != null) {
					accessor.setReceiptId(receiptId);
				}
			}
		}

		Throwable exception = ex;
		if (exception instanceof MessageDeliveryException) {
			exception = exception.getCause();
		}
		
		// web socket session이 닫힐 때 자동으로 CloseEvent message가 전송된다.
		// => StompChannelInterceptor에서 예외 발생할때 error message를 전송할 경우, error message와 CloseEvent message 두 개가 전송된다.
		// => client에서 예외를 구분해 처리하기 힘들다.
		// => error message 전송대신 session close로 처리하며 CloseEvent message의 reason으로 예외를 구분한다. 
		if (exception instanceof InvalidJwtException) {
			chatService.closeWebSocketSession(clientHeaderAccessor.getSessionId(), new CloseStatus(1000, ErrorCode.UNAUTHORIZED.getCode()));
			return null;

//			session close와 비교하기 위해 error message 전송은 주석 처리해 놓는다.			
//			try { 
//				ErrorResponse errorResponse = new ErrorResponse(ErrorCode.UNAUTHORIZED);
//				byte[] errorPayload = objectMapper.writeValueAsBytes(errorResponse);
//				accessor.setMessage(errorResponse.getCode());
//				return handleInternal(accessor, errorPayload, ex, clientHeaderAccessor); 
//			}catch (JsonProcessingException e) {
//				logger.error("JsonProcessingException[{}]", e); 
//			}
			 
		}

		return handleInternal(accessor, new byte[0], ex, clientHeaderAccessor);
	}
}
