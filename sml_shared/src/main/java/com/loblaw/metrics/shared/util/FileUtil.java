package com.loblaw.metrics.shared.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileUtil {

	/**
	 * Write messages separated by newline to filePath
	 * 
	 * @param filePath String representing location to write file
	 * @param messages List of messages to write to file
	 * @return int value representing status of the write operation. 1 if messages
	 *         were successfully written to file. -1 for IOExceptions and -2
	 *         otherwise.
	 */
	public int writeToFile(String filePath, List<String> messages) {
		Path path = Paths.get(filePath);
		int ret = 0;

		try {
			Files.write(path, messages);
			ret = 1;
		} catch (IOException e) {
			ret = -1;
			log.error("File Utility : File write exception in writeToFile : {}", e.toString(), e);
		} catch (Exception e) {
			ret = -2;
			log.error("");
			log.error("File Utility : Unexpected exception in writeToFile : {}", e.toString(), e);
		}

		return ret;
	}

	/**
	 * Read file located at filePath and add its contents to results
	 * 
	 * @param filePath String representing the path to the file
	 * @param results  List to store the messages read in file
	 * @return int value representing status of the read operation. 1 if file was
	 *         read successfully. -1 for IOExceptions and -2 otherwise.
	 */
	public int readFile(String filePath, List<String> results) {
		Path path = Paths.get(filePath);
		int ret = 0;

		try {
			results.addAll(Files.readAllLines(path));
			ret = 1;
		} catch (IOException e) {
			ret = -1;
			log.error("File Utility : File read exception in readFile : {}", e.toString(), e);
		} catch (Exception e) {
			ret = -2;
			log.error("File Utility : Unexpected exception in readFile : {}", e.toString(), e);
		}

		return ret;
	}

	/**
	 * Read file located at filePath and add its contents to results
	 * 
	 * @param filePath String representing the path to the file
	 * @param results  List to store the messages read in file
	 * @param numLines Number of lines to extract from the file
	 * @return int value representing status of the read operation. 1 if file was
	 *         read successfully. -1 for IOExceptions and -2 otherwise.
	 */
	public int readFile(String filePath, List<String> results, int numLines) {
		int ret = 0;

		try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
			String line;

			for (int i = 0; i < numLines; i++) {
				line = br.readLine();

				if (line != null)
					results.add(line);
				else
					break;
			}
			ret = 1;

		} catch (IOException e) {
			ret = -1;
			log.error("File Utility : File read exception in readFile : {}", e.toString(), e);
		} catch (Exception e) {
			ret = -2;
			log.error("File Utility : Unexpected exception in readFile : {}", e.toString(), e);
		}

		return ret;
	}
}
