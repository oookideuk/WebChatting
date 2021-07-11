package com.example.demo.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurationSupport;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

import com.example.demo.chat.handler.CustomStompSubProtocolErrorHandler;
import com.example.demo.chat.handler.CustomSubProtocolWebSocketHandler;
import com.example.demo.chat.handler.StompChannelInterceptor;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig extends WebSocketMessageBrokerConfigurationSupport implements WebSocketMessageBrokerConfigurer {
	private StompChannelInterceptor stompChannelInterceptor;
	private	StompSubProtocolErrorHandler stompSubProtocolErrorHandler; 
	
	public WebSocketConfig(StompChannelInterceptor stompChannelInterceptor
			, StompSubProtocolErrorHandler stompSubProtocolErrorHandler) {
		this.stompChannelInterceptor = stompChannelInterceptor;
		this.stompSubProtocolErrorHandler = stompSubProtocolErrorHandler;
	}
	
	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		/*
		 * WebSocket 접속 위한 EndPoint 설정
		 * 도메인이 다른 서버에서도 접속 가능하도록 CORS 설정
		 * withSockJS : 브라우저에서 websocket을 지원하지 않는 경우 fallback 옵션을 활성화한다.
		 */
		registry.addEndpoint("/ws").setAllowedOrigins("*").withSockJS();
		//ErrorHandler를 등록한다.
		registry.setErrorHandler(stompSubProtocolErrorHandler);
	}
	
	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.enableSimpleBroker("/topic", "/queue");
		registry.setApplicationDestinationPrefixes("/pub");
	}
	
	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(stompChannelInterceptor);
	}
	
	@Override
	public void configureClientOutboundChannel(ChannelRegistration registration) {
		super.configureClientOutboundChannel(registration);
	}
	
	@Override
	public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
		super.configureWebSocketTransport(registry);
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		super.addArgumentResolvers(argumentResolvers);
	}

	@Override
	public void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
		super.addReturnValueHandlers(returnValueHandlers);
	}

	@Override
	public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
		return super.configureMessageConverters(messageConverters);
	}
	
	/**
	 * spring.main.allow-bean-definition-overriding=true 설정이 필요하다.
	 */
	@Bean
    public WebSocketHandler subProtocolWebSocketHandler() {
        return new CustomSubProtocolWebSocketHandler(clientInboundChannel(), clientOutboundChannel());
    }
}
