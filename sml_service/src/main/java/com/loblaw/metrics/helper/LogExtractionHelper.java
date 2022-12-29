package com.loblaw.metrics.helper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.loblaw.metrics.exception.FileReaderException;
import com.loblaw.metrics.model.LastRun;
import com.loblaw.metrics.model.TempLastRun;
import com.loblaw.metrics.shared.model.OutLogRes;
import com.loblaw.metrics.shared.util.DateValidator;
import com.loblaw.metrics.shared.util.FileUtil;
import com.loblaw.metrics.shared.util.StringUtil;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class LogExtractionHelper {

	@Autowired
	private Environment env;

	@Autowired
	private LastRun lastRun;

	@Autowired
	private LastRunHelper lastRunHelper;

	@Autowired
	private LogHelper logHelper;

	@Autowired
	private StringUtil stringUtil;

	@Autowired
	private FileUtil fileUtil;

	@Value("${log.last-run-file}")
	private String lastRunFile;

	private static final String DEFAULT_TIME = "01-Jan-2020 00:00:00.000";
	private static final String DATE_TIME_FORMAT = "dd-MMM-yyyy HH:mm:ss.SSS";
	private static final String REPLACEMENT_PATTERN = "(<<.*?>>)";
	private static final String LOG_FILE = "log.file.";
	private static final String LOG_FILE_TOTAL = "total";
	private static final String LOG_FILE_PATH = ".file-path";
	private static final String LOG_FILE_SEARCH = ".search";
	private static final String LOG_FILE_EXCLUDE = ".exclude";
	private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
	private static DateValidator dateValidator = new DateValidator(formatter);

	/**
	 * Extracts logs from files given in the properties file containing a
	 * searchString.
	 * 
	 * @param tempLastRun TempLastRun object used to keep track of the latest time
	 *                    stamp encountered in the logs
	 * @return int count of number of successful log extractions
	 */
	public int extractLogs(TempLastRun tempLastRun) {
		log.info("Starting scheduled log extraction (IC: {})", logHelper.incInResponse());

		String logPathTotal = LOG_FILE + LOG_FILE_TOTAL;
		Integer numLogs = 0;
		int numSuccExtractions = 0;
		boolean filterDate = true;

		if (env.containsProperty(logPathTotal))
			numLogs = stringUtil.parseInt(env.getProperty(logPathTotal));
		if (numLogs == null)
			numLogs = 0;

		// Iterate through list of files to extract logs from
		for (int i = 1; i <= numLogs; i++) {
			String logPathLocation = LOG_FILE + i + LOG_FILE_PATH;
			String logSearchLocation = LOG_FILE + i + LOG_FILE_SEARCH;
			String logExcludeLocation = LOG_FILE + i + LOG_FILE_EXCLUDE;

			// Ensure environment properties exist
			if (env.containsProperty(logPathLocation) && env.containsProperty(logSearchLocation)) {
				// Extract file path and split comma separated keywords to obtain list of search
				// keywords
				String filePath = env.getProperty(logPathLocation);
				String[] searchStrings = env.getProperty(logSearchLocation).split(",");
				String[] excludeStrings = env.containsProperty(logExcludeLocation)
						? env.getProperty(logExcludeLocation).split(",")
						: new String[0];

				log.debug("Extracting logs from: " + filePath);
				log.debug("Text to filter for is: " + Arrays.toString(searchStrings));
				log.debug("Text to exclude is: " + Arrays.toString(excludeStrings));

				// Replace date format given between << and >> with the current date time
				try {
					Matcher m = Pattern.compile(REPLACEMENT_PATTERN).matcher(filePath);
					if (m.find()) {
						DateTimeFormatter fileNameFormat = DateTimeFormatter
								.ofPattern(m.group(1).replaceAll("<|>", ""));
						filePath = filePath.replace(m.group(1), LocalDate.now().format(fileNameFormat));
					}

					Path path = Paths.get(filePath);
					List<String> firstLineList = new ArrayList<>();

					// Extract the first line from filePath
					if (fileUtil.readFile(filePath, firstLineList, 1) == 1) {
						String firstLine = firstLineList.get(0);

						// Do not consider last run time if first line does not start with a valid date
						if (firstLine.length() <= 24 || !dateValidator.isValid(firstLine.substring(0, 24))) 
							filterDate = false;
					} else
						throw new FileReaderException("File Util : Read File exception");

					numSuccExtractions += extractCurrentDate(path, searchStrings, excludeStrings, tempLastRun,
							filterDate);
				} catch (UnsupportedTemporalTypeException e) {
					log.error("Log Extraction Helper : Date parse exception in extractLogs : {}", e.toString(), e);
				} catch (Exception e) {
					log.error("Log Extraction Helper : Unexpected exception in extractLogs : {}", e.toString(), e);
				}
			} else {
				log.info("Log File path or search String is missing for log file: " + i);
			}
		}

		log.info("Finished scheduled log extraction");
		return numSuccExtractions;
	}

	/**
	 * Given filename and searchKeyword, extracts logs from filename containing a
	 * searchString and sends them to Splunk.
	 * 
	 * @param filePath        - String representation of the path to the log file
	 * @param searchKeywords  - String array representing search keywords to extract
	 *                        for
	 * @param excludeKeywords - String array representation of keywords avoid in
	 * @return - 1 if log extraction was successful, 0 otherwise
	 */
	public int extractLogs(String filePath, String[] searchStrings, String[] excludeKeywords) {
		log.debug("Starting to extract logs");
		log.debug("File Path: " + filePath);
		log.debug("Search Keyword: " + Arrays.toString(searchStrings));
		log.debug("Text to exclude is: " + Arrays.toString(excludeKeywords));

		int ret = 0;
		Path path = Paths.get(filePath);

		boolean updateLastRunTime = false;
		TempLastRun tempLastRun = null;

		ret = extractCurrentDate(path, searchStrings, excludeKeywords, tempLastRun, updateLastRunTime);

		log.debug("Finished extracting logs");
		return ret;
	}

	/**
	 * Extracts logs from logPath given searchString to filter and send them to
	 * Splunk
	 * 
	 * @param logPath        - Path representing the directory where logs are stored
	 * @param searchStrings  - String array representing the search keywords to
	 *                       extract for
	 * @param excludeStrings - String array representing list of Strings to exclude
	 * @param tempLastRun    - TempLastRun object used to keep track of the log's
	 *                       latest time stamp encountered
	 * @param filterDate     - Filter logs by last run date if true, otherwise
	 *                       extract all logs
	 * @return int value of 1 if logs were successfully extracted, and 0 otherwise
	 */
	public int extractCurrentDate(Path logPath, String[] searchStrings, String[] excludeStrings,
			TempLastRun tempLastRun, boolean filterDate) {
		int ret = 0;
		Stream<String> stream = null;

		// Calculate time taken to process extract logs
		long startTime = System.currentTimeMillis();

		try {
			Predicate<String> validLog = i -> false;

			// read all the lines in fileName, extracting logs containing searchString, with
			// length greater than 24 and occurring after the lastRunDate
			stream = Files.lines(logPath);
			if (filterDate) {
				// Grab the last time logs were extracted from this file
				LocalDateTime lastRunDate = getLastGrabTime();
				LocalDateTime curDate = LocalDateTime.now();

				// Returns true if log date is between lastRunDate and curDate
				Predicate<String> withinDate = i -> i.length() > 24 && dateValidator.isValid(i.substring(0, 24))
						&& LocalDateTime.parse(i.substring(0, 24), formatter).isAfter(lastRunDate)
						&& LocalDateTime.parse(i.substring(0, 24), formatter).isBefore(curDate);

				// Returns true if log contains a search keyword and within the defined date
				// period, but does not include an exclude keyword
				validLog = i -> Arrays.asList(searchStrings).stream()
						.anyMatch(searchKeyword -> i.toLowerCase().contains(searchKeyword.toLowerCase()))
						&& !Arrays.asList(excludeStrings).stream()
								.anyMatch(excludeKeyword -> i.toLowerCase().contains(excludeKeyword.toLowerCase()))
						&& withinDate.test(i);

			} else {
				validLog = i -> Arrays.asList(searchStrings).stream()
						.anyMatch(search -> i.toLowerCase().contains(search.toLowerCase()))
						&& !Arrays.asList(excludeStrings).stream()
								.anyMatch(excludeKeyword -> i.toLowerCase().contains(excludeKeyword.toLowerCase()));
			}
			stream.filter(validLog)
					.forEach(msg -> sendLogToSplunk(msg, logPath.getFileName().toString(), tempLastRun, filterDate));

			ret = 1;
		} catch (IOException e) {
			log.error("Log Extraction Helper : File reader exception in extractCurrentDate : {}", e.toString(), e);
		} catch (DateTimeParseException e) {
			log.error("Log Extraction Helper : Date parse exception in extractCurrentDate : {}", e.toString(), e);
		} catch (Exception e) {
			log.error("Log Extraction Helper : Unexpected exception in extractCurrentDate : {}", e.toString(), e);
		} finally {
			if (stream != null)
				stream.close();
		}

		// Calculate time taken to extract logs
		long finishTime = System.currentTimeMillis();
		long elapsedTime = finishTime - startTime;

		log.info("Time taken to extract logs from " + logPath.getFileName() + " was " + elapsedTime + "ms");

		return ret;
	}

	/**
	 * Wraps message and fileName as an OutLogRes and sends its JSON representation
	 * to Splunk. If updateLastRunTime, then temp last run time is updated with the
	 * latest log time
	 * 
	 * @param message           - String representing log message to be sent to
	 *                          Splunk
	 * @param fileName          - String representing the name of the log file
	 * @param tempLastRun       - TempLastRun object used to keep track of the log's
	 *                          latest time stamp encountered
	 * @param updateLastRunTime - true if temp last run is to be updated, false
	 *                          otherwise
	 */
	public void sendLogToSplunk(String message, String fileName, TempLastRun tempLastRun, boolean updateLastRunTime) {
		log.info("Starting to log message to Splunk");
		boolean incCounter = false;

		OutLogRes outLogRes = logHelper.logReqHelper(message, fileName);
		String strLog = stringUtil.mapToString(outLogRes);
		// log strLog to file

		if (strLog != null) {
			logHelper.logOutResponse(strLog, incCounter);

			log.debug("Message: " + message);

			if (tempLastRun != null && updateLastRunTime && strLog.length() > 24) {
				// Set the temp last run date
				String lastDate = message.substring(0, 24);
				LocalDateTime curLatestDate = LocalDateTime.parse(lastDate, formatter);
				LocalDateTime latestDate = getTempLastGrabTime(tempLastRun);

				if (latestDate == null || curLatestDate.isAfter(latestDate))
					tempLastRun.setTempLastLogDateTime(lastDate);
			}
		}
		log.info("Finished logging message to Splunk");
	}

	/**
	 * Update last run time with the temporary last run time
	 * 
	 * @param tempLastRun - TempLastRun object used to keep track of the log's
	 *                    latest time stamp encountered
	 */
	public void updateLastRun(TempLastRun tempLastRun) {
		String strTempLastRunDate = tempLastRun.getTempLastLogDateTime();
		LocalDateTime tempLastRunDate = getTempLastGrabTime(tempLastRun);
		LocalDateTime lastRunDate = getLastGrabTime();

		// Set the last run date and the default last run date
		if (strTempLastRunDate != null && tempLastRunDate != null && tempLastRunDate.isAfter(lastRunDate)) {
			log.debug("Updating lastRun date to: " + strTempLastRunDate);
			lastRun.setLastLogDateTime(strTempLastRunDate);
			lastRunHelper.setDefaultLastRun(tempLastRunDate, lastRunFile, DATE_TIME_FORMAT);
		}
	}

	/**
	 * Increment out counter
	 */
	public void updateCounterInfo() {
		int outCount = logHelper.incOutResponse();
		log.info("Finished logging extracted logs (OC: " + outCount + ")");
	}

	/**
	 * Get the last time logs have been extracted from fileName
	 * 
	 * @return a LocalDateTime representation of the last time fileName has been
	 *         extracted from
	 */
	private LocalDateTime getLastGrabTime() {
		log.info("Extracting last log run time");

		String lastRunString = lastRun.getLastLogDateTime();
		LocalDateTime lastRunDate = null;

		if (lastRunString == null) {
			lastRun.setLastLogDateTime(lastRunHelper.getDefaultLastRun(lastRunFile, DATE_TIME_FORMAT, DEFAULT_TIME));
		}

		try {
			lastRunDate = LocalDateTime.parse(lastRun.getLastLogDateTime(), formatter);
		} catch (DateTimeParseException e) {
			log.error("Log Extraction Helper : Date parse exception in getLastGrabTime : {}", e.toString(), e);
		} catch (Exception e) {
			log.error("Log Extraction Helper : Unexpected exception in getLastGrabTime : {}", e.toString(), e);
		}

		log.debug("Last log run date: " + lastRunDate);
		return lastRunDate;
	}

	/**
	 * Get the temporary last run time for log extraction
	 * 
	 * @param tempLastRun - TempLastRun object used to keep track of the log's
	 *                    latest time stamp encountered
	 * @return a LocalDateTime representation of the temporary last run time
	 */
	private LocalDateTime getTempLastGrabTime(TempLastRun tempLastRun) {
		log.info("Extracting last temp log run time");

		String lastRunString = tempLastRun.getTempLastLogDateTime();
		LocalDateTime lastRunDate = null;

		if (lastRunString == null) {
			return null;
		}

		try {
			lastRunDate = LocalDateTime.parse(tempLastRun.getTempLastLogDateTime(), formatter);
		} catch (DateTimeParseException e) {
			log.error("Log Extraction Helper : Date parse exception in getTempLastGrabTime : {}", e.toString(), e);
		} catch (Exception e) {
			log.error("Log Extraction Helper : Unexpected exception in getTempLastGrabTime : {}", e.toString(), e);
		}

		log.debug("Last temp run date: " + lastRunDate);
		return lastRunDate;
	}
}
