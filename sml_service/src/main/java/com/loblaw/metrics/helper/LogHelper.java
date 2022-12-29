/**
 * 
 */
package com.loblaw.metrics.helper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.loblaw.metrics.config.SmlProperties;
import com.loblaw.metrics.service.CounterInfoService;
import com.loblaw.metrics.shared.model.AppReq;
import com.loblaw.metrics.shared.model.OutAppReq;
import com.loblaw.metrics.shared.model.OutContainerRes;
import com.loblaw.metrics.shared.model.OutDataRes;
import com.loblaw.metrics.shared.model.OutDbQueryRes;
import com.loblaw.metrics.shared.model.OutLogRes;

/**
 * Util Helper to help manage the model transformation
 * 
 * @author srsridh
 *
 */
@Component
public class LogHelper {

	private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

	private String date = null;
	private String uniqueId = null;
	private String hostName = null;
	private String ipAddress = null;

	@Autowired
	private SmlProperties smlProperties;

	@Autowired
	private AppRequestMapper mapper;

	@Autowired
	private Logger logger;

	@Autowired
	@Qualifier("in-counter")
	private CounterInfoService inCounter;

	@Autowired
	@Qualifier("out-counter")
	private CounterInfoService outCounter;

	/**
	 * Map appReq to an OutAppReq response with additional attributes
	 * 
	 * @param appReq - AppReq to map to an OutAppReq
	 * @return - an OutAppReq representing the transformed appReq
	 */
	public OutAppReq reqHelper(AppReq appReq) {
		// Map appReq to outAppReq
		OutAppReq outAppReq = mapper.appReqToOutAppReq(appReq);

		setDateIdStore();

		// Calculated Fields
		outAppReq.setDateTime(date);
		outAppReq.setUniqueid(uniqueId);
		outAppReq.setHost(hostName);
		outAppReq.setIpAddress(ipAddress);

		return outAppReq;
	}

	/**
	 * Converts a message into a OutDataRes
	 * 
	 * @param message - a String message to be attached to a response
	 * @return - an OutDataRes representing a message wrapped with additional
	 *         details
	 */
	public OutDataRes reqHelper(String message) {
		OutDataRes outDataRes = new OutDataRes();

		setDateIdStore();

		// Calculated Fields
		outDataRes.setDateTime(date);
		outDataRes.setUniqueid(uniqueId);
		outDataRes.setStore(hostName);
		outDataRes.setMessage(message);

		return outDataRes;
	}

	/**
	 * Takes a list of queryResults and wraps it as an OutDbQueryRes with additional
	 * fields for time, unique id, store number, and build version
	 * 
	 * @param queryResults - List of String representing results of a database query
	 * @param methodName   - String representing the query that was run
	 * @return - an OutDbQueryRes representing database query results wrapped with
	 *         additional details
	 */
	public OutDbQueryRes reqHelper(List<String> queryResults, String methodName, int responseCode, long elapsedTime) {
		OutDbQueryRes outDbQueryRes = new OutDbQueryRes();

		setDateIdStore();

		// Calculated Fields
		outDbQueryRes.setDateTime(date);
		outDbQueryRes.setUniqueid(uniqueId);
		outDbQueryRes.setStore(hostName);

		outDbQueryRes.setQueryResults(queryResults.toString());
		outDbQueryRes.setMethodName(methodName);
		outDbQueryRes.setResponseTime((int) elapsedTime);
		outDbQueryRes.setResponseCode(responseCode);

		return outDbQueryRes;
	}

	/**
	 * Maps a message, filePath, and searchKeyword to an OutLogRes
	 * 
	 * @param message       - String representing the message of a log
	 * @param filePath      - String representing the path to the log
	 * @param searchKeyword - String representing the keyword to filter the log by
	 * @return - a OutLogRes representing the log message response
	 */
	public OutLogRes logReqHelper(String message, String filePath) {
		OutLogRes outLogRes = new OutLogRes();

		setDateIdStore();

		// Calculated Fields
		outLogRes.setDateTime(date);
		outLogRes.setStore(hostName);
		outLogRes.setMessage(message);
		outLogRes.setFileName(filePath);

		return outLogRes;
	}

	/**
	 * Update outContainerRes with methodName, date and store
	 * 
	 * @param outContainerRes - OutContainerRes representing container metrics
	 *                        response to update
	 * @param methodName      - String name of the type of response
	 */
	public void updateOutContainerRes(OutContainerRes outContainerRes, String methodName) {
		setDateIdStore();

		// Calculated Fields
		outContainerRes.setMethodName(methodName);
		outContainerRes.setDateTime(date);
		outContainerRes.setStore(hostName);
	}

	/**
	 * Set static variables of date to the current time, uniqueId to a random unique
	 * id, store to the current store number, and buildVersion to the store's
	 * extracted build version
	 */
	private void setDateIdStore() {
		date = dateFormat.format(LocalDateTime.now());
		uniqueId = UUID.randomUUID().toString();
		hostName = smlProperties.getHostName();
		ipAddress = smlProperties.getIpAddress();
	}

	/**
	 * Given a message, log its contents into a file to be picked up by a Splunk
	 * forwarder and increment counter
	 * 
	 * @param message - String representing log message
	 * @return - integer representing the count of the number of messages sent
	 */
	public int logOutResponse(String message) {
		// logs message to file and increments outCounter
		logger.info(message);
		outCounter.incCounter();

		return outCounter.getInfo().getCounter();
	}

	/**
	 * Given a message, log its contents into a file to be picked up by a Splunk
	 * forwarder and increment counter if incCounter is true
	 * 
	 * @param message    - String representing log message
	 * @param incCounter - set boolean to true to increment counter, false otherwise
	 * @return - integer representing the count of the number of messages sent
	 */
	public int logOutResponse(String message, boolean incCounter) {
		// logs message to file and increments outCounter
		logger.info(message);

		if (incCounter)
			outCounter.incCounter();

		return outCounter.getInfo().getCounter();
	}

	/**
	 * Increment the inCounter and return its count
	 * 
	 * @return - integer representing the count of operations started
	 */
	public int incInResponse() {
		inCounter.incCounter();

		return inCounter.getInfo().getCounter();
	}

	/**
	 * Increment the outCounter and return its count
	 * 
	 * @return - integer representing the count of operations sent out
	 */
	public int incOutResponse() {
		outCounter.incCounter();

		return outCounter.getInfo().getCounter();
	}
}
