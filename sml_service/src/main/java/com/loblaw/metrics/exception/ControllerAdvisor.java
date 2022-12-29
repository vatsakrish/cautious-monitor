package com.loblaw.metrics.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class ControllerAdvisor extends ResponseEntityExceptionHandler {
	@ExceptionHandler(UnexpectedApplicationHealthException.class)
	public ResponseEntity<String> UnexpectedApplicationHealthException(UnexpectedApplicationHealthException ex) {
		return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@ExceptionHandler(UnexpectedContainerMetricsException.class)
	public ResponseEntity<String> UnexpectedContainerMetricsException(UnexpectedContainerMetricsException ex) {
		return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@ExceptionHandler(UnexpectedDatabaseQueryException.class)
	public ResponseEntity<String> UnexpectedDatabaseQueryException(UnexpectedDatabaseQueryException ex) {
		return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@ExceptionHandler(UnexpectedLogExtractionException.class)
	public ResponseEntity<String> UnexpectedLogExtractionException(UnexpectedLogExtractionException ex) {
		return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
	}
	
	@ExceptionHandler(FileReaderException.class)
	public ResponseEntity<String> FileReaderException(FileReaderException ex) {
		return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(EmptyMessageException.class)
	public ResponseEntity<String> EmptyMessageException(EmptyMessageException ex) {
		return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
	}
	
	@ExceptionHandler(UnsuccessfulSqlQueryException.class)
	public ResponseEntity<String> UnsuccessfulSqlQueryException(UnsuccessfulSqlQueryException ex) {
		return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
	}
}
