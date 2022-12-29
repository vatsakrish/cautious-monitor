package com.loblaw.metrics.exception;

public class UnexpectedDatabaseQueryException extends RuntimeException {

	private static final long serialVersionUID = -3026609890724199563L;

	public UnexpectedDatabaseQueryException(String message) {
		super(message);
	}
}
