package com.loblaw.metrics.service;

import com.loblaw.metrics.model.CounterInfo;

/**
 * Interface for MetricInfoService
 */
public interface CounterInfoService {

	void incCounter();

	CounterInfo getInfo();
}
