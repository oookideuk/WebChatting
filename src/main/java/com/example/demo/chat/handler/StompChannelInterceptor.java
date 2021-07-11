package com.example.demo.chat.handler;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.auth.jwt.JwtTokenProvider;
import com.example.demo.dto.ChatDTO;
import com.example.demo.service.ChatService;

@Component
public class StompChannelInterceptor implements ChannelInterceptor {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private JwtTokenProvider jwtTokenProvider;
	private ChatService chatService;
	@Value("${server.port}") private int port;
	
	public StompChannelInterceptor(JwtTokenProvider jwtTokenProvider, ChatService chatService) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.chatService = chatService;
	}

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {		
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
		//logger.debug("port1[{}] port2[{}]", port, webServerAppCtxt.getWebServer().getPort());
		String authorizationHeader;
		String accessToken;
		
		switch (accessor.getCommand()) {
			case CONNECT: 
				logger.debug("pre connect log");
				authorizationHeader = accessor.getFirstNativeHeader("Authorization");
				accessToken = jwtTokenProvider.getTokenFromHeader(authorizationHeader);
				jwtTokenProvider.validateToken(accessToken);
				
				break;
			case SUBSCRIBE:
				logger.debug("pre subscribe log");
				authorizationHeader = accessor.getFirstNativeHeader("Authorization");
				accessToken = jwtTokenProvider.getTokenFromHeader(authorizationHeader);
				jwtTokenProvider.validateToken(accessToken);
				
				break;
			case DISCONNECT:
				try {
					logger.debug("pre disconnect log");
					String sessionId = accessor.getSessionId();
					String hostAddress = InetAddress.getLocalHost().getHostAddress();
					//채팅 방 오프라인 시간을 업데이트한다.
					chatService.updateRoomOfflineDate(sessionId, hostAddress, port);
					//온라인 참가자 정보를 지운다.
					chatService.deleteOnlineParticipant(sessionId, hostAddress, port);
					
				} catch (UnknownHostException e) {
					logger.error("UnknownHostException[{}]", e);
				} catch (Exception e) {
					logger.debug("Exception[{}]", e);
				}
				break;
			default:				
				break;
		}
		return message;
	}

	@Override
	public void postSend(Message<?> message, MessageChannel channel, boolean sent){
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
		String sessionId = accessor.getSessionId();
		switch(accessor.getCommand()) {
		
			case SUBSCRIBE:
				logger.debug("post subscribe log");
				String type = accessor.getFirstNativeHeader("type");
				//chatRoom 구독
				if(type != null && type.equals("chatRoom")) {
					try {
						this.subscribeChatRoom(accessor);
					} catch (UnknownHostException e) {
						logger.error("UnknownHostException[{}]", e);
						throw new RuntimeException(e);
					} catch (Exception e) {
						logger.debug("Exception[{}]", e);
						throw new RuntimeException(e);
					}
					
					
					String destination = accessor.getDestination();
					String roomId = chatService.getRoomIdFromDestination(destination);
					String authorizationHeader = accessor.getFirstNativeHeader("Authorization");
					String accessToken = jwtTokenProvider.getTokenFromHeader(authorizationHeader);
					String participantId = jwtTokenProvider.getMemberId(accessToken);
					//구독 성공 메시지를 전달한다.
					chatService.sendSubscribeMessage(roomId, participantId);
				}
				
				break;
			case DISCONNECT:
				logger.debug("post disconnect log");
				// 서버에 저장한 web socket session은 close 했지만 map에 저장된 값이 안 비워진 경우 비운다.
				chatService.closeWebSocketSession(sessionId);
				
				break;
			default:
				break;
		}
	}
	
	/**
	 * ChatRoom을 구독한다.
	 */
	@Transactional
	private void subscribeChatRoom(StompHeaderAccessor accessor) throws UnknownHostException {
		String authorizationHeader = accessor.getFirstNativeHeader("Authorization");
		String accessToken = jwtTokenProvider.getTokenFromHeader(authorizationHeader);
		String participantId = jwtTokenProvider.getMemberId(accessToken);
		String memberName = jwtTokenProvider.getMemberName(accessToken);
		String destination = accessor.getDestination();
		String roomId = chatService.getRoomIdFromDestination(destination);
		String sessionId = accessor.getSessionId();
		
		ChatDTO.EnterReq enterReq;
		enterReq = ChatDTO.EnterReq.builder()
				.roomId(roomId)
				.participantId(participantId)
				.participantName(memberName)
				.sessionId(sessionId)
				.hostAddress(InetAddress.getLocalHost().getHostAddress())
				.port(port)
				.build();
		chatService.enterRoom(enterReq);
	}
}
