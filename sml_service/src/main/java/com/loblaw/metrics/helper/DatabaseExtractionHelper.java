package com.loblaw.metrics.helper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.loblaw.metrics.model.LastRun;
import com.loblaw.metrics.model.ScheduleTime;
import com.loblaw.metrics.shared.model.OutDbQueryRes;
import com.loblaw.metrics.shared.util.StringUtil;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DatabaseExtractionHelper {
	private static final int VALID_STATUS_CODE = 200;

	public static final int SKIP_STATUS_CODE = 0;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/**
	 * Application scoped variable containing a String value representing the last
	 * time a scheduled Database call was run
	 */
	@Autowired
	private LastRun lastRun;

	@Autowired
	private LogHelper logHelper;

	@Autowired
	private Environment env;

	@Autowired
	private LastRunHelper lastRunHelper;

	@Autowired
	private StringUtil stringUtil;

	@Value("${db.last-run-file}")
	private String lastRunFile;

	@Value("${db.max-row}")
	private Integer maxRow;

	private static final String DEFAULT_TIME = "2021-01-01 00:00:00.000000";
	private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSSSSS";
	private static final String QUERY_SERACH = ".search";
	private static final String QUERY_METHOD_NAME = ".method-name";
	private static final String FILL_DATE = "(<<.*>>)";

	/**
	 * On the given schedule, execute the queries given in the application
	 * properties on the connected database and send their results to Splunk
	 * 
	 * @param queryLocation - String representing application property location for
	 *                      database query String
	 * @param queryTotal    - String representing application property location for
	 *                      total number of queries
	 * @param scheduleTime  - ScheduleTime representing time basis on which
	 *                      scheduler was ran
	 */
	public void scheduledSendDatabaseDetails(String queryLocation, String queryTotal, ScheduleTime scheduleTime) {
		String lastRunStr = null;
		List<String> dbQueryResult = null;

		String query = "";
		String querySearchLocation = "";
		String queryMethodName = "";
		String queryMethodLocation = "";
		Integer numQueries = 0;
		int corrQueries = 0;

		int inCount = logHelper.incInResponse();
		log.info(
				String.format("Starting %s extraction of Database queries (IC: %d)", scheduleTime.toString(), inCount));

		// Get total number of queries to execute
		if (env.containsProperty(queryTotal)) {
			numQueries = stringUtil.parseInt(env.getProperty(queryTotal));
			if (numQueries == null)
				numQueries = 0;
		}

		// Extract last time database extraction was executed
		lastRunStr = getLastDbRun();

		// Execute each SQL query
		for (int i = 1; i <= numQueries; i++) {
			querySearchLocation = queryLocation + i + QUERY_SERACH;
			queryMethodLocation = queryLocation + i + QUERY_METHOD_NAME;

			if (env.containsProperty(querySearchLocation)) {
				dbQueryResult = new ArrayList<>();

				query = env.getProperty(querySearchLocation);

				// Only consider the last run time for short term scheduler
				if (lastRunStr != null && scheduleTime.equals(ScheduleTime.SHORT_TERM)) {
					query = query.replaceAll(FILL_DATE, String.format("'%s'", lastRunStr));
				}

				log.debug("Query to database is: " + query);

				// Track time taken for query
				long startTime = System.currentTimeMillis();

				// Query database and update last run date time
				int responseCode = addQueryResultsToList(dbQueryResult, query);

				// DB Query was successful, increment count of correct queries
				if (responseCode == VALID_STATUS_CODE) {
					corrQueries++;
				}

				// Calculate elapsed time for query
				long finishTime = System.currentTimeMillis();
				long elapsedTime = finishTime - startTime;

				if (env.containsProperty(queryMethodLocation)) {
					queryMethodName = env.getProperty(queryMethodLocation);
				}
				sendQueryResultsToSplunk(dbQueryResult, queryMethodName, responseCode, elapsedTime);
			}
		}

		int outCount = logHelper.incOutResponse();
		log.info(String.format("Finished %s extraction of Database queries (OC: %d)", scheduleTime.toString(),
				outCount));

		// Update the last db run time if the short term scheduler is running
		if (scheduleTime.equals(ScheduleTime.SHORT_TERM) && corrQueries > 0) {
			setLastDbRun();
		}
	}

	/**
	 * Given an SQL query to execute, append the results of the query to list
	 * 
	 * @param list  - List representing the results of SQL queries
	 * @param query - String representing an SQL query
	 * @return - int representing the reponse code of the database query
	 */
	public int addQueryResultsToList(List<String> list, String query) {
		int responseCode = SKIP_STATUS_CODE;

		try {
			// Execute query and add results to list
			jdbcTemplate.setMaxRows(maxRow);
			jdbcTemplate.queryForList(query).forEach(res -> {
				String strRes = stringUtil.mapToString(res);
				if (strRes != null)
					list.add(strRes);
			});
			responseCode = VALID_STATUS_CODE;

			log.debug("List of query results: " + list.toString());
		} catch (BadSqlGrammarException e) {
			// Handling SQL Exception
			responseCode = e.getSQLException().getErrorCode();
			log.error("Database Query Helper : SQL Exception in addQueryResultsToList : {}", e.toString(), e);
		} catch (CannotGetJdbcConnectionException e) {
			log.error("Database Query Helper : Connection Exception in addQueryResultsToList : {}", e.toString(), e);
		} catch (Exception e) {
			log.error("Database Query Helper : Unexpected Exception in addQueryResultsToList : {}", e.toString(), e);
		}

		return responseCode;
	}

	/**
	 * Given a list of query res, convert the list to an OutDbQueryRes object and
	 * send its contents to Splunk
	 * 
	 * @param res          - List representing the results of SQL queries
	 * @param methodName   - String representing method name to identify query
	 * @param responseCode - int representing response code of the database query
	 * @param elapsedTime  - long representing the time taken for database queries
	 */
	public void sendQueryResultsToSplunk(List<String> res, String methodName, int responseCode, long elapsedTime) {
		log.info("Starting to send query results to Splunk");

		// Avoid sending res with responseCode SKIP_STATUS_CODE
		if (responseCode != SKIP_STATUS_CODE) {
			OutDbQueryRes outDbQueryRes = logHelper.reqHelper(res, methodName, responseCode, elapsedTime);
			boolean incCounter = false;

			// Convert OutDbQueryRes to JSON and log its results
			String strOutDbQueryRes = stringUtil.mapToString(outDbQueryRes);

			log.debug("Db Query: " + strOutDbQueryRes);

			if (strOutDbQueryRes != null)
				logHelper.logOutResponse(strOutDbQueryRes, incCounter);
			log.info("Finished extracting database queries");
		} else {
			log.info("Skipping logging process to Splunk with status code: " + responseCode);
		}

		log.info("Finished sending query results to Splunk");
	}

	/**
	 * Calculates the last time a scheduled database query was run
	 * 
	 * @return a String representation of the last time a scheduled database query
	 *         was run
	 */
	private String getLastDbRun() {
		LocalDateTime lastRunDateTime = null;
		String ret = null;
		DateTimeFormatter format = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);

		// Sets last run time to a default time if not initialized
		if (lastRun.getLastDbDateTime() == null) {
			lastRun.setLastDbDateTime(lastRunHelper.getDefaultLastRun(lastRunFile, DATE_TIME_FORMAT, DEFAULT_TIME));
		}

		// Convert lastRun time from String to a date time object
		try {
			lastRunDateTime = LocalDateTime.parse(lastRun.getLastDbDateTime(), format);
		} catch (DateTimeParseException e) {
			log.error("Database Query Helper : Date parse exception in getLastDbRun : {}", e.toString(), e);
			lastRunDateTime = LocalDateTime
					.parse(lastRunHelper.getDefaultLastRun(lastRunFile, DATE_TIME_FORMAT, DEFAULT_TIME), format);
		} catch (Exception e) {
			log.error("Database Query Helper : Unexpected exception in getLastDbRun : {}", e.toString(), e);
		}

		if (lastRunDateTime != null)
			ret = lastRunDateTime.format(format);

		return ret;
	}

	/**
	 * Sets the last time a scheduled database query was run to the current time
	 */
	private void setLastDbRun() {
		DateTimeFormatter format = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
		// Set the last time scheduled db queries were run to now
		lastRun.setLastDbDateTime(LocalDateTime.now().format(format));
		lastRunHelper.setDefaultLastRun(lastRunFile, DATE_TIME_FORMAT);
	}
}
