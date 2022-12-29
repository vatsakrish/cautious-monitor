package com.loblaw.metrics.service;

public interface HealthMetricService {
	String sendApplicationHealth();

	void scheduledSendApplicationHealth();

}
