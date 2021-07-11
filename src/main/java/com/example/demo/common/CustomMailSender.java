package com.example.demo.common;

import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

@Component
public class CustomMailSender {
	@Autowired
	private JavaMailSender javaMailSender;
	@Autowired
	private SpringTemplateEngine templateEngine;
	
	@Async
	public void sendMail(String subject, String to, Map<String, Object> contextMap, String path) throws MessagingException {
		MimeMessage message = javaMailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message, true);
		// 메일 제목 설정
		helper.setSubject(subject);
		// 수신자 설정
		helper.setTo(to);
		// 타임리프에 전달할 데이터 설정
		Context context = new Context();
		context.setVariables(contextMap);
		// 메일 내용 설정 : 템플릿 프로세스
		String html = templateEngine.process(path, context);
		helper.setText(html, true);
		//메일 보내기
		javaMailSender.send(message);
	}
}
