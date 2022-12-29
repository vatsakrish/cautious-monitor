package com.loblaw.metrics.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.loblaw.metrics.exception.UnexpectedDatabaseQueryException;
import com.loblaw.metrics.exception.UnsuccessfulSqlQueryException;
import com.loblaw.metrics.helper.DatabaseExtractionHelper;
import com.loblaw.metrics.helper.LogHelper;
import com.loblaw.metrics.model.ScheduleTime;
import com.loblaw.metrics.service.DbQueryService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DbQueryServiceImpl implements DbQueryService {

	@Autowired
	private DatabaseExtractionHelper databaseExtractionHelper;

	@Autowired
	private LogHelper logHelper;

	private static final String LONG_TERM_QUERY_LOCATION = "db.long-term.query.";
	private static final String LONG_TERM_QUERY_TOTAL = "db.long-term.query.total";
	private static final String SHORT_TERM_QUERY_LOCATION = "db.short-term.query.";
	private static final String SHORT_TERM_QUERY_TOTAL = "db.short-term.query.total";

	@Override
	/**
	 * Execute a query on the connected database and send its results to Splunk
	 * 
	 * @param query        - String representing the database query to be made
	 * @param sendToSplunk - boolean representing whether to send query results to
	 *                     Splunk
	 * @return a List representing the output of the query
	 */
	public List<String> getDatabaseDetails(String query, boolean sendToSplunk) {
		List<String> dbQueryResult = new ArrayList<>();
		int responseCode = 0;

		try {
			// Track time taken for query
			long startTime = System.currentTimeMillis();
			// Query database
			responseCode = databaseExtractionHelper.addQueryResultsToList(dbQueryResult, query);
			// Calculate elapsed time for query
			long finishTime = System.currentTimeMillis();
			long elapsedTime = finishTime - startTime;

			if (sendToSplunk) {
				String methodName = "custom_query";

				databaseExtractionHelper.sendQueryResultsToSplunk(dbQueryResult, methodName, responseCode, elapsedTime);
				int outCount = logHelper.incOutResponse();
				log.info(String.format("Finished extraction of Database queries (OC: %d)", outCount));
			}

		} catch (Exception e) {
			log.error("Database Query : Unexpected exception in getDatabaseDetails : {}", e.toString(), e);
			throw new UnexpectedDatabaseQueryException("Error querying database with REST call - " + e.getMessage());
		}

		// SQL query was unsuccessful
		if (responseCode < 0 || responseCode == DatabaseExtractionHelper.SKIP_STATUS_CODE) {
			log.info("SQL Query was unsuccessful with response code: " + responseCode);
			throw new UnsuccessfulSqlQueryException(responseCode);
		}

		return dbQueryResult;
	}

	/**
	 * On the given long term schedule, update the store build version and execute
	 * the queries given in the application properties on the connected database and
	 * send their results to Splunk.
	 */
	@Override
	@Scheduled(cron = "${db.long-term.cron-expression}")
	public void longTermScheduledDatabase() {
		try {
			databaseExtractionHelper.scheduledSendDatabaseDetails(LONG_TERM_QUERY_LOCATION, LONG_TERM_QUERY_TOTAL,
					ScheduleTime.LONG_TERM);
		} catch (Exception e) {
			log.error("Database Query : Unexpected exception in longTermScheduledDatabase : {}", e.toString(), e);
		}
	}

	/**
	 * On the given short term schedule, execute the queries given in the
	 * application properties on the connected database and send their results to
	 * Splunk
	 */
	@Override
	@Scheduled(cron = "${db.short-term.cron-expression}")
	public void shortTermScheduledDatabase() {
		try {
			databaseExtractionHelper.scheduledSendDatabaseDetails(SHORT_TERM_QUERY_LOCATION, SHORT_TERM_QUERY_TOTAL,
					ScheduleTime.SHORT_TERM);
		} catch (Exception e) {
			log.error("Database Query : Unexpected exception in shortTermScheduledDatabase : {}", e.toString(), e);
		}
	}
}
