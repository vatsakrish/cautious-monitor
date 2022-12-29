package com.loblaw.metrics.exception;

public class EmptyMessageException extends RuntimeException {

	private static final long serialVersionUID = 970663269430570412L;

	public EmptyMessageException(String message) {
		super(message);
	}
}
