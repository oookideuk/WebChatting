package com.example.demo.domain.repository.queryDsl;

import org.springframework.data.domain.Sort;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;

public class QueryDslUtil {
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static OrderSpecifier<?> makeOrderSpecifier(Expression<?> expression, Sort.Order order) {
		Order orderExpression = order.isAscending() ? Order.ASC : Order.DESC;
        return new OrderSpecifier(orderExpression, expression);
    }
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static OrderSpecifier<?> makeOrderSpecifier(Expression<?> expression, Order order){
		return new OrderSpecifier(order, expression);
	}
}
