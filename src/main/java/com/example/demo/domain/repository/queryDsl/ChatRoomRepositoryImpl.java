package com.example.demo.domain.repository.queryDsl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.util.StringUtils;

import com.example.demo.domain.OrderType;
import com.example.demo.domain.SearchType;
import com.example.demo.domain.entitiy.ChatRoomEntity;
import com.example.demo.domain.entitiy.QChatMessageEntity;
import com.example.demo.domain.entitiy.QChatParticipantEntity;
import com.example.demo.domain.entitiy.QChatRoomEntity;
import com.example.demo.dto.ChatDTO;
import com.querydsl.core.QueryResults;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPQLQuery;

public class ChatRoomRepositoryImpl extends QuerydslRepositorySupport implements ChatRoomRepositoryCustom {
	static QChatRoomEntity room = QChatRoomEntity.chatRoomEntity;
	static QChatParticipantEntity participant = QChatParticipantEntity.chatParticipantEntity;
	static QChatMessageEntity message = QChatMessageEntity.chatMessageEntity;
	
	public ChatRoomRepositoryImpl() {
		super(ChatRoomEntity.class);
	}
	
	@Override
	public Page<ChatRoomEntity> search(ChatDTO.RoomSearchReq searchReq, Pageable pageable){
		JPQLQuery<ChatRoomEntity> query = from(room)
				.leftJoin(room.participants, participant)
				.leftJoin(room.messages, message)
				.where(
						eqParticipant(searchReq)
						, containsTitle(searchReq))
				.groupBy(room.roomId);

		//paging, sort 설정
		QueryResults<ChatRoomEntity> results = this.setPagingAndOrder(query, pageable)
													.fetchResults();
		return new PageImpl<>(results.getResults(), pageable, results.getTotal());
	}
	
	private BooleanExpression eqParticipant(ChatDTO.RoomSearchReq req) {
		if(req.getSearchType() == null || !SearchType.Room.PARTICIPANT.equals(req.getSearchType())) {
			return null;
		}
		if(req.getParticipantId() == null) {
			return null;
		}
		return participant.pk.participantId.eq(req.getParticipantId());
	}
	
	private BooleanExpression containsTitle(ChatDTO.RoomSearchReq req) {
		if(req.getSearchType() == null || !SearchType.Room.TITLE.equals(req.getSearchType())) {
			return null;
		}
		if(StringUtils.isEmpty(req.getKeyword())) {
			return null;
		}
		return room.title.contains(req.getKeyword());
	}
	
	/**
	 * 페이징과 정렬을 설정한다.
	 */
	@SuppressWarnings("rawtypes")
	private JPQLQuery<ChatRoomEntity> setPagingAndOrder(JPQLQuery<ChatRoomEntity> query, Pageable pageable){
		List<OrderSpecifier> orders = this.getAllOrderSpecifiers(pageable);
		return query
				.orderBy(orders.stream().toArray(OrderSpecifier[]::new))
				.offset(pageable.getOffset())
				.limit(pageable.getPageSize());
	}
	@SuppressWarnings("rawtypes")
	private List<OrderSpecifier> getAllOrderSpecifiers(Pageable pageable) {
		List<OrderSpecifier> orders = new ArrayList<>();
		for (Sort.Order order : pageable.getSort()) {
			OrderSpecifier<?> orderSpecifier = null;
			switch (OrderType.Room.valueOf(order.getProperty())) {
				case CREATED_DATE:
					orderSpecifier = QueryDslUtil.makeOrderSpecifier(QChatRoomEntity.chatRoomEntity.createdDate, order);
					orders.add(orderSpecifier);
					break;
				case MESSAGE:
					orderSpecifier = QueryDslUtil.makeOrderSpecifier(QChatMessageEntity.chatMessageEntity.createdDate.max(), order);
					orders.add(orderSpecifier);
					break;
				case TITLE:
					orderSpecifier = QueryDslUtil.makeOrderSpecifier(QChatRoomEntity.chatRoomEntity.title, order);
					orders.add(orderSpecifier);
					break;
				default:
					break;
			}
		}
	    return orders;
	}
}
