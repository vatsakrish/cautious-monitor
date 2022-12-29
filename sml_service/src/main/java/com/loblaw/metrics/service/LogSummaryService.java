package com.loblaw.metrics.service;

public interface LogSummaryService {

	void scheduledSendLogData();

	void sendLogData(String filePath, String searchKeyword, String excludeKeywords);
}
