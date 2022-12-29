package com.loblaw.metrics.shared.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CpuDetails {
	private String user;
	private String nice;
	private String sys;
	private String idle;
	@JsonProperty("io_wait")
	private String ioWait;
	private String irq;
	@JsonProperty("soft_irq")
	private String softIrq;
	private String steal;
}
