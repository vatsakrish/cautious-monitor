package com.loblaw.metrics.shared.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class OutFileStatus {
	private String dateTime;
	private String store;
	private Boolean exists;
}
