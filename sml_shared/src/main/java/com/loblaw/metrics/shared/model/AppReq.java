/**
 *
 */
package com.loblaw.metrics.shared.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Incoming DOA for receiving data from other projects
 * 
 * @author srsridh
 *
 */

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppReq {
	@ApiModelProperty(required = true, value = "String value for project name", name = "projectName", dataType = "String", example = "CloudStorage")
	@NotNull
	private String projectName;
	@ApiModelProperty(required = true, value = "String value for method name", name = "methodName", dataType = "String", example = "get")
	@NotNull
	private String methodName;
	@ApiModelProperty(value = "String value request identifier", name = "requestId", dataType = "String", example = "18295")
	private String requestId;
	@ApiModelProperty(value = "String value for province", name = "province", dataType = "String", example = "ON")
	private String province;
	@ApiModelProperty(value = "Integer value for response code", name = "responseCode", dataType = "Integer", example = "200")
	private Integer responseCode;
	@ApiModelProperty(value = "Integer value for response time", name = "responseTime", dataType = "Integer", example = "253")
	private Integer responseTime;
	@ApiModelProperty(required = true, value = "String value for miscellaneous data", name = "data", dataType = "String", example = "256")
	@NotNull
	private String data;
	@ApiModelProperty(hidden = true)
	private String appStatus;
	@ApiModelProperty(hidden = true)
	private String serviceStatus;
}
