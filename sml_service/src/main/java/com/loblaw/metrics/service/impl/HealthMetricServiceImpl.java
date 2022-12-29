package com.loblaw.metrics.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.loblaw.metrics.config.SmlProperties;
import com.loblaw.metrics.exception.UnexpectedApplicationHealthException;
import com.loblaw.metrics.helper.HealthHelper;
import com.loblaw.metrics.helper.LogHelper;
import com.loblaw.metrics.service.HealthMetricService;
import com.loblaw.metrics.shared.model.AppReq;
import com.loblaw.metrics.shared.model.OutAppReq;
import com.loblaw.metrics.shared.model.OutDataRes;
import com.loblaw.metrics.shared.util.StringUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation to help extract application health status
 * 
 * @author brytung
 *
 */
@Service
@Slf4j
public class HealthMetricServiceImpl implements HealthMetricService {
	private static final String APP_HEALTH_METHOD_NAME = "app_health";
	private static final String SCHEDULED_APP_HEATLH_METHOD_NAME = "scheduled_app_health";
	private static final String APPLICATION_ENDPOINT_WITH_STATUS_TOTAL = "application.endpoint.with-status.total";
	private static final String APPLICATION_ENDPOINT_WITHOUT_STATUS_TOTAL = "application.endpoint.without-status.total";
	private static final String PROJECT_NAME = "Application_Health_Metrics";

	@Autowired
	private HealthHelper healthHelper;
	@Autowired
	private Environment env;
	@Autowired
	private LogHelper logHelper;
	@Autowired
	private SmlProperties smlProperties;
	@Autowired
	private StringUtil stringUtil;

	/**
	 * Get the health status of applications, and log their results
	 * 
	 * @return - a String representing the application health statuses as a JSON
	 */
	public String sendApplicationHealth() {
		String strAppData = "";
		try {
			strAppData = getApplicationHealthJson(APP_HEALTH_METHOD_NAME);

			log.debug("Application Health: " + strAppData);
			int outCount = logHelper.logOutResponse(strAppData);
			log.info(String.format("Finished extraction of application health metrics (OC: %d)", outCount));
			return strAppData;
		} catch (Exception e) {
			log.error("Application Health : Unexpected exception in sendApplicationHealth : {}", e.toString(), e);
			throw new UnexpectedApplicationHealthException("Error extracting application health from REST call");
		}
	}

	/**
	 * On a scheduled basis, get the health status of applications, and log their
	 * results
	 */
	@Scheduled(cron = "${application.cron-expression}")
	public void scheduledSendApplicationHealth() {
		int inCount = logHelper.incInResponse();
		log.info(String.format("Starting scheduled extraction of application health (IC: %d)", inCount));
		String strAppData = "";
		try {
			strAppData = getApplicationHealthJson(SCHEDULED_APP_HEATLH_METHOD_NAME);
		} catch (Exception e) {
			log.error("Application Health : Unexpected exception in scheduledSendApplicationHealth : {}", e.toString(),
					e);
			strAppData = "Error extracting application health status from host " + smlProperties.getHostName();

			OutDataRes outDataRes = logHelper.reqHelper(strAppData);
			strAppData = stringUtil.mapToString(outDataRes);
		}

		int outCount = logHelper.logOutResponse(strAppData);
		log.info(String.format("Finished scheduled extraction of application health metrics (OC: %d)", outCount));
		log.debug("Application Health: " + strAppData);
	}

	/**
	 * Extracts application health and service status, then returns it as a
	 * JSON
	 * 
	 * @param methodName - String representing type of content being extracted
	 * @return - JSON representation of an OutAppReq containing application health
	 *         and service details
	 */
	private String getApplicationHealthJson(String methodName) {
		String serviceStatus = null;

		// Start time to calculate processing time
		long startTime = System.currentTimeMillis();

		AppReq appReq = getApplicationHealth();
		serviceStatus = healthHelper.getServiceHealth();

		// Calculate time taken to process REST calls and Service status
		long finishTime = System.currentTimeMillis();
		long elapsedTime = finishTime - startTime;

		log.debug("Total time for processing all the endpoints is: " + elapsedTime);
		appReq.setResponseTime((int) elapsedTime);
		appReq.setMethodName(methodName);
		appReq.setServiceStatus(serviceStatus);

		OutAppReq outAppReq = logHelper.reqHelper(appReq);

		return stringUtil.mapToString(outAppReq);
	}

	/**
	 * Iterates through lists of health check end points and extracts their
	 * application status, HTTP status code, and error messages.
	 * 
	 * @return - a AppReq composed of information extracted from health checks.
	 */
	private AppReq getApplicationHealth() {
		AppReq appReq = new AppReq();
		Integer numWithStatus = 0;
		Integer numWithoutStatus = 0;
		String health = "{";
		// Extracts the number of end points with statuses and without statuses to
		// iterate through
		if (env.containsProperty(APPLICATION_ENDPOINT_WITH_STATUS_TOTAL)) {
			numWithStatus = stringUtil.parseInt(env.getProperty(APPLICATION_ENDPOINT_WITH_STATUS_TOTAL));
			if (numWithStatus == null)
				numWithStatus = 0;
		}

		if (env.containsProperty(APPLICATION_ENDPOINT_WITHOUT_STATUS_TOTAL)) {
			numWithoutStatus = stringUtil.parseInt(env.getProperty(APPLICATION_ENDPOINT_WITHOUT_STATUS_TOTAL));
			if (numWithoutStatus == null) {
				numWithoutStatus = 0;
			}
		}

		List<String> applicationHealthWithStatus = new ArrayList<>();
		List<String> applicationHealthWithoutStatus = new ArrayList<>();

		appReq.setProjectName(PROJECT_NAME);

		// Iterate through application health end points with statuses
		for (int i = 1; i <= numWithStatus; i++) {
			applicationHealthWithStatus.add("with-status." + i);
		}

		log.info("Starting to extract application health endpoints with statuses");
		health += healthHelper.extractHealth(applicationHealthWithStatus);
		log.info("Finished extracting application health endpoints with statuses");

		// Iterate through application health end points without statuses
		for (int i = 1; i <= numWithoutStatus; i++) {
			applicationHealthWithoutStatus.add("without-status." + i);
		}

		log.info("Starting to extract application health endpoints without statuses");
		health += healthHelper.extractHealth(applicationHealthWithoutStatus);
		log.info("Finished extracting application health endpoints without statuses");

		// Remove extra trailing commas
		health = health.replaceAll(", $", "");
		health += "}";
		appReq.setAppStatus(health);

		return appReq;
	}
}
