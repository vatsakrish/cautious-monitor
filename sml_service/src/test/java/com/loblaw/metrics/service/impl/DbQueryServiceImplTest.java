package com.loblaw.metrics.service.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.loblaw.metrics.exception.UnexpectedDatabaseQueryException;
import com.loblaw.metrics.exception.UnsuccessfulSqlQueryException;
import com.loblaw.metrics.helper.DatabaseExtractionHelper;
import com.loblaw.metrics.helper.LogHelper;
import com.loblaw.metrics.model.ScheduleTime;

@RunWith(SpringJUnit4ClassRunner.class)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { DbQueryServiceImpl.class })
public class DbQueryServiceImplTest {
	@MockBean
	private DatabaseExtractionHelper databaseExtractionHelper;
	@MockBean
	private LogHelper logHelper;
	@Autowired
	private DbQueryServiceImpl dbQueryServiceImpl;
	
	private static final String LONG_TERM_QUERY_LOCATION = "db.long-term.query.";
	private static final String LONG_TERM_QUERY_TOTAL = "db.long-term.query.total";
	private static final String SHORT_TERM_QUERY_LOCATION = "db.short-term.query.";
	private static final String SHORT_TERM_QUERY_TOTAL = "db.short-term.query.total";

	@Test
	@DisplayName("When multiple rows are extracted from db queries - then return list of results")
	public void getDatabaseDetails_queryMultipleResults_ReturnsResultsList() {
		String query = "SELECT count as TRANSACTION_COUNT by name, city FROM TX";
		boolean sendToSplunk = true;
		int responseStatus = 200;

		List<String> queryResults = new ArrayList<>();
		queryResults.add("{\"TRANSACTION_COUNT\": 0}");
		queryResults.add("{\"TRANSACTION_COUNT\": 5}");

		doAnswer(invocation -> {
			List<String> queryArgument = invocation.getArgument(0);
			for (String result : queryResults) {
				queryArgument.add(result);
			}
			return responseStatus;
		}).when(databaseExtractionHelper).addQueryResultsToList(anyList(), anyString());

		doNothing().when(databaseExtractionHelper).sendQueryResultsToSplunk(anyList(), anyString(), anyInt(),
				anyLong());

		List<String> actual = dbQueryServiceImpl.getDatabaseDetails(query, sendToSplunk);

		verify(databaseExtractionHelper).addQueryResultsToList(anyList(), anyString());
		verify(databaseExtractionHelper).sendQueryResultsToSplunk(anyList(), anyString(), anyInt(), anyLong());
		assertEquals(queryResults, actual);
	}

	@Test
	@DisplayName("When no rows are extracted from db queries - then return an empty list")
	public void getDatabaseDetails_noResults_thenReturEmptyList() {
		String query = "SELECT prescription as PRESCRIPTION by prescription_id FROM store";
		boolean sendToSplunk = false;
		int responseStatus = 200;
		
		List<String> queryResults = new ArrayList<>();
		
		when(databaseExtractionHelper.addQueryResultsToList(anyList(), anyString())).thenReturn(responseStatus);

		List<String> actual = dbQueryServiceImpl.getDatabaseDetails(query, sendToSplunk);

		verify(databaseExtractionHelper).addQueryResultsToList(anyList(), anyString());
		assertEquals(queryResults, actual);
	}

	@Test(expected = UnexpectedDatabaseQueryException.class)
	@DisplayName("When multiple rows are extracted from db queries - then throw UnexpectedDatabaseQueryException")
	public void getDatabaseDetails_throwException_thenThrowUnexpectedDatabaseQueryException() {
		String query = "SELECT count as TRANSACTION_COUNT by name, city FROM TX";
		boolean sendToSplunk = true;

		doThrow(new UnexpectedDatabaseQueryException("Testing exception")).when(databaseExtractionHelper)
				.addQueryResultsToList(anyList(), anyString());

		dbQueryServiceImpl.getDatabaseDetails(query, sendToSplunk);
	}
	
	@Test(expected = UnsuccessfulSqlQueryException.class)
	@DisplayName("When db query fails and returns a negative response code - then throw UnsuccessfulSqlQueryException")
	public void getDatabaseDetails_databaseQueryFailsWithNegativeResponseCode_thenThrowUnsuccessfulSqlQueryException() {
		String query = "SELECT invalid_name as name FROM TX";
		boolean sendToSplunk = false;
		int responseCode = -206;
		
		when(databaseExtractionHelper.addQueryResultsToList(anyList(), anyString())).thenReturn(responseCode);

		dbQueryServiceImpl.getDatabaseDetails(query, sendToSplunk);
	}	

	@Test
	@DisplayName("When short term scheduler is run - then extract results and send to Splunk")
	public void shortTermScheduledDatabase_noExceptions() {
		doNothing().when(databaseExtractionHelper).scheduledSendDatabaseDetails(SHORT_TERM_QUERY_LOCATION,
				SHORT_TERM_QUERY_TOTAL, ScheduleTime.SHORT_TERM);

		dbQueryServiceImpl.shortTermScheduledDatabase();
		verify(databaseExtractionHelper).scheduledSendDatabaseDetails(SHORT_TERM_QUERY_LOCATION, SHORT_TERM_QUERY_TOTAL,
				ScheduleTime.SHORT_TERM);
	}

	@Test
	@DisplayName("When short term scheduler's scheduled send database details throws an exception - then catch the exception and log the error")
	public void shortTermScheduledDatabase_throwsException() {
		doThrow(new RuntimeException()).when(databaseExtractionHelper).scheduledSendDatabaseDetails(
				SHORT_TERM_QUERY_LOCATION, SHORT_TERM_QUERY_TOTAL, ScheduleTime.SHORT_TERM);

		dbQueryServiceImpl.shortTermScheduledDatabase();
	}

	@Test
	@DisplayName("When long term scheduler is run - then extract results and send to Splunk")
	public void longTermScheduleDatabase_noExceptions() {
		doNothing().when(databaseExtractionHelper).scheduledSendDatabaseDetails(LONG_TERM_QUERY_LOCATION,
				LONG_TERM_QUERY_TOTAL, ScheduleTime.LONG_TERM);

		dbQueryServiceImpl.longTermScheduledDatabase();
		verify(databaseExtractionHelper).scheduledSendDatabaseDetails(LONG_TERM_QUERY_LOCATION, LONG_TERM_QUERY_TOTAL,
				ScheduleTime.LONG_TERM);
	}

	@Test
	@DisplayName("When long term scheduler's scheduled send database details throws an exception - then catch the exception and log the error")
	public void longTermScheduleDatabase_throwsException() {
		doThrow(new RuntimeException()).when(databaseExtractionHelper)
				.scheduledSendDatabaseDetails(LONG_TERM_QUERY_LOCATION, LONG_TERM_QUERY_TOTAL, ScheduleTime.LONG_TERM);

		dbQueryServiceImpl.longTermScheduledDatabase();
	}
}