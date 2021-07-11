package com.example.demo.controller;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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
import com.example.demo.domain.entitiy.ChatParticipantEntity;
import com.example.demo.domain.entitiy.ChatParticipantPK;
import com.example.demo.domain.entitiy.ChatRoomEntity;
import com.example.demo.domain.entitiy.MemberEntity;
import com.example.demo.domain.repository.ChatMessageRepository;
import com.example.demo.domain.repository.ChatParticipantRepository;
import com.example.demo.domain.repository.ChatRoomRepository;
import com.example.demo.dto.AuthDTO;
import com.example.demo.dto.AuthDTO.JwtAuthRes;
import com.example.demo.dto.ChatDTO;
import com.example.demo.dto.ChatDTO.MessageRes;
import com.example.demo.exception.ErrorCode;
import com.example.demo.exception.ErrorResponse;
import com.example.demo.service.AuthService;
import com.example.demo.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.Logger;
import io.jsonwebtoken.Claims;

@ActiveProfiles("local")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ChatControllerTests {
	private final Logger logger = (Logger) LoggerFactory.getLogger(this.getClass());
	private BlockingQueue<ChatDTO.MessageRes> blockingQueue = new ArrayBlockingQueue<MessageRes>(1);
	private String webSocketUrl;
	private MockMvc mvc;
	@Autowired private JwtTokenProvider jwtTokenProvider;
	@Autowired private ObjectMapper objectMapper;
	@Autowired private AuthService authService;
	@Autowired private ChatService chatService;
	@LocalServerPort private int port;
	@Autowired private ChatRoomRepository chatRoomRepo;
	@Autowired private ChatParticipantRepository chatParticipantRepo;
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
	}
	
	@Nested
	@ActiveProfiles("local")
	@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
	@DisplayName("채팅방 생성 테스트")
	class CreateRoomTest {
		private String memberIdAAA = "aaa";
		private String accessTokenAAA;
		
		@BeforeEach
		void setup() {
			//로그인
			AuthDTO.LoginReq loginReq = AuthDTO.LoginReq.builder().memberId(memberIdAAA).password("aaa111").build();
			JwtAuthRes authRes = authService.login(loginReq).getBody();
			accessTokenAAA = authRes.getAccessToken();
		}
		
		@Test
		@DisplayName("채팅방 생성")
		void createRoom() throws Exception {
//			for(int i = 1; i <= 201; i++ ) {
//				String title = ""+i;
//				mvc.perform(post("/v1/chat/rooms")
//						.header("Authorization", "Bearer " + accessTokenAAA)
//						.param("title", title))
//					.andExpect(jsonPath("$.title").value(title))
//					.andDo(print())
//					.andReturn();
//			}
			String title = "채팅방 생성 테스트333";
			mvc.perform(post("/v1/chat/rooms")
					.header("Authorization", "Bearer " + accessTokenAAA)
					.param("title", title))
				.andExpect(jsonPath("$.title").value(title))
				.andDo(print())
				.andReturn();
		}
		
		@Test
		@DisplayName("액세스 토큰 없음")
		void noAccessToken() throws Exception{
			String title = "채팅방 생성 테스트";
			ErrorResponse errorResponse = new ErrorResponse(ErrorCode.UNAUTHORIZED);
			String errorResponseJson = objectMapper.writeValueAsString(errorResponse);
			
			mvc.perform(post("/v1/chat/rooms")
					.param("title", title))
				.andExpect(content().json(errorResponseJson))
				.andDo(print())
				.andReturn();
		}
		
		@Test
		@DisplayName("validation - title")
		void validateTitle() throws Exception {
			mvc.perform(post("/v1/chat/rooms")
					.header("Authorization", "Bearer " + accessTokenAAA)
					.param("title", "   "))
				.andExpect(jsonPath("$.code", is("INVALID_INPUT_VALUE")))
				.andDo(print())
				.andReturn();
		}
	}
	
	@Nested
	@ActiveProfiles("local")
	@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
	@DisplayName("채팅방 입장")
	class enterRoomTest{
		private String roomId = "417dba2f-6278-4ced-a5c8-b8bc307a21ad";
		private String memberId = "ccc";
		private String accessToken;
		
		@Test
		@DisplayName("채팅방 입장")
		void enterRoom() throws InterruptedException, ExecutionException, TimeoutException {
			//로그인
			AuthDTO.LoginReq loginReq = AuthDTO.LoginReq.builder().memberId(memberId).password("aaa111").build();
			JwtAuthRes authRes = authService.login(loginReq).getBody();
			accessToken = authRes.getAccessToken();
			
			//채팅방 연결
			StompSession stompSessionAAA = connectWebSocket(accessToken);
			//채팅방을 구독한다.
			StompHeaders stompHeaders = makeSubscribeStompHeader(roomId, accessToken);
			stompSessionAAA.subscribe(stompHeaders, new ChatStompFrameHandler());
			logger.debug("구독 메시지[{}]", blockingQueue.poll(5, SECONDS));
			Thread.sleep(1000);
		}	
	}
	
	@Nested
	@ActiveProfiles("local")
	@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
	@DisplayName("채팅방 나가기 테스트")
	class LeaveRoomTest{
		private String roomId;
		private String memberIdAAA = "ccc";
		private String accessTokenAAA;
		
		@BeforeEach
		void setup() throws InterruptedException, ExecutionException, TimeoutException {
			//로그인
			AuthDTO.LoginReq loginReq = AuthDTO.LoginReq.builder().memberId(memberIdAAA).password("aaa111").build();
			JwtAuthRes authRes = authService.login(loginReq).getBody();
			accessTokenAAA = authRes.getAccessToken();
			
			//채팅방 생성
			ChatDTO.CreateReq createReq = ChatDTO.CreateReq.builder()
					.title("채팅방 생성").build();
			roomId = chatService.createRoom(createReq).getRoomId();
			logger.debug("roomId [{}]", roomId);
			
			//채팅방 연결
			StompSession stompSessionAAA = connectWebSocket(accessTokenAAA);
			//채팅방을 구독한다.
			StompHeaders stompHeaders = makeSubscribeStompHeader(roomId, accessTokenAAA);
			stompSessionAAA.subscribe(stompHeaders, new ChatStompFrameHandler());
			logger.debug("구독 메시지[{}]", blockingQueue.poll(5, SECONDS));
			Thread.sleep(1000);
		}
		
		@Test
		@DisplayName("채팅방 나가기")
		void leaveRoom() throws Exception {
			mvc.perform(post("/v1/chat/rooms/"+roomId+"/leave")
					.header("Authorization", "Bearer " + accessTokenAAA))
				.andDo(print());
		}
	}
	
	@Nested
	@ActiveProfiles("local")
	@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
	@DisplayName("메시지 보내기 테스트")
	class SendMessageTest{
		private String roomId;
		private String memberIdAAA = "aaa";
		private String accessTokenAAA;
		
		@BeforeEach
		void setup() throws InterruptedException, ExecutionException, TimeoutException {
			//로그인
			AuthDTO.LoginReq loginReq = AuthDTO.LoginReq.builder().memberId(memberIdAAA).password("aaa111").build();
			JwtAuthRes authRes = authService.login(loginReq).getBody();
			accessTokenAAA = authRes.getAccessToken();
			
			//채팅방 생성
			ChatDTO.CreateReq createReq = ChatDTO.CreateReq.builder()
					.title("채팅방 생성").build();
			roomId = chatService.createRoom(createReq).getRoomId();			
			
			//채팅방 연결
			StompSession stompSessionAAA = connectWebSocket(accessTokenAAA);
			//채팅방을 구독한다.
			StompHeaders stompHeaders = makeSubscribeStompHeader(roomId, accessTokenAAA);
			stompSessionAAA.subscribe(stompHeaders, new ChatStompFrameHandler());
			Thread.sleep(1000);
			logger.debug("구독 메시지[{}]", blockingQueue.poll(5, SECONDS));
		}
		
		@Test
		@DisplayName("텍스트 메시지 보내기")
		void sendTextMessage() throws Exception {
			mvc.perform(post("/v1/chat/rooms/"+roomId+"/messages/send")
					.header("Authorization", "Bearer " + accessTokenAAA)
					.param("type", "TEXT")
					.param("message", "안녕하세요.22"))
				.andExpect(status().isOk())
				.andDo(print())
				.andReturn();
			
			List<ChatMessageEntity> mlist = chatMessageRepo.findByRoomId(roomId);
			assertEquals("안녕하세요.22", mlist.get(1).getMessage());
		}
		
		@Test
		@DisplayName("이미지 메시지 보내기")
		void sendImageMessage() throws Exception {
			mvc.perform(post("/v1/chat/rooms/"+roomId+"/messages/send")
					.header("Authorization", "Bearer " + accessTokenAAA)
					.param("type", "IMAGE")
					.param("message", "image"))
				.andExpect(status().isOk())
				.andDo(print())
				.andReturn();
			
			List<ChatMessageEntity> mlist = chatMessageRepo.findByRoomId(roomId);
			assertEquals(ChatMessageType.IMAGE, mlist.get(1).getType());
		}
		
		@Test
		@DisplayName("액세스 토큰 없음")
		void noAccessToken() throws Exception{
			ErrorResponse errorResponse = new ErrorResponse(ErrorCode.UNAUTHORIZED);
			String errorResponseJson = objectMapper.writeValueAsString(errorResponse);
			
			mvc.perform(post("/v1/chat/rooms/"+roomId+"/messages/send")
					.param("type", "IMAGE")
					.param("attachUrl", "image"))
				.andExpect(content().json(errorResponseJson))
				.andDo(print())
				.andReturn();
		}
		
		@Test
		@DisplayName("validation - type")
		void validateType() throws Exception {
			mvc.perform(post("/v1/chat/rooms/"+roomId+"/messages/send")
					.header("Authorization", "Bearer " + accessTokenAAA)
					.param("type", "ㅁㅁㅁ")
					.param("attachUrl", "image"))
				.andExpect(jsonPath("$.code", is("INVALID_INPUT_VALUE")))
				.andDo(print())
				.andReturn();
		}
	}
	
	@Nested
	@ActiveProfiles("local")
	@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
	@DisplayName("파일 업로드 테스트")
	class UploadFileTest {
		private String roomId;
		private String memberIdAAA = "aaa";
		private String accessTokenAAA;
		private String memberIdCCC = "ccc";
		private String accessTokenCCC;
		private MockMultipartFile uploadFile;
		
		@BeforeEach
		void setup() throws InterruptedException, ExecutionException, TimeoutException, IOException {
			//업로드 파일 세팅
			FileInputStream image = FileUtils.openInputStream(Paths.get("src/main/resources/static/img/basicProfilePicture.png").toAbsolutePath().toFile());
			uploadFile = new MockMultipartFile("uploadFile", "test2.png", MediaType.IMAGE_PNG_VALUE, image);
			
			//로그인
			AuthDTO.LoginReq loginReq = AuthDTO.LoginReq.builder().memberId(memberIdAAA).password("aaa111").build();
			JwtAuthRes authRes = authService.login(loginReq).getBody();
			accessTokenAAA = authRes.getAccessToken();
			
			MemberEntity member = MemberEntity.builder().memberId(memberIdCCC).name("가나다").password("test").role(Role.ROLE_MEMBER).build();
			CustomUser customUser = new CustomUser(member);
			Claims accessClaims = jwtTokenProvider.generateAccessClaims(UUID.randomUUID().toString(), customUser);
			accessTokenCCC = jwtTokenProvider.generateToken(accessClaims);
			
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
		void uploadFile() throws Exception {
			mvc.perform(multipart("/v1/chat/upload")
					.file(uploadFile)
					.header("Authorization", "Bearer " + accessTokenAAA)
					.param("roomId", roomId))
				.andDo(print());
		}
		
		@Test
		@DisplayName("채팅방이 존재하지 않음")
		void doesNotExistRoom() throws Exception{
			ErrorResponse errorResponse = new ErrorResponse(ErrorCode.INVALID_INPUT_VALUE);
			String errorResponseJson = objectMapper.writeValueAsString(errorResponse);
			
			mvc.perform(multipart("/v1/chat/upload")
					.file(uploadFile)
					.header("Authorization", "Bearer " + accessTokenAAA)
					.param("roomId", "abc"))
				.andExpect(content().json(errorResponseJson))
				.andDo(print());
		}
		
		@Test
		@DisplayName("채팅방에 참가자 존재하지 않음")
		void doesNotExistParticipant() throws Exception{
			ErrorResponse errorResponse = new ErrorResponse(ErrorCode.INVALID_INPUT_VALUE);
			String errorResponseJson = objectMapper.writeValueAsString(errorResponse);
			
			mvc.perform(multipart("/v1/chat/upload")
					.file(uploadFile)
					.header("Authorization", "Bearer " + accessTokenCCC)
					.param("roomId", roomId))
				.andExpect(content().json(errorResponseJson))
				.andDo(print());
		}
	}
	
	@Nested
	@ActiveProfiles("local")
	@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
	@DisplayName("파일 다운로드 테스트")
	class DownloadFileTest {
		private MockMultipartFile uploadFile;
		private String fileName;
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		private String memberIdAAA = "aaa";
		private String roomId = "test1234";
		private String downloadUrl;
		
		@BeforeEach
		void setup() throws IOException {
			//채팅방 생성
			ChatRoomEntity room = ChatRoomEntity.builder()
					.roomId(roomId)
					.title("테스트1234").build();
			chatRoomRepo.save(room);
			
			//참가자 생성
			ChatParticipantPK participantPk = ChatParticipantPK.builder()
					.roomId(roomId)
					.participantId(memberIdAAA).build();
			ChatParticipantEntity participant = ChatParticipantEntity.builder()
					.pk(participantPk)
					.room(room).build();
			chatParticipantRepo.save(participant);
			
			//업로드 파일 세팅
			FileInputStream image = FileUtils.openInputStream(Paths.get("src/main/resources/static/img/basicProfilePicture.png").toAbsolutePath().toFile());
			uploadFile = new MockMultipartFile("uploadFile", "test2.png", MediaType.IMAGE_PNG_VALUE, image);
			
			//파일 업로드
			ChatDTO.UploadReq uploadReq = ChatDTO.UploadReq.builder()
					.roomId(roomId)
					.uploaderId(memberIdAAA)
					.uploadFile(uploadFile).build();
			downloadUrl = chatService.uploadFile(uploadReq).getUrl();
			
			this.makeParamsAndFileName();
		}
		
		private void makeParamsAndFileName() {
			String url = downloadUrl.split("\\?")[0];
			String queries = downloadUrl.split("\\?")[1];
			fileName = url.substring(url.lastIndexOf("/")+1, url.length());
			for(String str : queries.split("&")) {
				params.add(str.split("=")[0], str.split("=")[1]);
			}
		}
		
		@Test
		@DisplayName("파일 다운로드")
		void downloadFile() throws Exception {
			mvc.perform(get("/v1/chat/download/"+fileName)
					.params(params))
				.andExpect(status().isOk())
				.andDo(print())
				.andReturn();
		}
		
		@Test
		@DisplayName("FileName이 다름")
		void doesNotEqualFileName() throws Exception {
			ErrorResponse errorResponse = new ErrorResponse(ErrorCode.INVALID_INPUT_VALUE);
			String errorResponseJson = objectMapper.writeValueAsString(errorResponse);
			
			mvc.perform(get("/v1/chat/download/"+"abc")
					.params(params))
				.andExpect(content().json(errorResponseJson))
				.andDo(print())
				.andReturn();
		}
	}
	
	@Nested
	@ActiveProfiles("local")
	@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
	@DisplayName("채팅방 검색 테스트")
	class SearchRoomsTest{
		private String memberIdAAA = "aaa";
		private String accessTokenAAA;
		private String memberIdCCC = "ccc";
		private int insertCount;
		
		@BeforeEach
		void setup() {
			insertCount = 50;
			
			//액세스 토큰을 생성한다.
			MemberEntity member = MemberEntity.builder().memberId(memberIdAAA).name("가나다").password("aaa111").role(Role.ROLE_MEMBER).build();
			CustomUser customUser = new CustomUser(member);
			Claims accessClaims = jwtTokenProvider.generateAccessClaims(UUID.randomUUID().toString(), customUser);
			accessTokenAAA = jwtTokenProvider.generateToken(accessClaims);
			
			//테스트 데이터를 입력한다.
			//this.insertTestData();
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
		
		//@AfterEach
		void afterSetup() {
			chatRoomRepo.deleteAll();
		}
		
		@Nested
		@ActiveProfiles("local")
		@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
		@DisplayName("내가 참여한 채팅방 검색 테스트")
		class searchRoomByParticipant{
			@Test
			@DisplayName("내가 참여한 채팅방 검색 - 메시지 기준 정렬")
			void searchRoomsByParticipantOrderByMessage() throws Exception {
				mvc.perform(get("/v1/chat/participants/me/rooms")
						.header("Authorization", "Bearer "+ accessTokenAAA)
						.param("searchType", "PARTICIPANT")
						.param("page", "1")
						.param("size", "10")
						.param("sort", "MESSAGE,DESC"))
					.andExpect(jsonPath("$.rooms[0].title", is("aaa"+insertCount--)))
					.andExpect(jsonPath("$.rooms[1].title", is("aaa"+insertCount)))
					.andExpect(jsonPath("$.page", is(1)))
					.andExpect(jsonPath("$.size", is(10)))
					.andDo(print());
			}
			
			@Test
			@DisplayName("내가 참여한 채팅방 검색 - 채팅방 생성일 기준 정렬")
			void searchRoomsByParticipantOrderByCreatedDate() throws Exception {
				mvc.perform(get("/v1/chat/participants/me/rooms")
						.header("Authorization", "Bearer "+ accessTokenAAA)
						.param("searchType", "PARTICIPANT")
						.param("page", "1")
						.param("size", "10")
						.param("sort", "CREATED_DATE,DESC"))
		//			.andExpect(jsonPath("$.rooms[0].title", is(memberIdAAA +insertCount--)))
		//			.andExpect(jsonPath("$.rooms[1].title", is(memberIdAAA +insertCount)))
		//			.andExpect(jsonPath("$.page", is(1)))
		//			.andExpect(jsonPath("$.size", is(10)))
					.andDo(print());
			}
			
			@Test
			@DisplayName("잘못된 searchType")
			void invalidSearchType() throws Exception {
				mvc.perform(get("/v1/chat/participants/me/rooms")
						.header("Authorization", "Bearer "+ accessTokenAAA)
						.param("searchType", "test")
						.param("page", "1")
						.param("size", "10")
						.param("sort", "MESSAGE,DESC"))
					.andExpect(jsonPath("$.code", is("INVALID_INPUT_VALUE")))
					.andDo(print());
			}
			
			@Test
			@DisplayName("잘못된 orderType")
			void doesNotExistPageable() throws Exception {
				ErrorResponse errorResponse = new ErrorResponse(ErrorCode.INVALID_INPUT_VALUE);
				String errorResponseJson = objectMapper.writeValueAsString(errorResponse);
				
				mvc.perform(get("/v1/chat/participants/me/rooms")
						.header("Authorization", "Bearer "+ accessTokenAAA)
						.param("searchType", "PARTICIPANT")
						.param("page", "1")
						.param("size", "10")
						.param("sort", "test,DESC"))
					.andExpect(jsonPath("$.code", is("INVALID_INPUT_VALUE")))
					.andExpect(content().json(errorResponseJson))
					.andDo(print());
			}
		}
		
		@Nested
		@ActiveProfiles("local")
		@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
		@DisplayName("채팅방 검색")
		class searchRoom {
			@Test
			@DisplayName("모든 채팅방 조회")
			void searchALLRoom() throws Exception {
				mvc.perform(get("/v1/chat/rooms")
						.header("Authorization", "Bearer "+ accessTokenAAA)
						.param("page", "1")
						.param("size", "50")
						.param("sort", "MESSAGE,DESC"))
					.andExpect(jsonPath("$.rooms[0].title", is(memberIdAAA +insertCount)))
					.andExpect(jsonPath("$.rooms[1].title", is(memberIdCCC +insertCount)))
					.andDo(print());
			}
			
			@Test
			@DisplayName("제목 기준 검색")
			void searchRoomsByTitle() throws Exception {
				mvc.perform(get("/v1/chat/rooms")
						.header("Authorization", "Bearer "+ accessTokenAAA)
						.param("searchType", "TITLE")
						.param("keyword", "1")
						.param("page", "1")
						.param("size", "10")
						.param("sort", "MESSAGE,DESC"))
					.andExpect(jsonPath("$.rooms[0].title", is(memberIdAAA +41)))
					.andExpect(jsonPath("$.rooms[1].title", is(memberIdCCC +41)))
					.andDo(print());
			}
		}
		
	}
	
	@Nested
	@ActiveProfiles("local")
	@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
	@DisplayName("채팅방 내 참가자 조회 테스트")
	class GetParticipantsByRoom{
		private String memberIdAAA = "aaa";
		private String accessTokenAAA;
		private String memberIdCCC = "ccc";
		private String roomId;
		
		@BeforeEach
		void setup() {
			//액세스 토큰을 생성한다.
			MemberEntity member = MemberEntity.builder().memberId(memberIdAAA).name("가나다").password("aaa111").role(Role.ROLE_MEMBER).build();
			CustomUser customUser = new CustomUser(member);
			Claims accessClaims = jwtTokenProvider.generateAccessClaims(UUID.randomUUID().toString(), customUser);
			accessTokenAAA = jwtTokenProvider.generateToken(accessClaims);
			
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
			mvc.perform(get("/v1/chat/rooms/" + roomId + "/participants")
					.header("Authorization", "Bearer "+ accessTokenAAA))
				.andExpect(jsonPath("$.roomId", is(roomId)))
				.andExpect(jsonPath("$.participants[0].roomId", is(roomId)))
				.andExpect(jsonPath("$.participants[0].participantId", is(memberIdAAA)))
				.andExpect(jsonPath("$.participants[1].participantId", is(memberIdCCC)))
				.andDo(print());
		}
	}
	
	@Nested
	@ActiveProfiles("local")
	@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
	@DisplayName("채팅방 조회 테스트")
	class FindRoomById{
		private String memberIdAAA = "aaa";
		private String accessTokenAAA;
		private String roomId;
		
		@BeforeEach
		void setup() {
			//액세스 토큰을 생성한다.
			MemberEntity member = MemberEntity.builder().memberId(memberIdAAA).name("가나다").password("aaa111").role(Role.ROLE_MEMBER).build();
			CustomUser customUser = new CustomUser(member);
			Claims accessClaims = jwtTokenProvider.generateAccessClaims(UUID.randomUUID().toString(), customUser);
			accessTokenAAA = jwtTokenProvider.generateToken(accessClaims);
			
			//테스트 데이터를 입력한다.
			this.insertTestData();
		}
		
		private void insertTestData() {
			//채팅방 생성
			roomId = UUID.randomUUID().toString();
			ChatRoomEntity room = ChatRoomEntity.builder()
					.roomId(roomId)
					.title("테스트 채팅방 생성").build();
			chatRoomRepo.save(room);
		}
		
		@Test
		@DisplayName("채팅방 조회")
		void findRoomById() throws Exception {
			mvc.perform(get("/v1/chat/rooms/" + roomId)
					.header("Authorization", "Bearer "+ accessTokenAAA))
				.andExpect(jsonPath("$.roomId", is(roomId)))
				.andExpect(jsonPath("$.title", is("테스트 채팅방 생성")))
				.andDo(print());
		}
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
