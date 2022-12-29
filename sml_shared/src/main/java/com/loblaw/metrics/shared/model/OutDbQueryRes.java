package com.loblaw.metrics.shared.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class OutDbQueryRes {
	private String dateTime;
	private String uniqueid;
	private String store;
	private Integer responseCode;
	private Integer responseTime;
	private String methodName; 
	private String queryResults;	
}