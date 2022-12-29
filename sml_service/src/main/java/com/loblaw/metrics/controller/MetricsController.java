package com.loblaw.metrics.controller;

import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.loblaw.metrics.exception.EmptyMessageException;
import com.loblaw.metrics.helper.LogHelper;
import com.loblaw.metrics.service.ContainerMetricsService;
import com.loblaw.metrics.service.CounterInfoService;
import com.loblaw.metrics.service.DbQueryService;
import com.loblaw.metrics.service.HealthMetricService;
import com.loblaw.metrics.service.LogSummaryService;
import com.loblaw.metrics.shared.SmlSharedConstants;
import com.loblaw.metrics.shared.model.AppReq;
import com.loblaw.metrics.shared.model.OutAppReq;
import com.loblaw.metrics.shared.model.OutDataRes;
import com.loblaw.metrics.shared.util.StringUtil;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ExampleProperty;
import lombok.extern.slf4j.Slf4j;

/**
 * Storemetrics rest controller
 */

@Slf4j
@RestController
@Validated
@RequestMapping(SmlSharedConstants.API_VERSION)
public class MetricsController {

	@Autowired
	@Qualifier("in-counter")
	public CounterInfoService inCounter;

	@Autowired
	private LogHelper logHelper;

	@Autowired
	private HealthMetricService healthMetricService;

	@Autowired
	private LogSummaryService logSummaryService;

	@Autowired
	private DbQueryService serviceHealthMetricService;

	@Autowired
	private ContainerMetricsService containerMetricsService;

	@Autowired
	private StringUtil stringUtil;

	/**
	 * Extract metrics from request given in the request body and output its results
	 * to Splunk
	 * 
	 * @param request - AppReq representing application metrics to be sent to Splunk
	 * @return - a ResponseEntity representing the status of the operation
	 */
	@ApiOperation(value = "Send application metrics to Splunk")
	@ApiResponses(value = {
			@ApiResponse(code = 202, message = "Application metric sucessfully sent to Splunk", examples = @io.swagger.annotations.Example(value = {
					@ExampleProperty(mediaType = "*/*", value = "Application metric successfully sent to Splunk") })) })
	@PostMapping(path = SmlSharedConstants.APP_METRICS_URL, consumes = "application/json")
	public ResponseEntity<String> appmetrics(
			@ApiParam(value = "AppReq object containing application's metric details", required = true) @RequestBody @Valid AppReq request) {
		inCounter.incCounter();
		log.info("Incoming Request: " + request + " (IC: " + inCounter.getInfo().getCounter() + ")");

		// Transform request to a OutAppReq by adding additional attributes
		OutAppReq outAppReq = logHelper.reqHelper(request);
		// Convert outAppReq to a JSON
		String strOutAppReq = stringUtil.mapToString(outAppReq);

		// Log JSON
		int outCount = logHelper.logOutResponse(strOutAppReq);
		log.info("Finished appmetrics request (OC: " + outCount + ")");

		String res = "Application metric successfully sent to Splunk";

		return new ResponseEntity<>(res, HttpStatus.ACCEPTED);
	}

//----------------------------------------------------------------------------------------------

	/**
	 * Send a message to Splunk
	 * 
	 * @param message - String representing a message to send to Splunk
	 * @return - a ResposneEntity containing the status of the response
	 */
	@ApiOperation(value = "Send custom message to Splunk")
	@ApiResponses(value = {
			@ApiResponse(code = 202, message = "Custom message sucessfully sent to Splunk", examples = @io.swagger.annotations.Example(value = {
					@ExampleProperty(mediaType = "*/*", value = "Message successfully sent to Splunk") })),
			@ApiResponse(code = 400, message = "Empty message was supplied") })
	@GetMapping(path = SmlSharedConstants.DATA_TO_SPLUNK_URL)
	public ResponseEntity<String> dataToSplunk(
			@ApiParam(value = "Message to send send to Splunk", example = "Custom message", required = true) @RequestParam String message) {
		inCounter.incCounter();
		log.info("Incoming Request to send data to Splunk (IC: " + inCounter.getInfo().getCounter() + ")");

		if (message != null && !message.isEmpty()) {
			// Wrap message as an OutDataRes and then log it has a JSON
			OutDataRes outDataRes = logHelper.reqHelper(message);
			String strOutDataRes = stringUtil.mapToString(outDataRes);
			log.debug("Messsage: " + message);

			// Log JSON
			int outCount = logHelper.logOutResponse(strOutDataRes);
			log.info("Finished data to Splunk request (OC: " + outCount + ")");
		} else {
			log.error("Empty message string passed");
			throw new EmptyMessageException("Empty message string passed");
		}

		String res = "Message successfully sent to Splunk";

		return new ResponseEntity<>(res, HttpStatus.ACCEPTED);
	}

// ----------------------------------------------------------------------------------------------

	/**
	 * Extract application health metrics from actuator endpoints
	 * 
	 * @return - a ResponseEntity containing the status of the response
	 */
	@ApiOperation(value = "Extract and send application health metrics to Splunk")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Application health metrics sucessfully extracted and sent to Splunk"),
			@ApiResponse(code = 500, message = "Unexpected error extracting application health metrics") })

	@GetMapping(path = SmlSharedConstants.APPHEALTH_METRICS_URL)
	public ResponseEntity<String> apphealthmetrics() {
		inCounter.incCounter();
		log.info("Incoming Request to extract application metrics (IC: " + inCounter.getInfo().getCounter() + ")");

		String res = healthMetricService.sendApplicationHealth();
		return new ResponseEntity<>(res, HttpStatus.OK);
	}

//----------------------------------------------------------------------------------------------

	/**
	 * Extract database information using the given query
	 * 
	 * @param query        - String representing the SQL query to execute
	 * @param sendToSplunk - String representing a flag to send query results to
	 *                     Splunk
	 * @return - a ResponseEntity containing the status of the response
	 */
	@ApiOperation(value = "Execute SQL query and extract results")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Database query sucessfully extracted", examples = @io.swagger.annotations.Example(value = {
					@ExampleProperty(mediaType = "*/*", value = "[{\"TRANSACTION_COUNT\":11}]") })),
			@ApiResponse(code = 400, message = "Error with SQL query"),
			@ApiResponse(code = 500, message = "Unexpected error extracting SQL query") })
	@GetMapping(path = SmlSharedConstants.DATABASE_QUERIES_URL)
	public ResponseEntity<String> databaseQueries(
			@ApiParam(value = "SQL query to execute and extract", example = "SELECT COUNT(*) as Total FROM TX", required = true) @RequestParam String query,
			@ApiParam(value = "True to send query results to Splunk, otherwise only return the results", required = false) @RequestParam(defaultValue = "false") String sendToSplunk) {
		inCounter.incCounter();
		log.info("Incoming Request to extract query information (IC: " + inCounter.getInfo().getCounter() + ")");
		log.debug("Query: " + query);
		log.debug("Send to splunk flag: " + sendToSplunk);

		boolean boolSendToSplunk = false;

		// Get the query results from the Database
		// Sends queryResults to Splunk if flag is set to true or y
		if ("true".equalsIgnoreCase(sendToSplunk) || "y".equalsIgnoreCase(sendToSplunk))
			boolSendToSplunk = true;

		List<String> queryResults = serviceHealthMetricService.getDatabaseDetails(query, boolSendToSplunk);
		return new ResponseEntity<>(queryResults.toString(), HttpStatus.OK);
	}

//----------------------------------------------------------------------------------------------

	/**
	 * Extracts container metric details
	 * 
	 * @param sendToSplunk - String representing a flag to send query results to
	 *                     Splunk
	 * @return- a response entity containing the status of response and the
	 *          resulting container metric details
	 */
	@ApiOperation(value = "Extract server container metrics")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Container health metrics sucessfully extracted"),
			@ApiResponse(code = 500, message = "Unexpected error extracting container health metrics") })
	@GetMapping(path = SmlSharedConstants.CONTAINERHEALTH_METRICS_URL)
	public ResponseEntity<String> containerhealthmetrics(
			@ApiParam(value = "True to send query results to Splunk, otherwise only return the results", required = false) @RequestParam(defaultValue = "false") String sendToSplunk) {
		inCounter.incCounter();
		log.info("Incoming Request to extract container health metrics (IC: " + inCounter.getInfo().getCounter() + ")");

		boolean boolSendToSplunk = false;

		// Sends container metric details to Splunk if flag is set to true or y
		if ("true".equalsIgnoreCase(sendToSplunk) || "y".equalsIgnoreCase(sendToSplunk)) {
			boolSendToSplunk = true;
		}

		String res = containerMetricsService.sendContainerMetrics(boolSendToSplunk);

		return new ResponseEntity<>(res, HttpStatus.OK);
	}

//----------------------------------------------------------------------------------------------

	/**
	 * Extract log entries from filePath with specified searchKeywords and send send
	 * them to Splunk if sendToSplunk is true. Return limit number of logs in the
	 * REST call.
	 * 
	 * @param filePath       - String representing the path to the log file
	 * @param searchKeywords - Comma separated String representing the keywords to
	 *                       filter log entries
	 * @return - a ResponseEntity containing the status of the response and the
	 *         String representation of a list of extracted logs
	 */
	@ApiOperation(value = "Extract logs and send to Splunk")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Successful log extraction", examples = @io.swagger.annotations.Example(value = {
					@ExampleProperty(mediaType = "*/*", value = "Logs successfully extracted and sent to Splunk") })),
			@ApiResponse(code = 400, message = "Error from reading log file at filePath"),
			@ApiResponse(code = 500, message = "Unexpected error extracting logs from filePath") })
	@GetMapping(path = SmlSharedConstants.LOG_SUMMARY_URL)
	public ResponseEntity<String> logExtractor(
			@ApiParam(value = "Absolute path to the log file to extract logs from", example = "C:/logs/catalina.log", required = true) @RequestParam String filePath,
			@ApiParam(value = "Comma separated list of keywords to search logs for and retain", example = "brave.tracing", required = true) @RequestParam String searchKeywords,
			@ApiParam(value = "Comma separated list of keywords to filter logs with and exclude", example = "POSService", required = false) @RequestParam(defaultValue = "") String excludeKeywords) {
		inCounter.incCounter();
		log.info(String.format("Incoming Request to extract log data (IC: %d)", inCounter.getInfo().getCounter()));

		logSummaryService.sendLogData(filePath, searchKeywords, excludeKeywords);

		String res = "Logs successfully extracted and sent to Splunk";

		return new ResponseEntity<>(res, HttpStatus.OK);
	}
}
