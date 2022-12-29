package com.loblaw.metrics.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.loblaw.metrics.exception.UnexpectedLogExtractionException;
import com.loblaw.metrics.helper.LogExtractionHelper;
import com.loblaw.metrics.model.TempLastRun;

@RunWith(SpringJUnit4ClassRunner.class)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { LogSummaryServiceImpl.class, TempLastRun.class })
public class LogSummaryServiceImplTest {

	@MockBean
	private Logger logger;

	@MockBean
	private LogExtractionHelper logExtractionHelper;

	@Autowired
	private LogSummaryServiceImpl logSummaryServiceImpl;

	@Test
	@DisplayName("When there are no logs to extract on schedule")
	public void scheduledSendLogData_noLogsExtracted() throws IOException {
		when(logExtractionHelper.extractLogs(any(TempLastRun.class))).thenReturn(1);
		doNothing().when(logExtractionHelper).updateLastRun(any(TempLastRun.class));
		doNothing().when(logExtractionHelper).updateCounterInfo();

		logSummaryServiceImpl.scheduledSendLogData();

		verify(logExtractionHelper).extractLogs(any(TempLastRun.class));
		verify(logExtractionHelper).updateLastRun(any(TempLastRun.class));
		verify(logExtractionHelper).updateCounterInfo();
	}

	@Test
	@DisplayName("When logs are extracted on schedule")
	public void scheduleSendLogData_logsExtracted() {
		when(logExtractionHelper.extractLogs(any(TempLastRun.class))).thenReturn(1);
		doNothing().when(logExtractionHelper).updateLastRun(any(TempLastRun.class));
		doNothing().when(logExtractionHelper).updateCounterInfo();

		logSummaryServiceImpl.scheduledSendLogData();

		verify(logExtractionHelper).extractLogs(any(TempLastRun.class));
		verify(logExtractionHelper).updateLastRun(any(TempLastRun.class));
		verify(logExtractionHelper).updateCounterInfo();
	}

	@Test
	@DisplayName("When extracting logs throwns an error catch the exception")
	public void scheduleSendLogData_exceptionThrown() {
		when(logExtractionHelper.extractLogs(any(TempLastRun.class)))
				.thenThrow(new RuntimeException("Test throwing exception"));
		logSummaryServiceImpl.scheduledSendLogData();
	}

	@Test
	@DisplayName("When there are logs to extract with a single search keyword - then return a String representation of a list of OutLogRes")
	public void sendLogData_logsExtractedWithSingleSearchKeyword_ReturnsListWithMessages() {
		String filePath = "/src/test/resources/sample-log.log";
		String searchKeywords = "brave";
		String[] searchStrings = { "brave" };
		String excludeStrings = "jsse";
		String[] excludeKeywords = { "jsse" };

		when(logExtractionHelper.extractLogs(filePath, searchStrings, excludeKeywords)).thenReturn(1);
		doNothing().when(logExtractionHelper).updateCounterInfo();

		logSummaryServiceImpl.sendLogData(filePath, searchKeywords, excludeStrings);

		verify(logExtractionHelper).extractLogs(filePath, searchStrings, excludeKeywords);
		verify(logExtractionHelper).updateCounterInfo();
	}

	@Test
	@DisplayName("Where there are logs to extract with multiple search keywords ")
	public void sendLogData_logsExtractedWithMultipleSearchKeywords_ReturnsListWithMessages() {
		String filePath = "/src/test/resources/sample-log.log";
		String searchKeywords = "brave,exception,ibm";
		String[] searchStrings = { "brave", "exception", "ibm" };
		String excludeStrings = "jsse,tomcat";
		String[] excludeKeywords = { "jsse", "tomcat" };

		when(logExtractionHelper.extractLogs(filePath, searchStrings, excludeKeywords)).thenReturn(1);
		doNothing().when(logExtractionHelper).updateCounterInfo();

		logSummaryServiceImpl.sendLogData(filePath, searchKeywords, excludeStrings);

		verify(logExtractionHelper).extractLogs(filePath, searchStrings, excludeKeywords);
		verify(logExtractionHelper).updateCounterInfo();
	}

	@Test
	@DisplayName("When there are no logs to extract - then return an empty String representation of a list")
	public void sendLogData_noLogsExtracted_ReturnsEmptyList() {
		String filePath = "file.log";
		String searchKeywords = "catalina";
		String[] searchStrings = { "catalina" };
		String excludeStrings = "";
		String[] excludeKeywords = {};

		when(logExtractionHelper.extractLogs(filePath, searchStrings, excludeKeywords)).thenReturn(1);
		doNothing().when(logExtractionHelper).updateCounterInfo();

		logSummaryServiceImpl.sendLogData(filePath, searchKeywords, excludeStrings);

		verify(logExtractionHelper).extractLogs(filePath, searchStrings, excludeKeywords);
		verify(logExtractionHelper).updateCounterInfo();
	}

	@Test(expected = UnexpectedLogExtractionException.class)
	@DisplayName("When extracting logs throws an exception - then catch the exception and throw an UnexpectedLogExtractionException")
	public void sendLogData_exceptionCaught_thenThrowUnexpectedLogExtractionException() {
		String filePath = "/src/test/resources/sample-log.log";
		String searchKeywords = "errors,expected";
		String[] searchStrings = { "errors", "expected" };
		String excludeStrings = "success";
		String[] excludeKeywords = { "success" };

		when(logExtractionHelper.extractLogs(filePath, searchStrings, excludeKeywords))
				.thenThrow(new RuntimeException("Test throwing exception"));

		logSummaryServiceImpl.sendLogData(filePath, searchKeywords, excludeStrings);
	}
}