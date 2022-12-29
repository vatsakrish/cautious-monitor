package com.loblaw.metrics.config;

import java.net.InetAddress;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Storemetrics Logging properties
 */

@Data
@Slf4j
@Component
@ConfigurationProperties("sml")
public class SmlProperties {
	// host-name
	private String hostName;

	// ip-address
	private String ipAddress;

	public SmlProperties() {
		// Try extract store number from host name. Using zero if it's running from test
		// environment
		hostName = getHostNameId();
		ipAddress = getIpAddressNum();

		log.info("Auto Detected host name " + hostName + " and IP address " + ipAddress);
	}

	@SneakyThrows
	public static String getHostNameId() {
		return InetAddress.getLocalHost().getHostName();
	}

	@SneakyThrows
	public static String getIpAddressNum() {
		return InetAddress.getLocalHost().getHostAddress();
	}
}
