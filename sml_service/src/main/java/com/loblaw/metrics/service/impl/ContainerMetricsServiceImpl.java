package com.loblaw.metrics.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.loblaw.metrics.exception.UnexpectedContainerMetricsException;
import com.loblaw.metrics.helper.ContainerMetricsHelper;
import com.loblaw.metrics.helper.LogHelper;
import com.loblaw.metrics.service.ContainerMetricsService;
import com.loblaw.metrics.shared.model.OutContainerRes;
import com.loblaw.metrics.shared.util.StringUtil;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ContainerMetricsServiceImpl implements ContainerMetricsService {

	@Autowired
	ContainerMetricsHelper containerMetricsHelper;

	@Autowired
	LogHelper logHelper;
	
	@Autowired
	StringUtil stringUtil;

	private static final String SCHEDULED_METHOD_NAME = "scheduled_container_metrics";
	private static final String METHOD_NAME = "container_metrics";

	/**
	 * Scheduled extraction of container metric details to Splunk
	 */
	@Override
	@Scheduled(cron = "${container-metrics.cron-expression}")
	public void scheduledSendContainerMetrics() {
		int inCount = logHelper.incInResponse();
		log.info(String.format("Scheduled extracting of container metrics (IC: %d)", inCount));

		try {
			getContainerMetrics(true, SCHEDULED_METHOD_NAME);
		} catch (Exception e) {
			log.error("Container Metrics : Unexpected exception in scheduledSendContainerMetrics : {}", e.toString(), e);
		}

		log.info("Finished scheduled extraction of container metrics");
	}

	/**
	 * Extract container metric details, setting response methodName and sending
	 * results to Splunk according to sendToSplunk
	 * 
	 * @param methodName   - String value representing what to name the response
	 *                     method name
	 * @param sendToSplunk - boolean value representing whether or not to send
	 *                     results to Splunk
	 */
	@Override
	public String sendContainerMetrics(boolean sendToSplunk) {
		log.info("Starting to get container metrics");

		String containerMetricsJson = "";

		try {
			OutContainerRes containerMetrics = getContainerMetrics(sendToSplunk, METHOD_NAME);
			containerMetricsJson = stringUtil.mapToString(containerMetrics);

		} catch (Exception e) {
			log.error("Container Metrics : Unexpected exception in sendContainerMetrics : {}", e.toString(), e);
			throw new UnexpectedContainerMetricsException(
					"Error extracting container metrics from server on REST call");
		}
		log.info("Finished extracting container metrics");

		return containerMetricsJson;
	}

	/**
	 * Extracts container metrics and sendToSplunk if true
	 * 
	 * @param sendToSplunk - boolean value set to true to send to Splunk, false
	 *                     otherwise
	 * @return - ContainerMetrics extracted from the server
	 */
	private OutContainerRes getContainerMetrics(boolean sendToSplunk, String methodName) {
		// Extract container metrics
		OutContainerRes outContainerMetrics = containerMetricsHelper.getServerDetails();
		logHelper.updateOutContainerRes(outContainerMetrics, methodName);

		if (sendToSplunk && outContainerMetrics != null) {
			// Send results to Splunk
			log.info("Sending container metrics to Splunk");
			containerMetricsHelper.sendContainerMetricsToSplunk(outContainerMetrics);
		}

		return outContainerMetrics;
	}
}
