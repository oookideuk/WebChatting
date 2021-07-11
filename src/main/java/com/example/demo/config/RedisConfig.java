package com.example.demo.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.example.demo.redis.RedisSubscriber;

@Configuration
public class RedisConfig {
	public static final String CHAT_ROOM_MESSAGE = "CHAT_ROOM_MESSAGE";
	public static final String CHAT_ROOM_DISCONNECT = "CHAT_ROOM_DISCONNECT";
	/**
	 * Topic 사용을 위한 Bean 설정
	 */
	@Bean
	public Map<String, ChannelTopic> channelTopics(){
		Map<String, ChannelTopic> channelTopics = new HashMap<>();
		channelTopics.put(CHAT_ROOM_MESSAGE, new ChannelTopic(CHAT_ROOM_MESSAGE));
		channelTopics.put(CHAT_ROOM_DISCONNECT, new ChannelTopic(CHAT_ROOM_DISCONNECT));
		return channelTopics;
	}
	
	/**
	 * redis에 발행(publish)된 메시지를 처리하는 listener 설정한다.
	 * 기본적으로 LettuceConnectionFactory 사용한다.
	 */
	@Bean
	public RedisMessageListenerContainer redisMessageListener(RedisConnectionFactory connectionFactory
			, MessageListenerAdapter listenerAdapter
			, @Qualifier("DisconnectChatRoomListenerAdapter") MessageListenerAdapter WebSocketSessionCloseListenerAdapter
			, Map<String, ChannelTopic> channelTopics) {		
		RedisMessageListenerContainer listenerContainer = new RedisMessageListenerContainer();
		listenerContainer.setConnectionFactory(connectionFactory);
		
		listenerContainer.addMessageListener(listenerAdapter, channelTopics.get(CHAT_ROOM_MESSAGE));
		listenerContainer.addMessageListener(WebSocketSessionCloseListenerAdapter, channelTopics.get(CHAT_ROOM_DISCONNECT));
		
		return listenerContainer;
	}
	
	/**
	 * 실제 메시지를 처리하는 subscriber 설정 추가
	 */
	@Bean
	public MessageListenerAdapter listenerAdapter(RedisSubscriber subscriber) {
		return new MessageListenerAdapter(subscriber, "sendChatRoomMessage");
	}
	@Bean(name= "DisconnectChatRoomListenerAdapter")
	public MessageListenerAdapter WebSocketSessionCloseListenerAdapter(RedisSubscriber subscriber) {
		return new MessageListenerAdapter(subscriber, "disconnectChatRoom");
	}
	
	/**
	 * 어플리케이션에서 사용할 redisTemplate 설정
	 */
	@Bean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory){
		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(connectionFactory);
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(String.class));
		return redisTemplate;
	}
}
