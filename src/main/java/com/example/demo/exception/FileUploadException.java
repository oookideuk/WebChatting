package com.example.demo.exception;

public class FileUploadException extends RuntimeException {
	private static final long serialVersionUID = 50017151868662762L;

	public FileUploadException(Exception e) {
		super(e);
	}
}
