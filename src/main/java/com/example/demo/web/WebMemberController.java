package com.example.demo.web;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 404 - Not found when entered directly into the browser
 * 참고
 * https://github.com/jonashackt/spring-boot-vuejs/issues/41
 * https://jamong-icetea.tistory.com/214
 */
@Controller
public class WebMemberController implements ErrorController{
	
	@GetMapping("/error")
	public String redirectRoot() {
		return "forward:/";
	}
	
	@Override
	public String getErrorPath() {
		return "/error";
	}
}
