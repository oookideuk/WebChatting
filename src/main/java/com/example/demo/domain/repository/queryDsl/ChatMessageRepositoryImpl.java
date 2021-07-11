package com.example.demo.domain.repository.queryDsl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.thymeleaf.util.StringUtils;

import com.example.demo.domain.SearchType;
import com.example.demo.domain.entitiy.ChatMessageEntity;
import com.example.demo.domain.entitiy.QChatMessageEntity;
import com.example.demo.domain.entitiy.QChatParticipantEntity;
import com.example.demo.dto.ChatDTO;
import com.querydsl.core.QueryResults;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;

public class ChatMessageRepositoryImpl extends QuerydslRepositorySupport implements ChatMessageRepositoryCustom {
	static QChatMessageEntity message = QChatMessageEntity.chatMessageEntity;
	static QChatParticipantEntity participant = QChatParticipantEntity.chatParticipantEntity;
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	public ChatMessageRepositoryImpl() {
		super(ChatMessageEntity.class);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public List<ChatMessageEntity> getMessagesByRoom(ChatDTO.MessagesReq req){
		List<OrderSpecifier> orders = this.getAllOrderSpecifiers(req);
		
		JPQLQuery subQuery = JPAExpressions
				.select(participant.entryDate)
				.from(participant)
				.where(participant.pk.roomId.eq(req.getRoomId())
						,participant.pk.participantId.eq(req.getParticipantId()));
		
		JPQLQuery query = from(message)
				.leftJoin(participant)
				.on(eqRoom(participant.pk.roomId.toString())
					,eqSender(participant.pk.participantId.toString()))
			.where(
					eqRoom(req.getRoomId())
					//채팅방에 참가한 이후의 메시지 목록을 가져오도록 한다.
					,message.createdDate.goe(subQuery)
					//SearchType.Message 조건을 추가한다.
					,searchCondition(req)
					)
			.orderBy(orders.stream().toArray(OrderSpecifier[]::new));
		
		setLimit(query, req.getSize());
		
		QueryResults<ChatMessageEntity> results = query.fetchResults();
		return results.getResults();
	}
	
	private BooleanExpression eqRoom(String roomId) {
		if(StringUtils.isEmpty(roomId)) {
			return null;
		}
		return message.room.roomId.eq(roomId);
	}
	private BooleanExpression eqSender(String sender) {
		if(StringUtils.isEmpty(sender)) {
			return null;
		}
		return message.sender.eq(sender);
	}
	
	/**
	 * limit를 설정한다.
	 */
	@SuppressWarnings("rawtypes")
	private void  setLimit(JPQLQuery query, Long limit) {
		if(limit != null) {
			query.limit(limit);
		}
	}
	
	/**
	 * 기준 날짜 이상, 이하 메시지를 가져오도록 한다.
	 */
	private BooleanExpression searchCondition(ChatDTO.MessagesReq req) {
		SearchType.Message type = req.getType();
		LocalDateTime date = req.getDate();
		if(type == null || date == null) {
			return null;
		}else if(type.equals(SearchType.Message.GOEDATE)) {
			return message.createdDate.goe(date);
		}else if(type.equals(SearchType.Message.LOEDATE)) {
			return message.createdDate.loe(date);
		}
		return null;
	}
	
	@SuppressWarnings("rawtypes")
	private List<OrderSpecifier> getAllOrderSpecifiers(ChatDTO.MessagesReq req) {
		List<OrderSpecifier> orders = new ArrayList<>();
		SearchType.Message type = req.getType();
		if(type == null) {
			//default는 메시지 생성날짜 기준 내림차순
			orders.add(QueryDslUtil.makeOrderSpecifier(QChatMessageEntity.chatMessageEntity.createdDate, Order.DESC));
		}if(type.equals(SearchType.Message.GOEDATE)) {
			orders.add(QueryDslUtil.makeOrderSpecifier(QChatMessageEntity.chatMessageEntity.createdDate, Order.ASC));
		}else if(type.equals(SearchType.Message.LOEDATE)) {
			orders.add(QueryDslUtil.makeOrderSpecifier(QChatMessageEntity.chatMessageEntity.createdDate, Order.DESC));
		}
		
	    return orders;
	}
}
