package com.loblaw.metrics.shared.util;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DateValidator {
	private DateTimeFormatter formatter;

	/**
	 * Validate whether date is formatted according to formatter
	 * 
	 * @param date - String representing date to check format of
	 * @return - true if date is formated correctly, false otherwise
	 */
	public boolean isValid(String date) {
		boolean ret = false;
		try {
			formatter.parse(date);
			ret = true;
		} catch (DateTimeParseException e) {
			ret = false;
		} catch (Exception e) {
			return false;
		}
		return ret;
	}
}
