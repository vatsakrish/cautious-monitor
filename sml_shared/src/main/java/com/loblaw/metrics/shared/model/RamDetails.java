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
public class RamDetails {
	@JsonProperty("available_ram")
	String availableRam;
	@JsonProperty("used_ram")
	String usedRam;
	@JsonProperty("total_ram")
	String totalRam;
	@JsonProperty("buffers_ram")
	String buffersRam;
	@JsonProperty("cached_ram")
	String cachedRam;
}
