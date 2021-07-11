package com.example.demo.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
	EXCEPTION("EXCEPTION", "에러", 400)
	//AUTH
	/** 계정 없음 or 패스워드 불일치 */
	, INVALID_LOGIN_INPUT("INVALID_LOGIN_INPUT", "잘못된 아이디 or 패스워드 입니다", 400)
	/** 계정 비활성화 */
	, ACCOUNT_DISABLED("ACCOUNT_DISABLED", "계정이 비활성화 상태입니다.", 400)
	/** 접근 권한 없음 */
	, FORBIDDEN("FORBIDDEN", "접근 권한이 없습니다.", 403)
	/** 로그인 필요 */
	, UNAUTHORIZED("UNAUTHORIZED", "로그인이 필요합니다.", 401)
	/** 잘못된 입력 값 */
	, INVALID_INPUT_VALUE("INVALID_INPUT_VALUE", "잘못된 입력 값 입니다.", 400)
	/** 프로필 사진 개수 초과 */
	, EXCEED_PROFILE_PICTURE("EXCEED_PROFILE_PICTURE", "프로필 사진은 하나만 등록 가능합니다.", 400)
	/** 회원을 찾을 수 없음 */
	, ACCOUNT_NOT_FOUND("ACCOUNT_NOT_FOUND", "회원을 찾을수 없습니다.", 400)
	/** 중복된 아이디 */
	, ACCOUNT_DUPLICATION("ACCOUNT_DUPLICATION", "중복된 아이디입니다.", 400)
	/** api 요청자와 로그인한 회원이 다름 */
	,INVALID_REQURESTOR("INVALID_REQURESTOR", "로그인한 회원이 아닙니다.", 400)
	/** 파일 업로드 실패 */
	,FILE_UPLOAD_EXCEPTION("FILE_UPLOAD_EXCEPTION", "파일 업로드에 실패했습니다.", 400)
	/** 등록 가능한 프로필 사진 개수 초과 */
	,PROFILE_PICTURE_COUNT_EXCEED("PROFILE_PICTURE_COUNT_EXCEED", "등록 가능항 프로필 사진 개수를 초과 했습니다.", 400)
	/** 등록된 프로필 사진이 없음 */
	,PROFILE_PICTURE_NOT_FOUND("PROFILE_PICTURE_NOT_FOUND", "등록된 프로필 사진이 없습니다.", 400)
	/** 이미지 파일이 아님 */
	,NOT_IMAGE_FILE("NOT_IMAGE_FILE", "이미지 파일이 아닙니다.", 400)
	/** 패스워드 불일치 */
	,PASSWORD_MITMATCH("PASSWORD_MISMATCH", "패스워드가 일치하지 않습니다.", 400)
	/** 토큰 없음 */
	,TOKEN_NOT_FOUND("TOKEN_NOT_FOUND", "토큰이 없습니다.", 400)
	/** 리프레시 실패 */
	,REFRESH_FAILURE("REFRESH_FAILURE", "리프레시 실패했습니다.", 400)
	/** 잘못된 요청 */
	,INVALID_REQUEST("INVALID_REQUEST", "잘못된 요청입니다.", 400)
	;
	
	private String code;
	private String message;
	private int status;
	private ErrorCode(String code, String message, int status) {
		this.code = code;
		this.message = message;
		this.status = status;
	}
}
