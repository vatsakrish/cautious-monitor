package com.loblaw.metrics.service.impl;

import com.loblaw.metrics.model.CounterInfo;
import com.loblaw.metrics.service.CounterInfoService;

public class CounterInfoServiceImpl implements CounterInfoService {

	private final CounterInfo info = new CounterInfo();

	public synchronized void incCounter() {
		info.incCounter();
	}

	public synchronized CounterInfo getInfo() {
		return new CounterInfo(info);
	}
}
