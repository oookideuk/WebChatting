package com.example.demo.test;


import java.net.UnknownHostException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.example.demo.domain.repository.ChatMessageAttachRepository;
import com.example.demo.domain.repository.ChatMessageRepository;
import com.example.demo.domain.repository.ChatParticipantRepository;
import com.example.demo.domain.repository.ChatRoomRepository;
import com.example.demo.domain.repository.JwtTokenRepository;
import com.example.demo.domain.repository.MemberRepository;
import com.example.demo.domain.repository.ProfilePictureRepository;
import com.example.demo.service.MemberService;

import ch.qos.logback.classic.Logger;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class Tests {
	private final Logger logger = (Logger) LoggerFactory.getLogger(this.getClass());
	private MockMvc mvc;
	@Autowired HandlerExceptionResolver handlerExceptionResolver;
	@Autowired PasswordEncoder passwordEncoder;
	@Autowired ChatRoomRepository chatRoomRepository;
	@Autowired ChatParticipantRepository chatParticipantRepo;
	@Autowired private JwtTokenRepository jwtTokenRepository;
	@Autowired private MemberRepository memberRepository;
	@Autowired private ProfilePictureRepository profilePictureRepo;
	@Autowired private MemberService memberService;
	@Autowired private RedisTemplate<String, Object> redisTemplate;
	@Autowired private ChatRoomRepository roomRepo;
	@Autowired private ChatMessageRepository chatMessageRepo;
	@Autowired private ChatMessageAttachRepository chatMessageAttachRepo;
	
	@BeforeEach
	void setup(WebApplicationContext webApplicationContex) throws UnknownHostException {
		mvc = MockMvcBuilders.webAppContextSetup(webApplicationContex)
				//한글깨짐 방지
				.addFilters(new CharacterEncodingFilter("UTF-8", true))
				.apply(SecurityMockMvcConfigurers.springSecurity())
				.build();
	}
	
	@Test
	@Transactional
	void test1() {
		
	}
}
