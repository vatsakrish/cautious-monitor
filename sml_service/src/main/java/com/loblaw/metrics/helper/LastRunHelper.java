package com.loblaw.metrics.helper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.loblaw.metrics.shared.util.FileUtil;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class LastRunHelper {

	@Autowired
	FileUtil fileUtil;

	/**
	 * Get the default last run time from lastRunFile
	 * 
	 * @param lastRunFile    - String representing the path to the file containing
	 *                       the last run date
	 * @param dateTimeFormat - String representing the format of the date
	 * @param defaultTime    - String representing the defaulTime to set the file if
	 *                       it does not exist
	 * @return a String representing the last time a schedule was run
	 */
	public String getDefaultLastRun(String lastRunFile, String dateTimeFormat, String defaultTime) {
		String line = "";
		DateTimeFormatter format = DateTimeFormatter.ofPattern(dateTimeFormat);

		List<String> lines = new ArrayList<>();
		int ret = fileUtil.readFile(lastRunFile, lines);

		// Read the first line in lastRunFile to get the default last run time
		if (ret > 0 && lines.size() == 1) {
			line = lines.get(0);

			try {
				format.parse(line);
			} catch (DateTimeParseException e) {
				// Catches DateTimeParseException and create lastRuFile with defaultTime
				log.error("Last Run Helper : Date parse exception in getDefaultLastRun : {}", e.toString(), e);
				setDefaultLastRun(LocalDateTime.parse(defaultTime, format), lastRunFile, dateTimeFormat);
				line = defaultTime;
			} catch (Exception e) {
				log.error("Last Run Helper : Unexpected exception in getDefaultLastRun : {}", e.toString(), e);
				setDefaultLastRun(LocalDateTime.parse(defaultTime, format), lastRunFile, dateTimeFormat);
			}
		}
		// Create the lastRunFile and populate it with the defaultTime if it is empty
		else {
			log.debug(String.format("%s is empty, setting to: %s", lastRunFile, defaultTime));
			setDefaultLastRun(LocalDateTime.parse(defaultTime, format), lastRunFile, dateTimeFormat);
			line = defaultTime;
		}

		return line;
	}

	/**
	 * Set the default last run time to the current time
	 * 
	 * @param lastRunFile    - String representing the path to the file containing
	 *                       the last run date
	 * @param dateTimeFormat - String representing the format of the date
	 */
	public void setDefaultLastRun(String lastRunFile, String dateTimeFormat) {
		setDefaultLastRun(LocalDateTime.now(), lastRunFile, dateTimeFormat);
	}

	/**
	 * Set the default last run time to time
	 * 
	 * @param time           - a LocalDateTime representing the time to set last run
	 * @param lastRunFile    - String representing the path to the file containing
	 *                       the last run date
	 * @param dateTimeFormat - String representing the format of the date
	 */
	public void setDefaultLastRun(LocalDateTime time, String lastRunFile, String dateTimeFormat) {
		DateTimeFormatter format = DateTimeFormatter.ofPattern(dateTimeFormat);
		List<String> messages = new ArrayList<>();

		log.info(String.format("Saving to %s the new last db run time: %s", lastRunFile, time.format(format)));
		messages.add(time.format(format));

		// Write the current date time to the LAST_RUN_FILE
		int res = fileUtil.writeToFile(lastRunFile, messages);

		if (res < 0) {
			log.error("Error writing new default last run time to file");
		}
	}
}