package com.example.demo.service.impl;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.EntityNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import com.example.demo.common.FileUtil;
import com.example.demo.common.UploadFile;
import com.example.demo.config.RedisConfig;
import com.example.demo.domain.ChatMessageType;
import com.example.demo.domain.SearchType;
import com.example.demo.domain.entitiy.ChatFileEntity;
import com.example.demo.domain.entitiy.ChatMessageEntity;
import com.example.demo.domain.entitiy.ChatOnlineParticipantEntity;
import com.example.demo.domain.entitiy.ChatOnlineParticipantPK;
import com.example.demo.domain.entitiy.ChatParticipantEntity;
import com.example.demo.domain.entitiy.ChatParticipantPK;
import com.example.demo.domain.entitiy.ChatRoomEntity;
import com.example.demo.domain.entitiy.MemberEntity;
import com.example.demo.domain.repository.ChatFileRepository;
import com.example.demo.domain.repository.ChatMessageRepository;
import com.example.demo.domain.repository.ChatOnlineParticipantRepository;
import com.example.demo.domain.repository.ChatParticipantRepository;
import com.example.demo.domain.repository.ChatRoomRepository;
import com.example.demo.domain.repository.MemberRepository;
import com.example.demo.dto.ChatDTO;
import com.example.demo.dto.ChatDTO.Participant;
import com.example.demo.dto.DownLoadUrl;
import com.example.demo.exception.ProfilePictureNotFoundException;
import com.example.demo.service.ChatService;
import com.example.demo.service.ProfilePictureService;

@Service
public class ChatServiceImpl implements ChatService{
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();	// 웹소켓 세션
	private RedisTemplate<String, Object> redisTemplate;
	private Map<String, ChannelTopic> channelTopics;
	private ChatRoomRepository chatRoomRepository;
	private ChatParticipantRepository chatParticipantRepository;
	private ChatOnlineParticipantRepository chatOnlineParticipantRepository;
	private ChatMessageRepository chatMessageRepo;
	private ChatFileRepository chatFileRepo;
	private MemberRepository memberRepo;
	private ProfilePictureService profilePictureService;
	
	public ChatServiceImpl(RedisTemplate<String, Object> redisTemplate
			, Map<String, ChannelTopic> channelTopics
			, ChatRoomRepository chatRoomRepository, ChatParticipantRepository chatParticipantRepository
			, ChatOnlineParticipantRepository chatOnlineParticipantRepository
			, ChatMessageRepository chatMessageRepo
			, ChatFileRepository chatFileRepo
			, MemberRepository memberRepo
			, ProfilePictureService profilePictureService) {
		this.redisTemplate = redisTemplate;
		this.chatRoomRepository = chatRoomRepository;
		this.chatParticipantRepository = chatParticipantRepository;
		this.channelTopics = channelTopics;
		this.chatOnlineParticipantRepository = chatOnlineParticipantRepository;
		this.chatMessageRepo = chatMessageRepo;
		this.chatFileRepo = chatFileRepo;
		this.memberRepo = memberRepo;
		this.profilePictureService = profilePictureService;
	}
	
	@Transactional(rollbackFor = Exception.class)
	@Override
	public ChatDTO.CreateRes createRoom(ChatDTO.CreateReq createReq) {
		ChatRoomEntity chatRoomEntity = createReq.toEntity();
		
		chatRoomRepository.save(chatRoomEntity);
		ChatDTO.CreateRes createRes = ChatDTO.CreateRes.of(chatRoomEntity);
		
		return createRes;
	}
	
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void enterRoom(ChatDTO.EnterReq enterReq) {
		//채팅방이 있는지 확인한다.
		if(!this.existsRoom(enterReq.getRoomId())) {
			throw new EntityNotFoundException("채팅방 없음 roomId : " + enterReq.getRoomId());
		}
		//채팅방에 처음 입장하는지 확인한다.
		if(!this.existsParticipant(enterReq.getRoomId(), enterReq.getParticipantId())) {
			//참가자를 저장한다.
			this.saveParticipant(enterReq);
			//입장 메시지를 전달한다.
			this.sendEnterRoomMessage(enterReq);
		}
		//온라인 참가자를 저장한다.
		this.saveOnlineParticipant(enterReq);
	}
	
	/**
	 * 채팅방이 존재하는지 확인한다.
	 */
	private boolean existsRoom(String roomId) {
		return chatRoomRepository.existsById(roomId);
	}
	
	@Override
	public boolean existsParticipant(String roomId, String participantId) {
		ChatParticipantPK chatParticipantPK = ChatParticipantPK.builder()
				.roomId(roomId)
				.participantId(participantId)
				.build();
		return chatParticipantRepository.existsById(chatParticipantPK);
	}
	
	/**
	 * 채팅방 입장 메시지를 전송한다.
	 */
	private void sendEnterRoomMessage(ChatDTO.EnterReq enterReq) {
		ChatDTO.MessageReq messageReq = ChatDTO.MessageReq.builder()
				.roomId(enterReq.getRoomId())
				.sender(enterReq.getParticipantId())
				.name("[알림]")
				.type(ChatMessageType.ENTER)
				.message(enterReq.getParticipantName() + "님이 입장했습니다.")
				.build();
		this.saveAndSendMessage(messageReq);
	}
	
	/**
	 * 채팅방 참가자를 저장한다.
	 */
	@Transactional(rollbackFor = Exception.class)
	private void saveParticipant(ChatDTO.EnterReq enterReq) {
		String roomId = enterReq.getRoomId();
		String participantId = enterReq.getParticipantId();
		
		ChatParticipantPK participantPk = ChatParticipantPK.builder()
				.roomId(roomId)
				.participantId(participantId)
				.build();
		ChatParticipantEntity participant = ChatParticipantEntity.builder()
				.pk(participantPk)
				.room(ChatRoomEntity.builder().roomId(roomId).build())
				.build();
		
		chatParticipantRepository.save(participant);
	}
	
	/**
	 * online participant 정보를 저장한다.
	 */
	@Transactional(rollbackFor = Exception.class)
	private void saveOnlineParticipant(ChatDTO.EnterReq enterReq) {
		ChatOnlineParticipantPK pk = ChatOnlineParticipantPK.builder()
				.sessionId(enterReq.getSessionId())
				.hostAddress(enterReq.getHostAddress())
				.port(enterReq.getPort())
				.build();
		ChatOnlineParticipantEntity onlineParticipant = ChatOnlineParticipantEntity.builder()
				.pk(pk)
				.room(ChatRoomEntity.builder().roomId(enterReq.getRoomId()).build())
				.participantId(enterReq.getParticipantId())
				.build();
		chatOnlineParticipantRepository.save(onlineParticipant);
	}
	
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void leaveRoom(ChatDTO.LeaveReq leaveReq) {
		//채팅방이 존재하는지 확인한다.
		if(!this.existsRoom(leaveReq.getRoomId())) {
			throw new EntityNotFoundException("채팅방 없음 roomId : " + leaveReq.getRoomId());
		}
		//채팅방에 참가자가 있는지 확인한다.
		if(!this.existsParticipant(leaveReq.getRoomId(), leaveReq.getParticipantId())) {
			throw new EntityNotFoundException("참가자 없음 roomId : " + leaveReq.getRoomId() + "participantId : " + leaveReq.getParticipantId());
		}
		
		//중복 접속 중인 online participant의 session 종료 메시지를 전송한다.
		List<ChatDTO.WebSocketSessionCloseReq> sessionCloseReqs = new ArrayList<>();
		List<ChatOnlineParticipantEntity> onlineParticipants = chatOnlineParticipantRepository.findByRoomIdAndParticipantId(leaveReq.getRoomId(), leaveReq.getParticipantId());
		for(ChatOnlineParticipantEntity onlineParticipant : onlineParticipants) {
			ChatDTO.WebSocketSessionCloseReq sessionCloseReq = ChatDTO.WebSocketSessionCloseReq.builder()
					.sessionId(onlineParticipant.getPk().getSessionId())
					.hostAddress(onlineParticipant.getPk().getHostAddress())
					.port(onlineParticipant.getPk().getPort())
					.build();
			sessionCloseReqs.add(sessionCloseReq);
		}
		redisTemplate.convertAndSend(channelTopics.get(RedisConfig.CHAT_ROOM_DISCONNECT).getTopic(), sessionCloseReqs);
		
		ChatRoomEntity room = chatRoomRepository.findById(leaveReq.getRoomId()).get();
		//채팅방 참가자 수가 1 이하면 채팅방 삭제한다.
		if(room.getParticipants().size() <= 1) {
			//채팅방을 삭제한다. cascade로 관련 DB 데이터 같이 삭제함.
			this.deleteRoom(room);
		}else {
			//나가기 메시지를 전달한다.
			this.sendLeaveMessage(leaveReq);
			//참가자를 삭제한다.
			ChatParticipantPK participantPk = ChatParticipantPK.builder().roomId(leaveReq.getRoomId()).participantId(leaveReq.getParticipantId()).build();
			chatParticipantRepository.deleteById(participantPk);		
		}
	}
	
	/**
	 * 채팅방을 삭제한다.
	 */
	private void deleteRoom(ChatRoomEntity room) {
		//TODO 서버에서 파일 삭제하기
		List<ChatFileEntity> files = room.getChatFiles();
		for(ChatFileEntity file : files) {
			logger.debug("file [{}]", file);
		}
		
		//채팅방 정보를 삭제한다.
		chatRoomRepository.delete(room);
	}
	
	/**
	 * 나가기 메시지를 전달한다.
	 */
	private void sendLeaveMessage(ChatDTO.LeaveReq leaveReq) {
		ChatDTO.MessageReq messageReq = ChatDTO.MessageReq.builder()
				.roomId(leaveReq.getRoomId())
				.sender(leaveReq.getParticipantId())
				.name("[알림]")
				.type(ChatMessageType.LEAVE)
				.message(leaveReq.getParticipantName() + "님이 나가셨습니다.")
				.build();
		this.saveAndSendMessage(messageReq);
	}
	
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteOnlineParticipant(String sessionId, String hostAddress, int port) {
		ChatOnlineParticipantPK pk = ChatOnlineParticipantPK.builder()
				.sessionId(sessionId)
				.hostAddress(hostAddress)
				.port(port).build();
		
		if(chatOnlineParticipantRepository.existsById(pk)) {
			chatOnlineParticipantRepository.deleteById(pk);
		}
	}
	
	@Override
	@Transactional(rollbackFor = Exception.class)
	public ChatMessageEntity saveAndSendMessage(ChatDTO.MessageReq messageReq) {
		//채팅방에 참가자가 있는지 확인한다.
		if(!this.existsParticipant(messageReq.getRoomId(), messageReq.getSender())) {
			throw new EntityNotFoundException("참가자 없음 roomId : " + messageReq.getRoomId() + "participantId : " + messageReq.getSender());
		}
		
		//메시지 정보를 저저장한다.
		ChatMessageEntity message = this.saveMessage(messageReq);
		//binary 타입 메시지의 경우 attach 정보를 저장한다.
		if(ChatMessageType.isBinaryTypeMessage(messageReq.getType())) {
			//message로 파일 url 정보가 들어온다.
			logger.debug("+++++++++++++++++++++++++++++++++++++");
			logger.debug("messageReq [{}]", messageReq);
			messageReq.setAttachUrl(messageReq.getMessage());
			this.saveMessageAttach(messageReq, message);
		}
		
		messageReq.setMessageId(message.getMessageId());
		messageReq.setCreatedDate(message.getCreatedDate());
		//메시지를 전송한다.
		this.sendMessage(messageReq);
		
		return message;
	}
		
	/**
	 * message를 저장한다.
	 */
	@Transactional(rollbackFor = Exception.class)
	private ChatMessageEntity saveMessage(ChatDTO.MessageReq messageReq) {
		ChatMessageEntity message = messageReq.toChatMessageEntity();
		return chatMessageRepo.save(message);
	}
	
	/**
	 * message attach를 저장한다.
	 */
	@Transactional(rollbackFor = Exception.class)
	private void saveMessageAttach(ChatDTO.MessageReq messageReq, ChatMessageEntity message) {
		message.setMessageAttach(messageReq.getAttachUrl());
	}
	
	/**
	 * 메시지를 전송한다.
	 */
	private void sendMessage(ChatDTO.MessageReq messageReq) {
		ChatDTO.MessageRes messageRes = messageReq.toMessageRes();
		redisTemplate.convertAndSend(channelTopics.get(RedisConfig.CHAT_ROOM_MESSAGE).getTopic(), messageRes);
	}
	
	@Override
	public void sendSubscribeMessage(String roomId, String participantId) {
		ChatDTO.MessageReq messageReq = ChatDTO.MessageReq.builder()
				.roomId(roomId)
				.sender(participantId)
				.type(ChatMessageType.SUBSCRIBE).build();
		sendMessage(messageReq);
	}
	
	@Override
	@Transactional(rollbackFor = Exception.class)
	public ChatDTO.UploadRes uploadFile(ChatDTO.UploadReq uploadReq) throws IOException {
		logger.debug("uploadReq[{}]", uploadReq);
		//채팅방 있는지 확인한다.
		if(!this.existsRoom(uploadReq.getRoomId())) {
			throw new EntityNotFoundException("채팅방 없음 roomId : " + uploadReq.getRoomId());
		}
		//채팅방에 참가자 있는지 확인한다.
		if(!this.existsParticipant(uploadReq.getRoomId(), uploadReq.getUploaderId())) {
			throw new EntityNotFoundException("참가자 없음 roomId : " + uploadReq.getRoomId() + "participantId : " + uploadReq.getUploaderId());
		}
		//업로드 파일을 저장한다.
		ChatFileEntity fileEntity = this.saveUploadFile(uploadReq);
		
		String downloadUrl = DownLoadUrl.makeChatFileUrl(fileEntity.getOriginalFileName(), fileEntity.getFileId());
		return ChatDTO.UploadRes.builder()
				.url(downloadUrl).build();
	}
	
	/**
	 * 파일을 DB및 서버에 저장한다. 
	 * @throws IOException 
	 */
	@Transactional(rollbackFor = Exception.class)
	private ChatFileEntity saveUploadFile(ChatDTO.UploadReq uploadReq) throws IOException {
		//파일 정보를 세팅한다.
		UploadFile uploadFile = new UploadFile(uploadReq.getUploadFile());
		uploadFile.setFileInformation();
		
		ChatFileEntity fileEntity = ChatFileEntity.builder()
				.type(uploadFile.getType())
				.storedFileName(uploadFile.getStoredFileName())
				.originalFileName(uploadFile.getOriginalFileName())
				.size(uploadFile.getSize())
				.uploadPath(uploadFile.getUploadPath().toString())
				.room(ChatRoomEntity.builder().roomId(uploadReq.getRoomId()).build())
				.participantId(uploadReq.getUploaderId()).build();
				
		//파일 정보를 DB에 저장한다.
		fileEntity = chatFileRepo.save(fileEntity);
		//파일을 서버에 저장한다.
		Path uploadPath = uploadFile.getUploadPath();
		FileUtil.makeDir(uploadPath);	// 디렉토리를 생성한다.
		FileUtil.saveFile(uploadFile);	// 파일을 저장한다.
		
		return fileEntity;
	}
	
	@Override
	@Transactional(rollbackFor = Exception.class)
	public byte[] downloadFile(ChatDTO.DownloadReq downloadReq, HttpHeaders headers) throws IOException {
		ChatFileEntity fileEntity = chatFileRepo.findById(downloadReq.getId()).orElseThrow(() -> new EntityNotFoundException("ChatFileEntity with fileId : " + downloadReq.getId()));
		//fileName과 originalFileName이 같은지 확인한다.
		if(!downloadReq.getFileName().equals(fileEntity.getOriginalFileName())) {
			throw new EntityNotFoundException("fileName : "+downloadReq.getFileName());
		}
		
		//header를 설정한다.
		if(FileUtil.isImage(fileEntity.getType())) {
			FileUtil.setHeadersForImage(headers, downloadReq.getFileName());
		}else {
			FileUtil.setHeadersForDownload(headers, downloadReq.getFileName());
		}
		return FileUtil.fileToByteArray(Paths.get(fileEntity.getUploadPath(), fileEntity.getStoredFileName()));
	}
	
	@Override
	@Transactional(rollbackFor = Exception.class)
	public ChatDTO.RoomSearchRes searchRooms(ChatDTO.RoomSearchReq searchReq, Pageable pageable){
		Page<ChatRoomEntity> searchedRooms = chatRoomRepository.search(searchReq, pageable);
		
		List<ChatDTO.Room> rooms = new ArrayList<>();
		for(ChatRoomEntity r : searchedRooms) {
			ChatDTO.Room room = ChatDTO.Room.of(r);
			//마지막 메시지 관련 정보를 추가한다.
			room.addLastMessageInfo(r.getMessages());
			
			//참가한 채팅 방 리스트 요청시 읽지 않은 메시지 개수를 추가한다.
			if(searchReq.getSearchType().equals(SearchType.Room.PARTICIPANT)) {
				//TODO offlineDate -> 클라이언트 활성화 여부로 수정하기
				//현재 채팅 방에 온라인 상태인지 확인한다.
				if(this.isParticipantOnline(room.getRoomId(), searchReq.getParticipantId())) {
					//온라인인 경우 읽지 않은 메시지는 0개.
					room.setUnreadMessageCount(0);
				
				//오프라인인 경우 읽지 않은 메시지 개수 계산.
				}else {
					//(오프라인 시간 < 메시지 도착 시간) 개수로 세팅한다.
					ChatParticipantPK participantPK = ChatParticipantPK.builder()
														.roomId(room.getRoomId())
														.participantId(searchReq.getParticipantId()).build();
					LocalDateTime offlineDate = chatParticipantRepository.getOne(participantPK).getOfflineDate();
					int unreadMessageCount = chatMessageRepo.countByRoomIdAndcreatedDate(room.getRoomId(), offlineDate);
					room.setUnreadMessageCount(unreadMessageCount);
					
					// TODO 마지막으로 읽은 메시지 인덱스 구하기로 시도해보기.
					// - r.getMessages().indexOf()로 구하면 이중 for문 ......
					// - createdDate 값이 같은 마지막으로 읽은 메시지가 여러개라면?
					//		정렬 기준 값(createdDate)이 같은 것 끼리는 pk를 기준으로 정렬된다. 
					//		마지막으로 읽은 메시지는 createdDate 기준 내림차순 했을때 가장 첫번째 메시지를 가져온다. 
					//		roomEntity에서 messages는 createdDate 기준으로 정렬후 가져온다.
					//		messages에서 마지막으로 읽은 메시지를 indexOf로 찾으면 앞에서부터 찾기때문에 createdDate가 같은 마지막으로 읽은 메시지가 여러개라도 상관없을듯.
				}
				
			}
			rooms.add(room);
		}
		
		return ChatDTO.RoomSearchRes.builder()
				.rooms(rooms)
				.page(searchedRooms.getNumber()+1)
				.size(searchedRooms.getSize())
				.totlaPage(searchedRooms.getTotalPages()).build();
	}
	
	/**
	 * 채팅 참가자가 온라인 상태인지 확인한다. 
	 */
	private boolean isParticipantOnline(String roomId, String participantId) {
		List<ChatOnlineParticipantEntity> onlineParticipants = chatOnlineParticipantRepository.findByRoomIdAndParticipantId(roomId, participantId);
		if(onlineParticipants.size() > 0) return true;
		else return false;
	}
	
	// 현재 : 오프라인 시간으로 파악. TODO : 클라이언트 활성화 여부로 파악하기.
	// WS disconnect할때 오프라인 시간을 업데이트 하는데 클라이언트가 비활성화 될때마다 WS 연결 끊기는 부담스러움. ex) 브라우저 탭 전환, ALT + TAB ...
	// 현재는 클라이언트 비활성화되도 WS disconnect 안함.
	@Override
	@Transactional(rollbackFor = Exception.class)
	public ChatDTO.MessagesRes getLastReadMessage(String roomId, String participantId){
		//채팅방 있는지 확인한다.
		if(!this.existsRoom(roomId)) {
			throw new EntityNotFoundException("채팅방 없음 roomId : " + roomId);
		}
		//채팅방에 참가자 있는지 확인한다.
		if(!this.existsParticipant(roomId, participantId)) {
			throw new EntityNotFoundException("참가자 없음 roomId : " + roomId + " participantId : " + participantId);
		} 
		//비활성화 시간을 가져온다.
		ChatParticipantPK pk = ChatParticipantPK.builder()
				.roomId(roomId)
				.participantId(participantId).build();
		ChatParticipantEntity participant = chatParticipantRepository.getOne(pk);
		LocalDateTime inactivationDate = participant.getOfflineDate();
		//inactivationDate, LOE, size==1 메시지 가져온다.
		ChatDTO.MessagesReq messagesReq = ChatDTO.MessagesReq.builder()
				.roomId(roomId)
				.participantId(participantId)
				.date(inactivationDate)
				.type(SearchType.Message.LOEDATE)
				.size(1L)
				.build();
		ChatDTO.MessagesRes messagesRes = getMessagesByRoom(messagesReq);
		
		return messagesRes;
	}
	
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateRoomOfflineDate(String sessionId, String hostAddress, int port) {
		//participantId와 roomId 정보를 가져온다.
		Optional<ChatOnlineParticipantEntity> onlineParticipant = this.getOnlineParticipant(sessionId, hostAddress, port);
		if(onlineParticipant.isPresent()) {
			onlineParticipant.get().getRoom().getRoomId();
			onlineParticipant.get().getParticipantId();
			
			//오프라인 시간을 업데이트 한다.
			ChatParticipantPK participantPk = ChatParticipantPK.builder()
					.roomId(onlineParticipant.get().getRoom().getRoomId())
					.participantId(onlineParticipant.get().getParticipantId()).build();
			chatParticipantRepository.getOne(participantPk).updateOfflineDate(LocalDateTime.now());
		}
	}
	
	/**
	 * 채팅 방 온라인 참가자 정보를 가져온다.
	 */
	@Transactional(rollbackFor = Exception.class)
	private Optional<ChatOnlineParticipantEntity> getOnlineParticipant(String sessionId, String hostAddress, int port) {
		ChatOnlineParticipantPK pk = ChatOnlineParticipantPK.builder()
				.sessionId(sessionId)
				.hostAddress(hostAddress)
				.port(port).build();
		return chatOnlineParticipantRepository.findById(pk);
	}
	
	@Override
	@Transactional(rollbackFor = Exception.class)
	public ChatDTO.MessagesRes getMessagesByRoom(ChatDTO.MessagesReq req){
		//채팅방이 존재하는지 확인한다.
		if(!this.existsRoom(req.getRoomId())) {
			throw new EntityNotFoundException("채팅방 없음 roomId : " + req.getRoomId());
		}
		//채팅방에 참가자가 있는지 확인한다.
		if(!this.existsParticipant(req.getRoomId(), req.getParticipantId())) {
			throw new EntityNotFoundException("참가자 없음 roomId : " + req.getRoomId() + "participantId : " + req.getParticipantId());
		}
				
		List<ChatMessageEntity> searchedMessages = chatMessageRepo.getMessagesByRoom(req);
		
		//메시지 name 설정을 위해 회원 이름을 가져온다.
		HashMap<String, String> participantNamesMap = this.getParticipantNamesByMessage(searchedMessages);
		//응답 메시지를 생성한다.
		List<ChatDTO.MessageRes> messages = new ArrayList<>();
		for(ChatMessageEntity m : searchedMessages) {
			String attachUrl = null;
			if(ChatMessageType.isBinaryTypeMessage(m.getType())) {
				attachUrl = m.getAttachs().get(0).getUrl();
			}
			ChatDTO.MessageRes res = ChatDTO.MessageRes.builder()
										.roomId(req.getRoomId())
										.messageId(m.getMessageId())
										.type(m.getType())
										.sender(m.getSender())
										.name(participantNamesMap.get(m.getSender()))
										.message(m.getMessage())
										.attachUrl(attachUrl)
										.createdDate(m.getCreatedDate()).build();
			messages.add(res);
		}
		
		return ChatDTO.MessagesRes.builder()
				.roomId(req.getRoomId())
				.messages(messages).build();
	}
	
	/**
	 * 회원이름을 가져온다.
	 */
	private HashMap<String, String> getParticipantNamesByMessage(List<ChatMessageEntity> messages){
		Set<String> participantNamesSet = new HashSet<>();
		messages.forEach(m -> participantNamesSet.add(m.getSender()));
		
		HashMap<String, String> participantNamesMap = new HashMap<>();
		List<MemberEntity> members = memberRepo.findAllById(participantNamesSet);
		members.forEach( m -> participantNamesMap.put(m.getMemberId(), m.getName())  );
		
		return participantNamesMap;
	}
	
	@Override
	@Transactional(rollbackFor = Exception.class)
	public ChatDTO.ParticipantsRes getParticipantsByRoom(ChatDTO.ParticipantsReq req){
		//채팅방이 존재하는지 확인한다.
		if(!this.existsRoom(req.getRoomId())) {
			throw new EntityNotFoundException("채팅방 없음 roomId : " + req.getRoomId());
		}
		//채팅방에 입장한 상태인지 확인한다.
		if(!this.existsParticipant(req.getRoomId(), req.getParticipantId())) {
			throw new EntityNotFoundException("참가자 없음 roomId : " + req.getRoomId() + "participantId : " + req.getParticipantId());
		}
		
		String profilePictureUrl;
		ChatRoomEntity room = chatRoomRepository.getOne(req.getRoomId());
		HashMap<String, String> participantNamesMap = getParticipantNamesByParticipant(room.getParticipants());
		
		List<ChatDTO.Participant> participants = new ArrayList<>();
		for(ChatParticipantEntity p : room.getParticipants()) {
			
			try {
				//프로필 사진을 등록한다.
				profilePictureUrl = profilePictureService.findProfilePictureByMemberId(p.getPk().getParticipantId()).getSrc();
			} catch (ProfilePictureNotFoundException e) {
				//등록된 프로필 사진이 없는 경우.	 
				profilePictureUrl = "";
			} catch(Exception e) {
				profilePictureUrl = "";
			}
			ChatDTO.Participant participant = Participant.of(p, participantNamesMap.get(p.getPk().getParticipantId()), profilePictureUrl);
			participants.add(participant);
		}
		
		return ChatDTO.ParticipantsRes.builder()
				.roomId(req.getRoomId())
				.participants(participants).build();
	}
	
	/**
	 * 회원이름을 가져온다.
	 */
	private HashMap<String, String> getParticipantNamesByParticipant(List<ChatParticipantEntity> participants){
		Set<String> participantNamesSet = new HashSet<>();
		participants.forEach( p ->  participantNamesSet.add(p.getPk().getParticipantId()) );
		
		HashMap<String, String> participantNamesMap = new HashMap<>();
		List<MemberEntity> members = memberRepo.findAllById(participantNamesSet);
		members.forEach( m -> participantNamesMap.put(m.getMemberId(), m.getName())  );
		
		return participantNamesMap;
	}
	
	@Override
	public ChatDTO.Room findRoomById(String roomId) {
		ChatRoomEntity room = chatRoomRepository.findById(roomId).orElseThrow( () -> new EntityNotFoundException("채팅방 없음 roomId : " + roomId) );
		
		return ChatDTO.Room.of(room);
	}
	
	@Override
	public String getRoomIdFromDestination(String destination) {
        int lastIndex = destination.lastIndexOf('/');
        if (lastIndex != -1)
            return destination.substring(lastIndex + 1);
        else
            return "";
    }
		
	@Override
	public void addWebSocketSession(WebSocketSession session) {
		sessions.put(session.getId(), session);
	}
	
	@Override
	public void closeWebSocketSession(String sessionId) {
		this.closeWebSocketSession(sessionId, CloseStatus.NORMAL);
	}
	
	@Override
	public void closeWebSocketSession(String sessionId, CloseStatus status) {
		WebSocketSession session = sessions.get(sessionId);
		logger.debug("session[!!!{}]", session);
		logger.debug("before sessions size[{}]", sessions.size());
		if(session != null && session.isOpen()) {
			try {
				session.close(status);
			} catch (Exception e) {
				logger.error("Error while closing websocket session sessionId[{}] exception[{}]",sessionId, e);
			}
		}
		sessions.remove(sessionId);
		logger.debug("after sessions size[{}]", sessions.size());
	}
	
	@Override
	public boolean existsWebSocketSession(String sessionId) {
		WebSocketSession session = sessions.get(sessionId);
		if(session != null) return true;
		else return false;
	}
}
