package com.example.demo.web;

import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

//@Controller
public class WebChatController {
	/**
	 * 채팅 리스트 화면
	 */
	@GetMapping("/chat/rooms")
	public String chatRoomListPage() {
		return "/chat/chatRooms";
	}
	
	/**
	 * 채팅방 화면
	 */
	@GetMapping("/chat/rooms/{roomId}")
	public String chatRoomPage(HttpServletResponse response, @PathVariable String roomId) {
		response.addHeader("roomId", roomId);
		return "/chat/chatRoom";
	}
}
