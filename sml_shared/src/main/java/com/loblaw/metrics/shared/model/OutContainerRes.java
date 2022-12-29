package com.loblaw.metrics.shared.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class OutContainerRes {
	private String dateTime;
	private String store;
	private String methodName;
	@JsonProperty("soft_start_time")
	private String softStartTime;
	@JsonProperty("load_average")
	private LoadAverage loadAverage;
	@JsonProperty("cpu_details")
	private CpuDetails cpuDetails;
	@JsonProperty("ram_details")
	private RamDetails ramDetails;
	@JsonProperty("disk_utilization")
	private Object diskUtilization;
	@JsonProperty("top_five_processes")
	private List<ContainerProcess> topFiveProcesses;
}
