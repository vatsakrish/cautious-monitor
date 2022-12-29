package com.loblaw.metrics.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.loblaw.metrics.exception.UnexpectedLogExtractionException;
import com.loblaw.metrics.helper.LogExtractionHelper;
import com.loblaw.metrics.model.TempLastRun;
import com.loblaw.metrics.service.LogSummaryService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author brytung
 *
 */
@Service
@Slf4j
public class LogSummaryServiceImpl implements LogSummaryService {

	@Autowired
	private LogExtractionHelper logExtractionHelper;

	@Override
	@Scheduled(cron = "${log.cron-expression}")
	/**
	 * For every interval given by log.cron-expression, extract logs given in the
	 * application properties, and sends each line as a message to Splunk
	 */
	public void scheduledSendLogData() {
		try {
			TempLastRun tempLastRun = new TempLastRun();
			int numSuccExtractions = logExtractionHelper.extractLogs(tempLastRun);

			if (numSuccExtractions > 0) {
				logExtractionHelper.updateLastRun(tempLastRun);
				logExtractionHelper.updateCounterInfo();
			}
		} catch (Exception e) {
			log.error("Log Extraction : Unexpected exception in scheduledSendLogData : {}", e.toString(), e);
		}
	}

	@Override
	/**
	 * Extracts logs from filePath, filters them through searchKeyword, and sends
	 * the results to Splunk
	 * 
	 * @param fielPath        - String representing the path to the log file to be
	 *                        extracted
	 * @param searchKeywords  - a Comma separated String representing the keywords
	 *                        to filter for
	 * @param excludeKeywords - String representing list of comma separated keywords
	 *                        to avoid
	 */
	public void sendLogData(String filePath, String searchKeywords, String excludeKeywords) {
		try {
			String[] excludeStrings = excludeKeywords.isEmpty() ? new String[0] : excludeKeywords.split(",");
			String[] searchStrings = searchKeywords.split(",");

			int numSuccExtractions = logExtractionHelper.extractLogs(filePath, searchStrings, excludeStrings);

			if (numSuccExtractions == 1)
				logExtractionHelper.updateCounterInfo();

		} catch (Exception e) {
			log.error("Log Extraction : Unexpected exception in sendLogData : {}", e.toString(), e);
			throw new UnexpectedLogExtractionException("Error extracting log data from REST call - " + e.getMessage());
		}
	}
}
