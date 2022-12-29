package com.loblaw.metrics.exception;

public class FileReaderException extends RuntimeException {

	private static final long serialVersionUID = 8077735249045456892L;

	public FileReaderException (String message) {
		super(message);
	}
}
