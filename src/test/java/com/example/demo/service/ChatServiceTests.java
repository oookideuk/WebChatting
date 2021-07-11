package com.example.demo.service;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
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
import com.example.demo.domain.SearchType;
import com.example.demo.domain.entitiy.ChatMessageEntity;
import com.example.demo.domain.entitiy.ChatParticipantEntity;
import com.example.demo.domain.entitiy.ChatParticipantPK;
import com.example.demo.domain.entitiy.ChatRoomEntity;
import com.example.demo.domain.entitiy.MemberEntity;
import com.example.demo.domain.repository.ChatMessageRepository;
import com.example.demo.domain.repository.ChatOnlineParticipantRepository;
import com.example.demo.domain.repository.ChatParticipantRepository;
import com.example.demo.domain.repository.ChatRoomRepository;
import com.example.demo.dto.ChatDTO;
import com.example.demo.dto.ChatDTO.MessageRes;
import com.fasterxml.jackson.core.JsonProcessingException;

import io.jsonwebtoken.Claims;

@ActiveProfiles("local")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ChatServiceTests {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	@LocalServerPort private Integer port;
	private String webSocketUrl;
	private BlockingQueue<ChatDTO.MessageRes> blockingQueue = new ArrayBlockingQueue<MessageRes>(1);
	@Autowired private ChatService chatService;
	@Autowired private JwtTokenProvider jwtTokenProvider;
	@Autowired private ChatParticipantRepository chatParticipantRepo;
	@Autowired private ChatOnlineParticipantRepository chatOnlineParticipantRepo;
	@Autowired private ChatMessageRepository chatMessageRepo;
	@Autowired private ChatRoomRepository chatRoomRepo;
	
	@BeforeEach
	void init() throws InterruptedException, ExecutionException, TimeoutException, UnknownHostException {
		//WebSocket 세팅
		webSocketUrl = "ws://localhost:" + port + "/ws";
	}
	
	/**
	 * 채팅방 입장 테스트
	 */
	@Test
	void enterChatRoomTest() throws InterruptedException, ExecutionException, TimeoutException {
		//액세스 토큰 생성
		MemberEntity member = MemberEntity.builder().memberId("aaa").name("가나다").password("aaa111").role(Role.ROLE_MEMBER).build();
		CustomUser customUser = new CustomUser(member);
		Claims accessClaims = jwtTokenProvider.generateAccessClaims(UUID.randomUUID().toString(), customUser);
		String accessTokenAAA = jwtTokenProvider.generateToken(accessClaims);
		
		//채팅방 생성
		ChatDTO.CreateReq createReq = ChatDTO.CreateReq.builder()
				.title("test1234")
		        .build();
		String roomId = chatService.createRoom(createReq).getRoomId();
		
		//채팅방 연결
		StompSession stompSessionAAA = connectWebSocket(accessTokenAAA);
		StompHeaders stompHeaders = makeSubscribeStompHeader(roomId, accessTokenAAA);
		stompSessionAAA.subscribe(stompHeaders, new ChatStompFrameHandler());
		
		//message를 전송했는지 확인한다.
        ChatDTO.MessageRes message = blockingQueue.poll(5, SECONDS);
        assertEquals(roomId, message.getRoomId());
        assertEquals(ChatMessageType.ENTER, message.getType());
        logger.debug("message[{}]", message);
        //participant를 저장했는지 확인한다.
        assertEquals(true, chatParticipantRepo.findById(ChatParticipantPK.builder().roomId(roomId).participantId("aaa").build()).isPresent());
        //online participant를 저장했는지 확인한다.
        assertEquals(true, chatOnlineParticipantRepo.existsByRoom(ChatRoomEntity.builder().roomId(roomId).build()));
        //message를 저장했는지 확인한다.
        assertEquals(ChatMessageType.ENTER, chatMessageRepo.findByRoomId(roomId).get(0).getType());
        //stomp disconnect 후 online participant가 삭제됐는지 확인한다.
        stompSessionAAA.disconnect();
        Thread.sleep(1000);
        assertEquals(false, chatOnlineParticipantRepo.existsByRoom(ChatRoomEntity.builder().roomId(roomId).build()));
	}
	
	/**
	 * 채팅방 나가기 테스트
	 */
	@Test
	void leaveRoomTest() throws JsonProcessingException, InterruptedException, ExecutionException, TimeoutException {
		//두 명의 accessToken 생성
		MemberEntity member = MemberEntity.builder().memberId("aaa").name("가나다").password("aaa111").role(Role.ROLE_MEMBER).build();
		CustomUser customUser = new CustomUser(member);
		Claims accessClaims = jwtTokenProvider.generateAccessClaims(UUID.randomUUID().toString(), customUser);
		String accessTokenAAA = jwtTokenProvider.generateToken(accessClaims);
		
		member = MemberEntity.builder().memberId("ccc").name("바자다").password("aaa111").role(Role.ROLE_MEMBER).build();
		customUser = new CustomUser(member);
		accessClaims = jwtTokenProvider.generateAccessClaims(UUID.randomUUID().toString(), customUser);
		String accessTokenCCC = jwtTokenProvider.generateToken(accessClaims);
		
		//채팅방 생성
		ChatDTO.CreateReq createReq = ChatDTO.CreateReq.builder()
        		.title("test1234")
        		.build();
		String roomId = chatService.createRoom(createReq).getRoomId();
        
        //채팅방 연결
		StompSession stompSessionAAA = connectWebSocket(accessTokenAAA);
		StompSession stompSessionCCC = connectWebSocket(accessTokenCCC);
		
		//채팅방 구독
		StompHeaders stompHeaders = makeSubscribeStompHeader(roomId, accessTokenAAA);
		stompSessionAAA.subscribe(stompHeaders, new ChatStompFrameHandler());
		logger.debug("구독 메시지[{}]", blockingQueue.poll(5, SECONDS));
		
		stompHeaders = makeSubscribeStompHeader(roomId, accessTokenCCC);
		stompSessionCCC.subscribe(stompHeaders, new ChatStompFrameHandler());
		//채팅방 입장인원이 두 명이므로 입장 메시지 두 개 소비한다.
		logger.debug("구독 메시지[{}]", blockingQueue.poll(5, SECONDS));
		logger.debug("구독 메시지[{}]", blockingQueue.poll(5, SECONDS));
		Thread.sleep(1000);
        
		//aaa와 ccc가 채팅방에 입장했는지 확인한다.
		assertEquals(true, chatParticipantRepo.existsById(ChatParticipantPK.builder().roomId(roomId).participantId("aaa").build()));
        assertEquals(true, chatParticipantRepo.existsById(ChatParticipantPK.builder().roomId(roomId).participantId("ccc").build()));
        //aaa와 ccc가 online 상태인지 확인한다.
        assertEquals("aaa", chatOnlineParticipantRepo.findByRoomIdAndParticipantId(roomId, "aaa").get(0).getParticipantId());
        assertEquals("ccc", chatOnlineParticipantRepo.findByRoomIdAndParticipantId(roomId, "ccc").get(0).getParticipantId());
        
        //채팅방 나가기 테스트
        //두 명 있는 채팅방 나가기 - aaa
        ChatDTO.LeaveReq leaveReqAAA = ChatDTO.LeaveReq.builder()
        		.roomId(roomId)
        		.participantId("aaa")
        		.participantName("가나다")
        		.build();
		chatService.leaveRoom(leaveReqAAA);
		Thread.sleep(1000);
		
		//aaa의 WebSocket이 끊겼는지 확인한다.
		assertEquals(false, stompSessionAAA.isConnected());
		//채팅방이 존재하는지 확인한다.
		assertEquals(true, chatRoomRepo.existsById(roomId));
		//participant table에 aaa 삭제 됐는지 확인한다.
		assertEquals(false, chatParticipantRepo.existsById(ChatParticipantPK.builder().roomId(roomId).participantId("aaa").build()));
		//online participant table에 aaa 삭제 됐는지 확인한다.
		assertEquals(0, chatOnlineParticipantRepo.findByRoomIdAndParticipantId(roomId, "aaa").size());
		//나가기 메시지를 전송했는지 확인한다.
		ChatDTO.MessageRes message = blockingQueue.poll(5, SECONDS);
		assertEquals(roomId, message.getRoomId());
		assertEquals(ChatMessageType.LEAVE, message.getType());
        
		//한 명 있는 채팅방 나가기 - ccc
		ChatDTO.LeaveReq leaveReqCCC = ChatDTO.LeaveReq.builder()
        		.roomId(roomId)
        		.participantId("ccc")
        		.participantName("바자다")
        		.build();
		chatService.leaveRoom(leaveReqCCC);
		Thread.sleep(1000);
		
		//ccc의 WebSocket이 끊겼는지 확인한다.
		assertEquals(false, stompSessionCCC.isConnected());
		//채팅방 삭제 됐는지 확인한다.
		assertEquals(false, chatRoomRepo.existsById(roomId));
		//participant table에 ccc 삭제 됐는지 확인한다.
		assertEquals(false, chatParticipantRepo.existsById(ChatParticipantPK.builder().roomId(roomId).participantId("ccc").build()));
		//online participant table에 ccc 삭제 됐는지 확인한다.
		assertEquals(0, chatOnlineParticipantRepo.findByRoomIdAndParticipantId(roomId, "ccc").size());
	}

	/**
	 * 메시지 전송 테스트
	 */
	@Test
	void sendMessageTest() throws InterruptedException, ExecutionException, TimeoutException {
		//액세스 토큰 생성
		MemberEntity member = MemberEntity.builder().memberId("aaa").name("가나다").password("aaa111").role(Role.ROLE_MEMBER).build();
		CustomUser customUser = new CustomUser(member);
		Claims accessClaims = jwtTokenProvider.generateAccessClaims(UUID.randomUUID().toString(), customUser);
		String accessTokenAAA = jwtTokenProvider.generateToken(accessClaims);
		
		//채팅방 생성
		ChatDTO.CreateReq createReq = ChatDTO.CreateReq.builder()
				.title("test1234").build();
		String roomId = chatService.createRoom(createReq).getRoomId();
		
		//채팅방 연결
		StompSession stompSessionAAA = connectWebSocket(accessTokenAAA);
		//채팅방을 구독한다.
		StompHeaders stompHeaders = makeSubscribeStompHeader(roomId, accessTokenAAA);
		stompSessionAAA.subscribe(stompHeaders, new ChatStompFrameHandler());
		logger.debug("구독 메시지[{}]", blockingQueue.poll(5, SECONDS));
		Thread.sleep(1000);
		
		//TEXT 메시지 전송 테스트
		ChatDTO.MessageReq textMessageReq = ChatDTO.MessageReq.builder()
				.roomId(roomId)
				.type(ChatMessageType.TEXT)
				.sender("aaa")
				.name("가나다")
				.message("message")
				.build();
		chatService.saveAndSendMessage(textMessageReq);
		
		//TEXT 메시지가 클라이언트에 전송됐는지 확인한다.
		ChatDTO.MessageRes textMessageRes = blockingQueue.poll(5, SECONDS);
		assertEquals(ChatMessageType.TEXT, textMessageRes.getType());
		assertEquals("aaa", textMessageRes.getSender());
		logger.debug("textMessageRes[{}]", textMessageRes);
		
		//IMAGE 메시지 전송 테스트
		ChatDTO.MessageReq imageMessageReq = ChatDTO.MessageReq.builder()
				.roomId(roomId)
				.type(ChatMessageType.IMAGE)
				.sender("aaa")
				.name("가나다")
				.attachUrl("image")
				.build();
		chatService.saveAndSendMessage(imageMessageReq);
		
		//IMAGE 메시지가 클라이언트에 전송됐는지 확인한다.
		ChatDTO.MessageRes imageMessageRes = blockingQueue.poll(5, SECONDS);
		assertEquals(ChatMessageType.IMAGE, imageMessageRes.getType());
		assertEquals("image", imageMessageRes.getAttachUrl());
		logger.debug("imageMessageRes[{}]", imageMessageRes);
	}
	
	@Nested
	@ActiveProfiles("local")
	@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
	@DisplayName("파일 업로드 테스트")
	class uploadFileTest {
		private String roomId;
		private String accessTokenAAA;
		private String memberIdAAA = "aaa";
		private MockMultipartFile uploadFile;
		private String originalFileName = "테스트파일1.png";
		
		@BeforeEach
		void setup() throws IOException, InterruptedException, ExecutionException, TimeoutException {
			//파일 세팅
			FileInputStream image = FileUtils.openInputStream(Paths.get("src/main/resources/static/img/basicProfilePicture.png").toAbsolutePath().toFile());
			uploadFile = new MockMultipartFile("uploadFile", originalFileName, MediaType.IMAGE_PNG_VALUE, image);
			
			//액세스 토큰 세팅
			MemberEntity member = MemberEntity.builder().memberId(memberIdAAA).name("가나다").password("aaa111").role(Role.ROLE_MEMBER).build();
			CustomUser customUser = new CustomUser(member);
			Claims accessClaims = jwtTokenProvider.generateAccessClaims(UUID.randomUUID().toString(), customUser);
			accessTokenAAA = jwtTokenProvider.generateToken(accessClaims);
			
			//채팅방 생성
			ChatDTO.CreateReq createReq = ChatDTO.CreateReq.builder()
					.title("채팅방 생성").build();
			roomId = chatService.createRoom(createReq).getRoomId();
			
			//채팅방 연결
			StompSession stompSessionAAA = connectWebSocket(accessTokenAAA);
			//채팅방을 구독한다.
			StompHeaders stompHeaders = makeSubscribeStompHeader(roomId, accessTokenAAA);
			stompSessionAAA.subscribe(stompHeaders, new ChatStompFrameHandler());
			logger.debug("구독 메시지[{}]", blockingQueue.poll(5, SECONDS));
			Thread.sleep(1000);
		}
		
		@Test
		@DisplayName("파일 업로드")
		void uploadFile() throws IOException {
			ChatDTO.UploadReq uploadReq = ChatDTO.UploadReq.builder()
					.roomId(roomId)
					.uploaderId(memberIdAAA)
					.uploadFile(uploadFile).build();
			
			ChatDTO.UploadRes uploadRes = chatService.uploadFile(uploadReq);
			logger.debug("uploadRes[{}]", uploadRes);
			
		}
	}
	
	@Nested
	@ActiveProfiles("local")
	@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
	@DisplayName("채팅방 검색 테스트")
	class SearchRoomsTest{
		String memberIdAAA = "aaa";
		String memberIdCCC = "ccc";
		int insertCount;
		ChatDTO.RoomSearchReq searchReq;
		Pageable pageable;
		
		@BeforeEach
		void beforeSetup() {
			insertCount = 50;
			this.insertTestData();
		}
		
		private void insertTestData() {
			//채팅방, 참가자, 메시지 입력
			for(int i = 1; i <= insertCount; i++) {
				//memberIdCCC 데이터 입력
				String roomId = UUID.randomUUID().toString();
				ChatRoomEntity room = ChatRoomEntity.builder()
						.roomId(roomId)
						.title(memberIdCCC +i).build();
				ChatRoomEntity retRoom = chatRoomRepo.save(room);
				
				ChatParticipantPK participantPk = ChatParticipantPK.builder()
						.roomId(roomId)
						.participantId(memberIdCCC).build();
				ChatParticipantEntity participant = ChatParticipantEntity.builder()
						.pk(participantPk)
						.room(retRoom).build();
				chatParticipantRepo.save(participant);
				
				ChatMessageEntity message = ChatMessageEntity.builder()
						.type(ChatMessageType.TEXT)
						.message(memberIdCCC +i)
						.room(retRoom)
						.sender(memberIdCCC).build();
				chatMessageRepo.save(message);
				
				//memberIdAAA 데이터 입력
				roomId = UUID.randomUUID().toString();
				room = ChatRoomEntity.builder()
						.roomId(roomId)
						.title(memberIdAAA +i).build();
				retRoom = chatRoomRepo.save(room);
				
				participantPk = ChatParticipantPK.builder()
						.roomId(roomId)
						.participantId(memberIdAAA).build();
				participant = ChatParticipantEntity.builder()
						.pk(participantPk)
						.room(retRoom).build();
				chatParticipantRepo.save(participant);
				
				message = ChatMessageEntity.builder()
						.type(ChatMessageType.TEXT)
						.message(memberIdAAA +i)
						.room(retRoom)
						.sender(memberIdAAA).build();
				chatMessageRepo.save(message);
			}
		}
		
		@AfterEach
		void afterSetup() {
			chatRoomRepo.deleteAll();
		}
		
		@Nested
		@ActiveProfiles("local")
		@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
		@DisplayName("내가 참여한 채팅방 검색 테스트")
		class searchRoomByParticipant{
			@Test
			@DisplayName("내가 참여한 채팅방 검색")
			void searchRoomByParticipant1() throws Exception {
				//검색 조건 설정
				searchReq = ChatDTO.RoomSearchReq.builder()
						.searchType(SearchType.Room.PARTICIPANT)
						.participantId(memberIdAAA)
						.build();
				pageable = PageRequest.of(0, 30, Sort.by(Sort.Direction.DESC,"MESSAGE"));
				
				ChatDTO.RoomSearchRes searchRes = chatService.searchRooms(searchReq, pageable);
				for(ChatDTO.Room r : searchRes.getRooms()) {
					assertEquals(memberIdAAA + insertCount--, r.getTitle());
					logger.debug("room[{}]", r);
				}
				assertEquals(30, searchRes.getRooms().size());
				assertEquals(1, searchRes.getPage());
				assertEquals(2, searchRes.getTotlaPage());
			}	
		}
	}
	
	@Nested
	@ActiveProfiles("local")
	@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
	@DisplayName("채팅방 내 메시지 조회 테스트")
	class GetMessagesbyRoom{
		private String memberIdAAA = "aaa";
		private String memberIdCCC = "ccc";
		private String roomId;
		private int insertCount;
		
		@BeforeEach
		void setup() {
			insertCount = 20;
			//테스트 데이터를 입력한다.
			this.insertTestData();
		}
		
		private void insertTestData() {
			//채팅방 생성
			roomId = UUID.randomUUID().toString();
			ChatRoomEntity room = ChatRoomEntity.builder()
					.roomId(roomId)
					.title("테스트 채팅방 생성").build();
			ChatRoomEntity retRoom = chatRoomRepo.save(room);
			
			//memberIdCCC 채팅방 입장
			ChatParticipantPK participantPk = ChatParticipantPK.builder()
					.roomId(roomId)
					.participantId(memberIdCCC).build();
			ChatParticipantEntity participant = ChatParticipantEntity.builder()
					.pk(participantPk)
					.room(retRoom).build();
			chatParticipantRepo.save(participant);
			
			//memberIdCCC 메시지 전송
			for(int i =1; i <= insertCount; i++) {
				ChatMessageEntity message = ChatMessageEntity.builder()
						.type(ChatMessageType.TEXT)
						.message(memberIdCCC +i)
						.room(retRoom)
						.sender(memberIdCCC).build();
				chatMessageRepo.save(message);
			}
			
			//memberIdAAA 채팅방 입장	
			participantPk = ChatParticipantPK.builder()
					.roomId(roomId)
					.participantId(memberIdAAA).build();
			participant = ChatParticipantEntity.builder()
					.pk(participantPk)
					.room(retRoom).build();
			chatParticipantRepo.save(participant);
			
			//memberIdAAA 메시지 전송
			for(int i=1; i<=insertCount; i++) {
				ChatMessageEntity message = ChatMessageEntity.builder()
						.type(ChatMessageType.TEXT)
						.message(memberIdAAA +i)
						.room(retRoom)
						.sender(memberIdAAA).build();
				chatMessageRepo.save(message);
			}
		}
		
		@AfterEach
		void afterSetup() {
			chatRoomRepo.deleteAll();
		}
		
		@Test
		@DisplayName("채팅방 내 메시지 조회")
		void getMessagesbyRoom() throws Exception {
			Pageable pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.ASC,"CREATED_DATE"));
			ChatDTO.MessagesReq req = ChatDTO.MessagesReq.builder()
					.participantId(memberIdAAA)
					.roomId(roomId).build();
			
			ChatDTO.MessagesRes res = chatService.getMessagesByRoom(req);
			assertEquals(roomId, res.getRoomId());
			assertEquals(insertCount, res.getMessages().size());
			
			int count = 1;
			for(ChatDTO.MessageRes m : res.getMessages()) {
				assertEquals(memberIdAAA+count++, m.getMessage());
				logger.debug("message[{}]", m);
			}
		}
	}
	
	@Nested
	@ActiveProfiles("local")
	@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
	@DisplayName("채팅방 내 참가자 목록 조회 테스트")
	class GetParticipantsByRoom{
		private String memberIdAAA = "aaa";
		private String memberIdCCC = "ccc";
		private String roomId;
		
		@BeforeEach
		void setup() {
			//테스트 데이터를 입력한다.
			this.insertTestData();
		}
		
		private void insertTestData() {
			//채팅방 생성
			roomId = UUID.randomUUID().toString();
			ChatRoomEntity room = ChatRoomEntity.builder()
					.roomId(roomId)
					.title("테스트 채팅방 생성").build();
			ChatRoomEntity retRoom = chatRoomRepo.save(room);
			
			//memberIdCCC 채팅방 입장
			ChatParticipantPK participantPk = ChatParticipantPK.builder()
					.roomId(roomId)
					.participantId(memberIdCCC).build();
			ChatParticipantEntity participant = ChatParticipantEntity.builder()
					.pk(participantPk)
					.room(retRoom).build();
			chatParticipantRepo.save(participant);
			
			//memberIdAAA 채팅방 입장	
			participantPk = ChatParticipantPK.builder()
					.roomId(roomId)
					.participantId(memberIdAAA).build();
			participant = ChatParticipantEntity.builder()
					.pk(participantPk)
					.room(retRoom).build();
			chatParticipantRepo.save(participant);
		}
		
		@AfterEach
		void afterSetup() {
			chatRoomRepo.deleteAll();
		}
		
		@Test
		@DisplayName("채팅방 내 참가자 조회")
		void getParticipantsByRoom() throws Exception {
			ChatDTO.ParticipantsReq req = ChatDTO.ParticipantsReq.builder()
					.roomId(roomId)
					.participantId(memberIdAAA).build();
			ChatDTO.ParticipantsRes res = chatService.getParticipantsByRoom(req);
			
			assertEquals(roomId, res.getRoomId());
			assertEquals(2, res.getParticipants().size());
			for(ChatDTO.Participant p : res.getParticipants()) {
				logger.debug("participant[{}]", p);
			}
		}
	}
	
	
	
	
	
	//Websocket 세팅
	List<Transport> createTransportClient() {
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
