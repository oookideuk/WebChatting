package com.example.demo.dto;

import java.io.Serializable;

import javax.validation.constraints.Pattern;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

public class AuthDTO {
	@Getter @Setter @ToString
	public static class LoginReq implements Serializable{
		private static final long serialVersionUID = 7086301456417983434L;
		@Pattern(regexp = "^[A-Za-z0-9]{3,20}$", message = "숫자 or 알파벳 3~20자리")
		private String memberId;
		@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,20}$", message = "최소 하나의 숫자 and 알파벳 6~20자리")
		private String password;
		
		@Builder
		public LoginReq(String memberId, String password) {
			this.memberId = memberId;
			this.password = password;
		}
	}
	
	@Getter @ToString
	public static class JwtAuthRes implements Serializable{
		private static final long serialVersionUID = -8540209411748458248L;
		private String accessToken;
		private String refreshToken;
		
		@Builder
		private JwtAuthRes(String accessToken, String refreshToken) {
			this.accessToken = accessToken;
			this.refreshToken = refreshToken;
		}
	}
}
