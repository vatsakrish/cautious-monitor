package com.loblaw.metrics.model;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metric info for /info endpoint
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CounterInfo {
	private int counter;

	private Date lastProcessed;

	public CounterInfo(CounterInfo counterInfo) {
		this.counter = counterInfo.counter;

		if (counterInfo.lastProcessed != null)
			this.lastProcessed = new Date(counterInfo.lastProcessed.getTime());
	}

	public void incCounter() {
		counter++;
		lastProcessed = new Date();
	}
}
