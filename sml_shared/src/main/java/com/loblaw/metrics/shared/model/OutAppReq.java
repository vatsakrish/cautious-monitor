package com.loblaw.metrics.shared.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * POJO to output the metrics
 * 
 * @author srsridh
 *
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class OutAppReq implements Serializable {

	// Default Serial Version UID
	private static final long serialVersionUID = 1L;
	private String dateTime;
	private String uniqueid;
	private String projectName;
	private String host;
	private String province;
	private Integer responseCode;
	private Integer responseTime;
	private String methodName;
	private String data;
	private String requestId;
	//Adding health object for tracking store health
	@JsonProperty("app_status")
	private String appStatus;
	private String buildVersion;
	@JsonProperty("IP")
	private String ipAddress;
	@JsonProperty("service_status")
	private String serviceStatus;
}
