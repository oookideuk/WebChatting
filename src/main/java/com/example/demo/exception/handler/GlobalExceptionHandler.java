package com.example.demo.exception.handler;


import java.util.List;

import javax.mail.MessagingException;
import javax.persistence.EntityNotFoundException;
import javax.validation.UnexpectedTypeException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException.Forbidden;

import com.example.demo.exception.AccountDuplicationException;
import com.example.demo.exception.AccountNotFoundException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.exception.ErrorResponse;
import com.example.demo.exception.FileUploadException;
import com.example.demo.exception.InvalidJwtException;
import com.example.demo.exception.InvalidRequestorException;
import com.example.demo.exception.NotImageFileException;
import com.example.demo.exception.PasswordMismathException;
import com.example.demo.exception.ProfilePictureCountExceedException;
import com.example.demo.exception.ProfilePictureNotFoundException;
import com.example.demo.exception.RefreshFailureException;
import com.example.demo.exception.TokenNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@ExceptionHandler(Exception.class)
	public ResponseEntity<String> handleException(Exception e){
		logger.debug("handleException[{}]", e);
		return new ResponseEntity<String>("Exception ????????????", HttpStatus.BAD_REQUEST);
	}
	
	/**
	 * ????????? ??????
	 * ?????? ?????? or ???????????? ?????????
	 */
	@ExceptionHandler({UsernameNotFoundException.class, BadCredentialsException.class})
	public ResponseEntity<ErrorResponse> handleInvalidLoginInputException(Exception e){
		logger.debug("handleInvalidLoginInputException[{}]", e);
		return new ResponseEntity<ErrorResponse>(new ErrorResponse(ErrorCode.INVALID_LOGIN_INPUT), HttpStatus.BAD_REQUEST);
	}
	
	/**
	 * ????????? ??????
	 * ?????? ????????????
	 */
	@ExceptionHandler(DisabledException.class)
	public ResponseEntity<ErrorResponse> handleAccountDisableException(Exception e){
		logger.debug("handleAccountDisableException[{}]", e);
		return new ResponseEntity<ErrorResponse>(new ErrorResponse(ErrorCode.ACCOUNT_DISABLED), HttpStatus.BAD_REQUEST);
	}
	
	/**
	 * ????????? ??? ????????????.
	 */
	@ExceptionHandler({AuthenticationException.class, InvalidJwtException.class})
	public ResponseEntity<ErrorResponse> handleAuthenticationException(Exception e){
		logger.debug("handleAuthenticationException[{}]", e);
		return new ResponseEntity<ErrorResponse>(new ErrorResponse(ErrorCode.UNAUTHORIZED) ,HttpStatus.UNAUTHORIZED);
	}
	
	/**
	 * ?????? ?????? ?????? ?????? ????????????.
	 */
	@ExceptionHandler({Forbidden.class, AccessDeniedException.class})
	public ResponseEntity<ErrorResponse> handleAccessDeniedException(Exception e){
		logger.debug("handleAccessDeniedException[{}]", e);
		return new ResponseEntity<ErrorResponse>(new ErrorResponse(ErrorCode.FORBIDDEN) ,HttpStatus.FORBIDDEN);
	}
	
	/**
	 * ????????? ?????? ??? ?????? ?????? ????????????.
	 */
	@ExceptionHandler({AccountNotFoundException.class})
	public ResponseEntity<ErrorResponse> handleAccountNotFoundException(Exception e){
		logger.debug("handleAccountNotFoundException[{}]", e);
		return new ResponseEntity<ErrorResponse>(new ErrorResponse(ErrorCode.ACCOUNT_NOT_FOUND), HttpStatus.BAD_REQUEST);
	}
	
	/**
	 * ????????? ???????????? ?????? ????????????.
	 */
	@ExceptionHandler({AccountDuplicationException.class})
	public ResponseEntity<ErrorResponse> handleAccountDuplicationException(Exception e){
		logger.debug("handleAccountDuplicationException[{}]", e);
		return new ResponseEntity<ErrorResponse>(new ErrorResponse(ErrorCode.ACCOUNT_DUPLICATION), HttpStatus.BAD_REQUEST);
	}
	
	/**
	 * api ???????????? ???????????? ????????? ?????? ?????? ????????????.
	 */
	@ExceptionHandler({InvalidRequestorException.class})
	public ResponseEntity<ErrorResponse> handleInvalidRequestorException(Exception e){
		logger.debug("handleInvalidRequestorException[{}]", e);
		return new ResponseEntity<ErrorResponse>(new ErrorResponse(ErrorCode.INVALID_REQURESTOR), HttpStatus.BAD_REQUEST);
	}
	
	/**
	 * ?????? ????????? ??????
	 */
	@ExceptionHandler(FileUploadException.class)
	public ResponseEntity<ErrorResponse> handleFileUploadException(Exception e){
		logger.debug("handleFileUploadException[{}]", e);
		return new ResponseEntity<ErrorResponse>(new ErrorResponse(ErrorCode.FILE_UPLOAD_EXCEPTION), HttpStatus.BAD_REQUEST);
	}
	
	/**
	 * DB ????????? ????????? ????????? ????????????.
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(Exception e){
		logger.debug("handleDataIntegrityViolationException[{}]", e);
		return new ResponseEntity<ErrorResponse>(new ErrorResponse(ErrorCode.INVALID_INPUT_VALUE), HttpStatus.BAD_REQUEST);
	}
	
	/**
	 * DB ?????? ??? ?????? Entity??? ?????? ??? ????????????.
	 */
	@ExceptionHandler(EntityNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleEntityNotFoundException(Exception e){
		logger.debug("handleEntityNotFoundException[{}]", e);
		return new ResponseEntity<ErrorResponse>(new ErrorResponse(ErrorCode.INVALID_INPUT_VALUE), HttpStatus.BAD_REQUEST);
	}
	
	/**
	 * ?????? ????????? ????????? ?????? ????????? ???????????? ??? ????????????.
	 */
	@ExceptionHandler(ProfilePictureCountExceedException.class)
	public ResponseEntity<ErrorResponse> handleProfilePictureCountExceedException(Exception e){
		logger.debug("handleProfilePictureCountExceedException[{}]", e);
		return new ResponseEntity<ErrorResponse>(new ErrorResponse(ErrorCode.EXCEED_PROFILE_PICTURE), HttpStatus.BAD_REQUEST);
	}
	
	/**
	 * ????????? ????????? ????????? ?????? ??? ????????????.
	 */
	@ExceptionHandler(ProfilePictureNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleProfilePictureNotFoundException(Exception e){
		logger.debug("handleProfilePictureNotFoundException[{}]",e);
		return new ResponseEntity<ErrorResponse>(new ErrorResponse(ErrorCode.PROFILE_PICTURE_NOT_FOUND), HttpStatus.BAD_REQUEST);
	}
	
	/**
	 * ????????? ????????? ?????? ?????? ????????????.
	 */
	@ExceptionHandler(NotImageFileException.class)
	public ResponseEntity<ErrorResponse> handleNotImageFileException(Exception e){
		logger.debug("handleNotImageFileException[{}]", e);
		return new ResponseEntity<ErrorResponse>(new ErrorResponse(ErrorCode.NOT_IMAGE_FILE), HttpStatus.BAD_REQUEST);
	}
	
	/**
	 * ???????????? ?????????
	 */
	@ExceptionHandler(PasswordMismathException.class)
	public ResponseEntity<ErrorResponse> handlePasswordMismatchException(Exception e){
		logger.debug("handlePasswordMismatchException[{}]", e);
		return new ResponseEntity<ErrorResponse>(new ErrorResponse(ErrorCode.PASSWORD_MITMATCH), HttpStatus.BAD_REQUEST);
	}
	
	/**
	 * DB??? ?????? ?????? ??????
	 */
	@ExceptionHandler(TokenNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleTokenNotFoundException(Exception e){
		logger.debug("handleTokenNotFoundException[{}]", e);
		return new ResponseEntity<ErrorResponse>(new ErrorResponse(ErrorCode.TOKEN_NOT_FOUND), HttpStatus.BAD_REQUEST);
	}
	
	/**
	 * ???????????? ??????
	 */
	@ExceptionHandler(RefreshFailureException.class)
	public ResponseEntity<ErrorResponse> handleRefreshFailureException(Exception e){
		logger.debug("handleRefreshFailureException[{}]", e);
		return new ResponseEntity<ErrorResponse>(new ErrorResponse(ErrorCode.REFRESH_FAILURE), HttpStatus.BAD_REQUEST);
	}
	
	/**
	 * ????????? ?????? ??????
	 */
	@ExceptionHandler({MessagingException.class, MailException.class})
	public ResponseEntity<ErrorResponse> handleMessagingException(Exception e){
		logger.debug("handleMessagingException[{}]", e);
		return new ResponseEntity<ErrorResponse>(new ErrorResponse(ErrorCode.INVALID_INPUT_VALUE), HttpStatus.BAD_REQUEST);
	}
	
	/**
	 * validation
	 */
	@ExceptionHandler({BindException.class, MethodArgumentNotValidException.class, UnexpectedTypeException.class})
	public ResponseEntity<ErrorResponse> handleValidationException(BindException e){
		logger.debug("handleValidationException[{}]", e);
		BindingResult brs = e.getBindingResult();
		List<FieldError> errors = brs.getFieldErrors();
		for(FieldError error : errors) {
			logger.debug("error[{}]", error);
			
		}
		
		return new ResponseEntity<ErrorResponse>(new ErrorResponse(ErrorCode.INVALID_INPUT_VALUE, e.getBindingResult()), HttpStatus.BAD_REQUEST);
	}
	
	/**
	 * RequestMethod ???????????? ??????.
	 */
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(Exception e){
		logger.debug("handleHttpRequestMethodNotSupportedException[{}]", e);
		return new ResponseEntity<ErrorResponse>(new ErrorResponse(ErrorCode.INVALID_REQUEST), HttpStatus.BAD_REQUEST);
	}
	
	@ExceptionHandler(DataAccessException.class)
	public ResponseEntity<ErrorResponse> handleDataAccessException(Exception e){
		logger.debug("handleDataAccessException[{}]", e);
		return new ResponseEntity<ErrorResponse>(new ErrorResponse(ErrorCode.INVALID_INPUT_VALUE), HttpStatus.BAD_REQUEST);
	}
}	
