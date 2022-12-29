package com.loblaw.metrics.shared.util;

public class NumberUtil {
	/**
	 * Convert from bytes to gigabytes
	 * 
	 * @param bytes - long representing the number of bytes
	 * @return - double representing the converted value from bytes to gigabytes
	 */
	public double bytesToGb(long bytes) {
		// Convert to gigabytes by dividing by 2^30
		return bytes / Math.pow(2, 30);
	}
	
	/**
	 * Convert from kilobytes to gigabytes
	 * 
	 * @param kilobytes - long representing the number of kilobytes
	 * @return - double representing the converted value from kilobytes to gigabytes
	 */
	public double kilobytesToGb(long kilobytes) {
		// Convert to gigabytes by dividing by 2^20
		return kilobytes / Math.pow(2, 20);
	}
}
