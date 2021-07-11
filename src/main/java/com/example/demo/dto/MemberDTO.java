package com.example.demo.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import com.example.demo.domain.Role;
import com.example.demo.domain.entitiy.MemberEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

public class MemberDTO {
	
	@Getter @Setter
	@ToString
	@NoArgsConstructor @AllArgsConstructor
	@Builder
	public static class SignupReq implements Serializable {
		private static final long serialVersionUID = -7302144908319514835L;
		@Pattern(regexp = "^[A-Za-z0-9]{3,20}$", message = "숫자 or 알파벳 3~20자리")
		private String memberId;
		@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,20}$", message = "최소 하나의 숫자 and 알파벳 6~20자리")
		private String password;
		private String encodedPassword;
		@Pattern(regexp = "^[A-Za-z0-9가-힣]{3,20}$", message = "숫자 or 한글 or 알파벳 3~20자리")
		private String name;
		private Role role;
		@Email(message = "올바른 형식의 이메일 주소여야 합니다.")
		@NotBlank(message = "올바른 형식의 이메일 주소여야 합니다.")
		private String email;
		private String emailAuthKey;
		
		public MemberEntity toEntity() {
			return MemberEntity.builder()
					.memberId(this.memberId)
					.password(this.encodedPassword)
					.name(this.name)
					.role(this.role)
					.email(this.email)
					.emailAuthKey(this.emailAuthKey)
					.build();
		}
	}
	
	@Getter @Setter
	@ToString
	@NoArgsConstructor @AllArgsConstructor
	@Builder
	public static class PersonalInfomationModificationReq implements Serializable{
		private static final long serialVersionUID = -8106906804462951602L;
		private String memberId;
		@Pattern(regexp = "^[A-Za-z0-9가-힣]{3,20}$", message = "숫자 or 한글 or 알파벳 3~20자리")
		private String name;
	}
	
	@Getter @Setter
	@ToString
	@NoArgsConstructor @AllArgsConstructor
	@Builder
	public static class passwordModificationReq implements Serializable{
		private static final long serialVersionUID = -8706085399262784975L;
		@Pattern(regexp = "^[A-Za-z0-9]{3,20}$", message = "숫자 or 알파벳 3~20자리")
		private String memberId;
		@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,20}$", message = "최소 하나의 숫자 and 알파벳 6~20자리")
		private String password;
		@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,20}$", message = "최소 하나의 숫자 and 알파벳 6~20자리")
		private String passwordChange;
	}
	
	@Getter
	@Setter
	@ToString
	@Builder
	@EqualsAndHashCode
	public static class Response implements Serializable {
		private static final long serialVersionUID = -8276796145494887477L;
		private String memberId;
		private String name;
		private Role role;
		private String email;
		private LocalDateTime registerDate;	
		
		public static MemberDTO.Response of(MemberEntity entity) {
			return MemberDTO.Response.builder()
					.memberId(entity.getMemberId())
					.name(entity.getName())
					.role(entity.getRole())
					.email(entity.getEmail())
					.registerDate(entity.getRegisterDate())
					.build();
		}
	}
}
