package com.loblaw.metrics.model;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.ApplicationScope;

import lombok.Data;

@Component 
@ApplicationScope
@Data
public class LastRun {
	private String lastDbDateTime;
	private String lastLogDateTime;
}