package com.example.demo.controller;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.lang.reflect.Type;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import com.example.demo.auth.jwt.JwtTokenProvider;
import com.example.demo.auth.security.CustomUser;
import com.example.demo.domain.ChatMessageType;
import com.example.demo.domain.Role;
import com.example.demo.domain.entitiy.ChatMessageEntity;
import com.example.demo.domain.entitiy.MemberEntity;
import com.example.demo.domain.repository.ChatMessageRepository;
import com.example.demo.dto.ChatDTO;
import com.example.demo.dto.ChatDTO.MessageRes;
import com.example.demo.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.Logger;
import io.jsonwebtoken.Claims;

@ActiveProfiles("local")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ChatControllerTest {
	private final Logger logger = (Logger) LoggerFactory.getLogger(this.getClass());
	private String aaa = "aaa";
	private String ccc = "ccc";
	private String aaaAccessToken;
	private String cccAccessToken;
	private BlockingQueue<ChatDTO.MessageRes> blockingQueue = new ArrayBlockingQueue<MessageRes>(1);
	private String webSocketUrl;
	private MockMvc mvc;
	@LocalServerPort private int port;
	@Autowired private JwtTokenProvider jwtTokenProvider;
	@Autowired private ObjectMapper objectMapper;
	@Autowired private ChatService chatService;
	@Autowired private ChatMessageRepository chatMessageRepo;
	
	@BeforeEach
	void setup(WebApplicationContext webApplicationContex) throws UnknownHostException {
		mvc = MockMvcBuilders.webAppContextSetup(webApplicationContex)
				//한글깨짐 방지
				.addFilters(new CharacterEncodingFilter("UTF-8", true))
				.apply(SecurityMockMvcConfigurers.springSecurity())
				.build();
		
		//WebSocket 세팅
		webSocketUrl = "ws://localhost:" + port + "/ws";
		//액세스 토큰 생성
		aaaAccessToken = makeAccessToken(aaa, "aaa111");
		cccAccessToken = makeAccessToken(ccc, "aaa111");
	}
	
	@Nested
	@ActiveProfiles("local")
	@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
	@DisplayName("채팅방 메시지 조회 테스트")
	class 채팅방메시지조회테스트{
		String roomId;
		StompSession aaaStompSession;
		List<ChatMessageEntity> messages = new ArrayList<>();
		
		@BeforeEach
		void setup() throws InterruptedException, ExecutionException, TimeoutException {
			//채팅방 생성
			String title = "getMessageByRoom 채팅방";
			roomId = makeChatRoom(title);
			//채팅방 입장
			aaaStompSession = enterChatRoom(roomId, aaaAccessToken);
			Thread.sleep(1000);
			//메시지 전송
			for(int i=1; i<=20; i++) {
				ChatDTO.MessageReq messageReq = ChatDTO.MessageReq.builder()
						.roomId(roomId)
						.sender(aaa)
						.name(aaa)
						.message("aaa"+i)
						.type(ChatMessageType.TEXT)
						.build();
				messages.add(chatService.saveAndSendMessage(messageReq));
			}
		}
		
		@Test
		@DisplayName("채팅방 메시지 조회")
		void getMessagesByRoom() throws Exception {
			ChatMessageEntity message = messages.get(0);	//입장 메시지 포함 두 번째 메시지다.
			mvc.perform(get("/v1/chat/rooms/" + roomId + "/messages")
					.header("Authorization", "Bearer "+ aaaAccessToken)
					.param("date", message.getCreatedDate().toString())
					.param("type", "GOEDATE")	//date 기준 오름차순으로 정렬후 가져온다.
					.param("size", "5"))
				.andExpect(jsonPath("$.roomId", is(roomId)))
				.andExpect(jsonPath("$.messages[0].roomId", is(roomId)))
				.andExpect(jsonPath("$.messages[0].message", is(aaa + 1)))
				.andExpect(jsonPath("$.messages[1].message", is(aaa + 2)))
				.andExpect(jsonPath("$.messages[2].message", is(aaa + 3)))
				.andExpect(jsonPath("$.messages.size()", is(5)))
				.andDo(print());
			
			message = messages.get(1);
			mvc.perform(get("/v1/chat/rooms/" + roomId + "/messages")
					.header("Authorization", "Bearer "+ aaaAccessToken)
					.param("date", message.getCreatedDate().toString())
					.param("type", "LOEDATE")	//date 기준 내림차순으로 정렬후 가져온다.
					.param("size", "5"))
				.andExpect(jsonPath("$.roomId", is(roomId)))
				.andExpect(jsonPath("$.messages[0].roomId", is(roomId)))
				.andExpect(jsonPath("$.messages[0].message", is(aaa + 2)))
				.andExpect(jsonPath("$.messages[1].message", is(aaa + 1)))
				.andExpect(jsonPath("$.messages.size()", is(3)))
				.andDo(print());
			
			message = messages.get(0);
			mvc.perform(get("/v1/chat/rooms/" + roomId + "/messages")
					.header("Authorization", "Bearer "+ aaaAccessToken)
					.param("date", message.getCreatedDate().toString())
					.param("type", "GOEDATE"))
				.andExpect(jsonPath("$.messages.size()", is(20)))	//size 전달 안 하면 전부 가져온다. 
				.andDo(print());
			
			//date 없는 경우 createdDate desc 정렬후 최신순으로 가져온다.
			mvc.perform(get("/v1/chat/rooms/" + roomId + "/messages")
					.header("Authorization", "Bearer "+ aaaAccessToken)
					.param("size", "10")
					.param("type", "LOEDATE"))
				.andExpect(jsonPath("$.messages[0].message", is(aaa + 20)))
				.andExpect(jsonPath("$.messages[1].message", is(aaa + 19)))
				.andExpect(jsonPath("$.messages.size()", is(10))) 
				.andDo(print());
		}
	}
	
	
	@Nested
	@ActiveProfiles("local")
	@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
	@DisplayName("채팅방에서 마지막으로 읽은 메시지 조회 테스트")
	class 마지막으로읽은메시지조회테스트{
		String roomId;
		StompSession aaaStompSession;
		StompSession cccStompSession;
		List<ChatMessageEntity> readMessages = new ArrayList<>();
		
		@BeforeEach
		void setup() throws InterruptedException, ExecutionException, TimeoutException {
			//채팅방 생성
			String title = "getLastReadMessage 채팅방";
			roomId = makeChatRoom(title);
			//aaa, ccc 채팅방 입장. stomp connect 및 subscribe
			aaaStompSession = enterChatRoom(roomId, aaaAccessToken);
			cccStompSession = enterChatRoom(roomId, cccAccessToken);
			Thread.sleep(1000);	//subscribe 완료까지 시간 걸림.
			//ccc 메시지 전송, 읽은 메시지
			for(int i=1; i<=5; i++) {
				ChatDTO.MessageReq messageReq = ChatDTO.MessageReq.builder()
						.roomId(roomId)
						.sender(ccc)
						.name(ccc)
						.message("ccc"+i)
						.type(ChatMessageType.TEXT)
						.build();
				readMessages.add(chatService.saveAndSendMessage(messageReq));
			}
			//aaa 채팅방 disconnect or 비활성화
			aaaStompSession.disconnect();
			Thread.sleep(1000);	//offlineDate 업데이트까지 시간 걸림.
			//ccc 메시지 전송, 안 읽은 메시지
			for(int i=6; i<=10; i++) {
				ChatDTO.MessageReq messageReq = ChatDTO.MessageReq.builder()
						.roomId(roomId)
						.sender(ccc)
						.name(ccc)
						.message("ccc"+i)
						.type(ChatMessageType.TEXT)
						.build();
				readMessages.add(chatService.saveAndSendMessage(messageReq));
			}
			Thread.sleep(1000);
		}
		
		@Test
		@DisplayName("마지막으로 읽은 메시지 조회")
		void getLastReadMessage1() throws Exception {
			//aaa 마지막으로 읽은 메시지 조회
			mvc.perform(get("/v1/chat/rooms/" + roomId + "/messages/lastRead")
					.header("Authorization", "Bearer "+ aaaAccessToken))
				.andExpect(jsonPath("$.messages[0].message", is(ccc + 5)))
				.andDo(print());
			
			//ccc 마지막으로 읽은 메시지 조회
			mvc.perform(get("/v1/chat/rooms/" + roomId + "/messages/lastRead")
					.header("Authorization", "Bearer "+ cccAccessToken))
				.andExpect(jsonPath("$.messages.size()", is(0)))
				.andDo(print());
		}
	}
	
	/**
	 * 채팅방에 입장한다.
	 * Stomp connect 및 subscribe
	 */
	private StompSession enterChatRoom(String roomId, String accessToken) throws InterruptedException, ExecutionException, TimeoutException {
		StompSession session = connectWebSocket(accessToken);
		StompHeaders stompHeaders = makeSubscribeStompHeader(roomId, accessToken);
		session.subscribe(stompHeaders, new ChatStompFrameHandler());
		return session;
	}
	
	/**
	 * 채팅방 생성 후 roomId 반환한다.
	 */
	private String makeChatRoom(String title) {
		ChatDTO.CreateReq createReq = ChatDTO.CreateReq.builder()
				.title(title)
		        .build();
		String roomId = chatService.createRoom(createReq).getRoomId();
		logger.debug("makeChatRoom roomId[{}]", roomId );
		return roomId;
	}
	
	/**
	 * 액세스 토큰 생성 후 반환한다.
	 */
	private String makeAccessToken(String memberId, String password) {
		MemberEntity member = MemberEntity.builder().memberId(memberId).name(memberId).password(password).role(Role.ROLE_MEMBER).build();
		CustomUser customUser = new CustomUser(member);
		Claims accessClaims = jwtTokenProvider.generateAccessClaims(UUID.randomUUID().toString(), customUser);
		return jwtTokenProvider.generateToken(accessClaims);
	}
	
	//Websocket 세팅
	private List<Transport> createTransportClient() {
		List<Transport> transports = new ArrayList<>(1);
		transports.add(new WebSocketTransport(new StandardWebSocketClient()));
		return transports;
	}
	
	class ChatStompFrameHandler implements StompFrameHandler{
		@Override
		public Type getPayloadType(StompHeaders headers) {
			return ChatDTO.MessageRes.class;
		}

		@Override
		public void handleFrame(StompHeaders headers, Object payload) {
			blockingQueue.add((ChatDTO.MessageRes) payload);
		}
	}
	
	/**
	 * 채팅방 웹소켓과 연결한다.
	 */
	private StompSession connectWebSocket(String accessToken) throws InterruptedException, ExecutionException, TimeoutException {
		WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer "+ accessToken);
        WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(createTransportClient()));
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        return stompClient.connect(webSocketUrl, handshakeHeaders, connectHeaders, new StompSessionHandlerAdapter() { }).get(1, SECONDS);
	}
	
	/**
	 * 채팅방을 구독을 위한 header를 만든다.
	 */
	private StompHeaders makeSubscribeStompHeader(String roomId, String accessToken) {
		StompHeaders headers = new StompHeaders();
        headers.setDestination("/topic/v1/chat/rooms/" + roomId);
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("type", "chatRoom");
        
        return headers;
	}
}
