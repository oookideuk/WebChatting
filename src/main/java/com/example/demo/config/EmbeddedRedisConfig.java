package com.example.demo.config;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import redis.embedded.RedisServer;

/**
 * local 환경일 경우 embedded redis가 실행된다.
 */
@Profile("local")
@Configuration
public class EmbeddedRedisConfig {
	@Value("${spring.redis.port}")
	private int redisPort;
	private RedisServer redisServer;
	
	@PostConstruct
	public void init() {
		redisServer = new RedisServer(redisPort);
		redisServer.start();
	}
	
	@PreDestroy
	public void destroy() {
		if(redisServer != null) {
			redisServer.stop();
		}
	}
}
