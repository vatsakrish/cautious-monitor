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
public class ContainerProcess {
	String user;
	String name;
	@JsonProperty("memory_usage")
	String memoryUsage;
	@JsonProperty("cpu_usage")
	String cpuUsage;
}