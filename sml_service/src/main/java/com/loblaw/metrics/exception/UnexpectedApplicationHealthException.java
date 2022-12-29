package com.loblaw.metrics.exception;

public class UnexpectedApplicationHealthException extends RuntimeException {

	private static final long serialVersionUID = 3439947610683417938L;

	public UnexpectedApplicationHealthException(String message) {
		super(message);
	}
}
