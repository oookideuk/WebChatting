package com.example.demo.exception;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.validation.BindingResult;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class ErrorResponse implements Serializable {
	private static final long serialVersionUID = 5797634650382462170L;
	
	private String code;
	private String message;
	private int status;
	private List<FieldError> errors;
	
	public ErrorResponse(ErrorCode errorCode) {
		this.code = errorCode.getCode();
		this.message = errorCode.getMessage();
		this.status = errorCode.getStatus();
		this.errors = new ArrayList<>();
	}
	
	public ErrorResponse(ErrorCode errorCode, BindingResult bindingResult) {
		this.code = errorCode.getCode();
		this.message = errorCode.getMessage();
		this.status = errorCode.getStatus();
		this.errors = FieldError.of(bindingResult);
	}
	
	@Getter
	private static class FieldError{
		private String field;
		private String value;
		private String reason;
		
		private FieldError(String field, String value, String reason) {
			this.field = field;
			this.value = value;
			this.reason = reason;
		}
		
		private static List<FieldError> of(BindingResult bindingResult) {
            final List<org.springframework.validation.FieldError> fieldErrors = bindingResult.getFieldErrors();
            return fieldErrors.stream()
                    .map(error -> new FieldError(
                            error.getField()
                            , error.getRejectedValue() == null ? "" : error.getRejectedValue().toString()
                            , error.getDefaultMessage()))
                    .collect(Collectors.toList());
        }
	}
	
	
}
