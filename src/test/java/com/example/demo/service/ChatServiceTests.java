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
		//WebSocket ??????
		webSocketUrl = "ws://localhost:" + port + "/ws";
	}
	
	/**
	 * ????????? ?????? ?????????
	 */
	@Test
	void enterChatRoomTest() throws InterruptedException, ExecutionException, TimeoutException {
		//????????? ?????? ??????
		MemberEntity member = MemberEntity.builder().memberId("aaa").name("?????????").password("aaa111").role(Role.ROLE_MEMBER).build();
		CustomUser customUser = new CustomUser(member);
		Claims accessClaims = jwtTokenProvider.generateAccessClaims(UUID.randomUUID().toString(), customUser);
		String accessTokenAAA = jwtTokenProvider.generateToken(accessClaims);
		
		//????????? ??????
		ChatDTO.CreateReq createReq = ChatDTO.CreateReq.builder()
				.title("test1234")
		        .build();
		String roomId = chatService.createRoom(createReq).getRoomId();
		
		//????????? ??????
		StompSession stompSessionAAA = connectWebSocket(accessTokenAAA);
		StompHeaders stompHeaders = makeSubscribeStompHeader(roomId, accessTokenAAA);
		stompSessionAAA.subscribe(stompHeaders, new ChatStompFrameHandler());
		
		//message??? ??????????????? ????????????.
        ChatDTO.MessageRes message = blockingQueue.poll(5, SECONDS);
        assertEquals(roomId, message.getRoomId());
        assertEquals(ChatMessageType.ENTER, message.getType());
        logger.debug("message[{}]", message);
        //participant??? ??????????????? ????????????.
        assertEquals(true, chatParticipantRepo.findById(ChatParticipantPK.builder().roomId(roomId).participantId("aaa").build()).isPresent());
        //online participant??? ??????????????? ????????????.
        assertEquals(true, chatOnlineParticipantRepo.existsByRoom(ChatRoomEntity.builder().roomId(roomId).build()));
        //message??? ??????????????? ????????????.
        assertEquals(ChatMessageType.ENTER, chatMessageRepo.findByRoomId(roomId).get(0).getType());
        //stomp disconnect ??? online participant??? ??????????????? ????????????.
        stompSessionAAA.disconnect();
        Thread.sleep(1000);
        assertEquals(false, chatOnlineParticipantRepo.existsByRoom(ChatRoomEntity.builder().roomId(roomId).build()));
	}
	
	/**
	 * ????????? ????????? ?????????
	 */
	@Test
	void leaveRoomTest() throws JsonProcessingException, InterruptedException, ExecutionException, TimeoutException {
		//??? ?????? accessToken ??????
		MemberEntity member = MemberEntity.builder().memberId("aaa").name("?????????").password("aaa111").role(Role.ROLE_MEMBER).build();
		CustomUser customUser = new CustomUser(member);
		Claims accessClaims = jwtTokenProvider.generateAccessClaims(UUID.randomUUID().toString(), customUser);
		String accessTokenAAA = jwtTokenProvider.generateToken(accessClaims);
		
		member = MemberEntity.builder().memberId("ccc").name("?????????").password("aaa111").role(Role.ROLE_MEMBER).build();
		customUser = new CustomUser(member);
		accessClaims = jwtTokenProvider.generateAccessClaims(UUID.randomUUID().toString(), customUser);
		String accessTokenCCC = jwtTokenProvider.generateToken(accessClaims);
		
		//????????? ??????
		ChatDTO.CreateReq createReq = ChatDTO.CreateReq.builder()
        		.title("test1234")
        		.build();
		String roomId = chatService.createRoom(createReq).getRoomId();
        
        //????????? ??????
		StompSession stompSessionAAA = connectWebSocket(accessTokenAAA);
		StompSession stompSessionCCC = connectWebSocket(accessTokenCCC);
		
		//????????? ??????
		StompHeaders stompHeaders = makeSubscribeStompHeader(roomId, accessTokenAAA);
		stompSessionAAA.subscribe(stompHeaders, new ChatStompFrameHandler());
		logger.debug("?????? ?????????[{}]", blockingQueue.poll(5, SECONDS));
		
		stompHeaders = makeSubscribeStompHeader(roomId, accessTokenCCC);
		stompSessionCCC.subscribe(stompHeaders, new ChatStompFrameHandler());
		//????????? ??????????????? ??? ???????????? ?????? ????????? ??? ??? ????????????.
		logger.debug("?????? ?????????[{}]", blockingQueue.poll(5, SECONDS));
		logger.debug("?????? ?????????[{}]", blockingQueue.poll(5, SECONDS));
		Thread.sleep(1000);
        
		//aaa??? ccc??? ???????????? ??????????????? ????????????.
		assertEquals(true, chatParticipantRepo.existsById(ChatParticipantPK.builder().roomId(roomId).participantId("aaa").build()));
        assertEquals(true, chatParticipantRepo.existsById(ChatParticipantPK.builder().roomId(roomId).participantId("ccc").build()));
        //aaa??? ccc??? online ???????????? ????????????.
        assertEquals("aaa", chatOnlineParticipantRepo.findByRoomIdAndParticipantId(roomId, "aaa").get(0).getParticipantId());
        assertEquals("ccc", chatOnlineParticipantRepo.findByRoomIdAndParticipantId(roomId, "ccc").get(0).getParticipantId());
        
        //????????? ????????? ?????????
        //??? ??? ?????? ????????? ????????? - aaa
        ChatDTO.LeaveReq leaveReqAAA = ChatDTO.LeaveReq.builder()
        		.roomId(roomId)
        		.participantId("aaa")
        		.participantName("?????????")
        		.build();
		chatService.leaveRoom(leaveReqAAA);
		Thread.sleep(1000);
		
		//aaa??? WebSocket??? ???????????? ????????????.
		assertEquals(false, stompSessionAAA.isConnected());
		//???????????? ??????????????? ????????????.
		assertEquals(true, chatRoomRepo.existsById(roomId));
		//participant table??? aaa ?????? ????????? ????????????.
		assertEquals(false, chatParticipantRepo.existsById(ChatParticipantPK.builder().roomId(roomId).participantId("aaa").build()));
		//online participant table??? aaa ?????? ????????? ????????????.
		assertEquals(0, chatOnlineParticipantRepo.findByRoomIdAndParticipantId(roomId, "aaa").size());
		//????????? ???????????? ??????????????? ????????????.
		ChatDTO.MessageRes message = blockingQueue.poll(5, SECONDS);
		assertEquals(roomId, message.getRoomId());
		assertEquals(ChatMessageType.LEAVE, message.getType());
        
		//??? ??? ?????? ????????? ????????? - ccc
		ChatDTO.LeaveReq leaveReqCCC = ChatDTO.LeaveReq.builder()
        		.roomId(roomId)
        		.participantId("ccc")
        		.participantName("?????????")
        		.build();
		chatService.leaveRoom(leaveReqCCC);
		Thread.sleep(1000);
		
		//ccc??? WebSocket??? ???????????? ????????????.
		assertEquals(false, stompSessionCCC.isConnected());
		//????????? ?????? ????????? ????????????.
		assertEquals(false, chatRoomRepo.existsById(roomId));
		//participant table??? ccc ?????? ????????? ????????????.
		assertEquals(false, chatParticipantRepo.existsById(ChatParticipantPK.builder().roomId(roomId).participantId("ccc").build()));
		//online participant table??? ccc ?????? ????????? ????????????.
		assertEquals(0, chatOnlineParticipantRepo.findByRoomIdAndParticipantId(roomId, "ccc").size());
	}

	/**
	 * ????????? ?????? ?????????
	 */
	@Test
	void sendMessageTest() throws InterruptedException, ExecutionException, TimeoutException {
		//????????? ?????? ??????
		MemberEntity member = MemberEntity.builder().memberId("aaa").name("?????????").password("aaa111").role(Role.ROLE_MEMBER).build();
		CustomUser customUser = new CustomUser(member);
		Claims accessClaims = jwtTokenProvider.generateAccessClaims(UUID.randomUUID().toString(), customUser);
		String accessTokenAAA = jwtTokenProvider.generateToken(accessClaims);
		
		//????????? ??????
		ChatDTO.CreateReq createReq = ChatDTO.CreateReq.builder()
				.title("test1234").build();
		String roomId = chatService.createRoom(createReq).getRoomId();
		
		//????????? ??????
		StompSession stompSessionAAA = connectWebSocket(accessTokenAAA);
		//???????????? ????????????.
		StompHeaders stompHeaders = makeSubscribeStompHeader(roomId, accessTokenAAA);
		stompSessionAAA.subscribe(stompHeaders, new ChatStompFrameHandler());
		logger.debug("?????? ?????????[{}]", blockingQueue.poll(5, SECONDS));
		Thread.sleep(1000);
		
		//TEXT ????????? ?????? ?????????
		ChatDTO.MessageReq textMessageReq = ChatDTO.MessageReq.builder()
				.roomId(roomId)
				.type(ChatMessageType.TEXT)
				.sender("aaa")
				.name("?????????")
				.message("message")
				.build();
		chatService.saveAndSendMessage(textMessageReq);
		
		//TEXT ???????????? ?????????????????? ??????????????? ????????????.
		ChatDTO.MessageRes textMessageRes = blockingQueue.poll(5, SECONDS);
		assertEquals(ChatMessageType.TEXT, textMessageRes.getType());
		assertEquals("aaa", textMessageRes.getSender());
		logger.debug("textMessageRes[{}]", textMessageRes);
		
		//IMAGE ????????? ?????? ?????????
		ChatDTO.MessageReq imageMessageReq = ChatDTO.MessageReq.builder()
				.roomId(roomId)
				.type(ChatMessageType.IMAGE)
				.sender("aaa")
				.name("?????????")
				.attachUrl("image")
				.build();
		chatService.saveAndSendMessage(imageMessageReq);
		
		//IMAGE ???????????? ?????????????????? ??????????????? ????????????.
		ChatDTO.MessageRes imageMessageRes = blockingQueue.poll(5, SECONDS);
		assertEquals(ChatMessageType.IMAGE, imageMessageRes.getType());
		assertEquals("image", imageMessageRes.getAttachUrl());
		logger.debug("imageMessageRes[{}]", imageMessageRes);
	}
	
	@Nested
	@ActiveProfiles("local")
	@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
	@DisplayName("?????? ????????? ?????????")
	class uploadFileTest {
		private String roomId;
		private String accessTokenAAA;
		private String memberIdAAA = "aaa";
		private MockMultipartFile uploadFile;
		private String originalFileName = "???????????????1.png";
		
		@BeforeEach
		void setup() throws IOException, InterruptedException, ExecutionException, TimeoutException {
			//?????? ??????
			FileInputStream image = FileUtils.openInputStream(Paths.get("src/main/resources/static/img/basicProfilePicture.png").toAbsolutePath().toFile());
			uploadFile = new MockMultipartFile("uploadFile", originalFileName, MediaType.IMAGE_PNG_VALUE, image);
			
			//????????? ?????? ??????
			MemberEntity member = MemberEntity.builder().memberId(memberIdAAA).name("?????????").password("aaa111").role(Role.ROLE_MEMBER).build();
			CustomUser customUser = new CustomUser(member);
			Claims accessClaims = jwtTokenProvider.generateAccessClaims(UUID.randomUUID().toString(), customUser);
			accessTokenAAA = jwtTokenProvider.generateToken(accessClaims);
			
			//????????? ??????
			ChatDTO.CreateReq createReq = ChatDTO.CreateReq.builder()
					.title("????????? ??????").build();
			roomId = chatService.createRoom(createReq).getRoomId();
			
			//????????? ??????
			StompSession stompSessionAAA = connectWebSocket(accessTokenAAA);
			//???????????? ????????????.
			StompHeaders stompHeaders = makeSubscribeStompHeader(roomId, accessTokenAAA);
			stompSessionAAA.subscribe(stompHeaders, new ChatStompFrameHandler());
			logger.debug("?????? ?????????[{}]", blockingQueue.poll(5, SECONDS));
			Thread.sleep(1000);
		}
		
		@Test
		@DisplayName("?????? ?????????")
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
	@DisplayName("????????? ?????? ?????????")
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
			//?????????, ?????????, ????????? ??????
			for(int i = 1; i <= insertCount; i++) {
				//memberIdCCC ????????? ??????
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
				
				//memberIdAAA ????????? ??????
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
		@DisplayName("?????? ????????? ????????? ?????? ?????????")
		class searchRoomByParticipant{
			@Test
			@DisplayName("?????? ????????? ????????? ??????")
			void searchRoomByParticipant1() throws Exception {
				//?????? ?????? ??????
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
	@DisplayName("????????? ??? ????????? ?????? ?????????")
	class GetMessagesbyRoom{
		private String memberIdAAA = "aaa";
		private String memberIdCCC = "ccc";
		private String roomId;
		private int insertCount;
		
		@BeforeEach
		void setup() {
			insertCount = 20;
			//????????? ???????????? ????????????.
			this.insertTestData();
		}
		
		private void insertTestData() {
			//????????? ??????
			roomId = UUID.randomUUID().toString();
			ChatRoomEntity room = ChatRoomEntity.builder()
					.roomId(roomId)
					.title("????????? ????????? ??????").build();
			ChatRoomEntity retRoom = chatRoomRepo.save(room);
			
			//memberIdCCC ????????? ??????
			ChatParticipantPK participantPk = ChatParticipantPK.builder()
					.roomId(roomId)
					.participantId(memberIdCCC).build();
			ChatParticipantEntity participant = ChatParticipantEntity.builder()
					.pk(participantPk)
					.room(retRoom).build();
			chatParticipantRepo.save(participant);
			
			//memberIdCCC ????????? ??????
			for(int i =1; i <= insertCount; i++) {
				ChatMessageEntity message = ChatMessageEntity.builder()
						.type(ChatMessageType.TEXT)
						.message(memberIdCCC +i)
						.room(retRoom)
						.sender(memberIdCCC).build();
				chatMessageRepo.save(message);
			}
			
			//memberIdAAA ????????? ??????	
			participantPk = ChatParticipantPK.builder()
					.roomId(roomId)
					.participantId(memberIdAAA).build();
			participant = ChatParticipantEntity.builder()
					.pk(participantPk)
					.room(retRoom).build();
			chatParticipantRepo.save(participant);
			
			//memberIdAAA ????????? ??????
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
		@DisplayName("????????? ??? ????????? ??????")
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
	@DisplayName("????????? ??? ????????? ?????? ?????? ?????????")
	class GetParticipantsByRoom{
		private String memberIdAAA = "aaa";
		private String memberIdCCC = "ccc";
		private String roomId;
		
		@BeforeEach
		void setup() {
			//????????? ???????????? ????????????.
			this.insertTestData();
		}
		
		private void insertTestData() {
			//????????? ??????
			roomId = UUID.randomUUID().toString();
			ChatRoomEntity room = ChatRoomEntity.builder()
					.roomId(roomId)
					.title("????????? ????????? ??????").build();
			ChatRoomEntity retRoom = chatRoomRepo.save(room);
			
			//memberIdCCC ????????? ??????
			ChatParticipantPK participantPk = ChatParticipantPK.builder()
					.roomId(roomId)
					.participantId(memberIdCCC).build();
			ChatParticipantEntity participant = ChatParticipantEntity.builder()
					.pk(participantPk)
					.room(retRoom).build();
			chatParticipantRepo.save(participant);
			
			//memberIdAAA ????????? ??????	
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
		@DisplayName("????????? ??? ????????? ??????")
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
	
	
	
	
	
	//Websocket ??????
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
	 * ????????? ???????????? ????????????.
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
	 * ???????????? ????????? ?????? header??? ?????????.
	 */
	private StompHeaders makeSubscribeStompHeader(String roomId, String accessToken) {
		StompHeaders headers = new StompHeaders();
        headers.setDestination("/topic/v1/chat/rooms/" + roomId);
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("type", "chatRoom");
        
        return headers;
	}
}
