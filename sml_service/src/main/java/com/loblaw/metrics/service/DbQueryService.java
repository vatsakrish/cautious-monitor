package com.loblaw.metrics.service;

import java.util.List;

public interface DbQueryService {
	List<String> getDatabaseDetails(String query, boolean sendToSplunk);

	void shortTermScheduledDatabase();

	void longTermScheduledDatabase();
}
