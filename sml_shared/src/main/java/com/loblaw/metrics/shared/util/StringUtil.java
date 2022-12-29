package com.loblaw.metrics.shared.util;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StringUtil {
	private static ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Extract the value of key from a map given by content
	 * 
	 * @param key     - String representing the value's key to extract for
	 * @param content - String representation of a map containing key
	 * @return - Object representing the value of key in content
	 */
	public Object extractMapValue(String key, String content) {
		Object ret = null;
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = objectMapper.readValue(content, Map.class);
			ret = map.get(key);
		} catch (JsonProcessingException e) {
			ret = null;
			log.error("String Utility : JSON processing exception in extractMapValue : {}", e.toString(), e);
		} catch (Exception e) {
			ret = null;
			log.error("String Utility : Unexpected exception in extractMapValue : {}", e.toString(), e);
		}
		return ret;
	}

	/**
	 * Convert object to a String
	 * 
	 * @param object - Object to convert into a string
	 * @return - String representation of object
	 */
	public String mapToString(Object object) {
		String ret = null;
		try {
			ret = objectMapper.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			ret = null;
			log.error("String Utility : JSON processing exception in mapToString : {}", e.toString(), e);
		} catch (Exception e) {
			ret = null;
			log.error("String Utility : Unexpected exception in mapToString : {}", e.toString(), e);
		}

		return ret;
	}

	/**
	 * Parse num to a Long
	 * 
	 * @param num - String representing a Long number
	 * @return - Long representing num
	 */
	public Long parseLong(String num) {
		Long ret = 0L;

		try {
			ret = Long.parseLong(num);
		} catch (NumberFormatException e) {
			ret = null;
			log.error("String Utility : Number parse exception in parseLong : {}", e.toString(), e);
		} catch (Exception e) {

			ret = null;
			log.error("String Utility : Unexpected exception in parseLong : {}", e.toString(), e);
		}
		return ret;
	}

	/**
	 * Parse num to an Integer
	 * 
	 * @param num - String representing an Integer number
	 * @return - Integer representing num
	 */
	public Integer parseInt(String num) {
		Integer ret = 0;

		try {
			ret = Integer.parseInt(num);
		} catch (NumberFormatException e) {
			ret = null;
			log.error("String Utility : Number parse exception in parseInt : {}", e.toString(), e);
		} catch (Exception e) {

			ret = null;
			log.error("String Utility : Unexpected exception in parseInt : {}", e.toString(), e);
		}
		return ret;
	}

	/**
	 * Extract the base name of path
	 * 
	 * @param path - String representing path to extract base name from
	 * @return - String representing the base name from path
	 */
	public String getBaseName(String path) {
		String baseName = path.replaceAll("\\/$", "");

		int lastIndex = baseName.lastIndexOf("/");

		if (lastIndex > 0 && lastIndex < baseName.length())
			baseName = baseName.substring(lastIndex + 1);
		else
			baseName = path;
		return baseName;
	}
}
