package com.loblaw.metrics.exception;

public class UnsuccessfulSqlQueryException extends RuntimeException {

	private static final long serialVersionUID = -5325435701454631841L;
	
	public UnsuccessfulSqlQueryException(int errorCode) {
		super(String.format("SQL query failed with Error Code: %d", errorCode));
	}
}
