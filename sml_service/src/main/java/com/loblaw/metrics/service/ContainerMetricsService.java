package com.loblaw.metrics.service;

public interface ContainerMetricsService {
	String sendContainerMetrics(boolean sendToSplunk);

	void scheduledSendContainerMetrics();
}
