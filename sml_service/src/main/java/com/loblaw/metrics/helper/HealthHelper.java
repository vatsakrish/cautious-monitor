package com.loblaw.metrics.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import com.loblaw.metrics.shared.model.ApplicationHealth;
import com.loblaw.metrics.shared.util.RestCallUtil;
import com.loblaw.metrics.shared.util.ServerUtil;
import com.loblaw.metrics.shared.util.StringUtil;
import com.loblaw.metrics.shared.util.WebClientErrorInterface;
import com.loblaw.metrics.shared.util.WebClientFlatMapInterface;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Util Helper to help extract application health details
 * 
 * @author brytung
 *
 */
@Component
@Slf4j
public class HealthHelper {

	@Autowired
	private Environment env;

	@Autowired
	private WebClient webClient;

	@Autowired
	private ServerUtil serverUtil;

	@Autowired
	private StringUtil stringUtil;

	@Autowired
	private RestCallUtil restCallUtil;

	private static final String APPLICATION_ENDPOINT = "application.endpoint.";
	private static final String HEADER = ".header";
	private static final String APPLICATION_ENDPOINT_NAME = ".name";
	private static final String APPLICATION_ENDPOINT_ENDPOINT = ".endpoint";
	private static final String APPLICATION_ENDPOINT_WITH_DETAILS = ".with-details";
	private static final int ERROR_MESSAGE_LENGTH = 20;
	private static final String SERVICE_ENDPOINT = "application.service.";
	private static final String SERVICE_TOTAL_ENDPOINT = "total";

	/**
	 * Extracts application health data from list of health end points provided
	 * 
	 * @param endpoints - List of String used to identify Health end points from
	 *                  properties
	 * @return a String containing the application name, application health, HTTP
	 *         status, error message, and details
	 */
	public String extractHealth(List<String> endpoints) {
		String output = "";

		// Makes parallel REST calls to application health end points
		List<ApplicationHealth> appHealthList = Flux.fromIterable(endpoints).parallel()
				.runOn(Schedulers.boundedElastic()).flatMap(this::getHealth)
				.collectSortedList((i, j) -> j.getApplicationName().compareTo(i.getApplicationName())).block();

		// Formats application health data into key value pairs
		for (ApplicationHealth appHealth : appHealthList) {
			output += String.format("\"%s\": %s, ", appHealth.getApplicationName(), stringUtil.mapToString(appHealth));
		}
		return output;
	}

	/**
	 * Extracts header details and health end point details given endpointId and
	 * makes a REST call to the end point to retrieve application health data
	 * 
	 * @param endpointId - String representation of the location of end point
	 *                   details in properties
	 * @return - Mono representation of ApplicationHealth response from the health
	 *         end point
	 */
	private Mono<ApplicationHealth> getHealth(String endpointId) {
		// Identify whether a status is expected from the response
		boolean withStatus = endpointId.contains("with-status");
		boolean withDetails = false;

		String location = APPLICATION_ENDPOINT + endpointId;
		ApplicationHealth applicationHealth = new ApplicationHealth();
		String applicationName = "";
		String endpoint = null;
		HttpEntity<String> entity;

		String applicationNameLocation = location + APPLICATION_ENDPOINT_NAME;
		String applicationEndpointLocation = location + APPLICATION_ENDPOINT_ENDPOINT;
		String applicationDetailsLocation = location + APPLICATION_ENDPOINT_WITH_DETAILS;

		// Extract application name, end point and withDetails value
		if (env.containsProperty(applicationNameLocation) && env.containsProperty(applicationEndpointLocation)
				&& env.containsProperty(applicationDetailsLocation)) {
			applicationName = env.getProperty(applicationNameLocation);
			applicationHealth.setApplicationName(applicationName);

			endpoint = env.getProperty(applicationEndpointLocation);

			// withDetails set to false unless WITH_DETAILS property is "true"
			log.debug(applicationName + ": Setting withDetails");
			String strWithDetails = env.getProperty(applicationDetailsLocation);
			if (strWithDetails == "true") {
				withDetails = true;
			}

			// Set the headers for the application endpoint, and get it's response to set
			// the applicationHealth's attributes
			entity = createHeader(location);

			// Start timing time taken for REST call
			long startTime = System.currentTimeMillis();

			// REST call to endpoint
			Mono<ApplicationHealth> appHealthMono = webClientGetApplicationHealth(endpoint, applicationHealth, entity);

			ApplicationHealth response = appHealthMono.block();

			// Stop timer after blocking for response
			long finishTime = System.currentTimeMillis();
			long elapsedTime = finishTime - startTime;

			// Set applicationHealth attributes with response values
			setApplicationHealth(applicationHealth, response, withStatus, withDetails);

			log.debug("Total time for processing " + applicationHealth.getApplicationName() + " is: " + elapsedTime
					+ "ms");

			return Mono.just(applicationHealth);
		} else {
			return Mono.empty();
		}
	}

	/**
	 * Make a REST call to endpoint using WebClient. Set the GET request headers
	 * using entity, and set applicationHealth HttpStatus using the response
	 * 
	 * @param endpoint          - String representing endpoint to make REST call on
	 * @param applicationHealth - ApplicationHealth containing the application name
	 *                          and used to store the HTTP Response
	 * @param entity            - Entity containing the HTTP Headers
	 * @return - a Mono response containing an ApplicationHealth object
	 */
	private Mono<ApplicationHealth> webClientGetApplicationHealth(String endpoint, ApplicationHealth applicationHealth,
			HttpEntity<String> entity) {
		WebClientErrorInterface<ApplicationHealth> wCE = error -> {
			ApplicationHealth response = new ApplicationHealth();
			// Sets ApplicationHealth error message
			if (error.getMessage() != null)
				response.setError(error.getMessage().substring(ERROR_MESSAGE_LENGTH).replaceAll("\"", "\'"));
			log.error(applicationHealth.getApplicationName() + ": Error sending REST call - " + error.getMessage());

			// Checks for socket exception and sets HTTP Status to 504 error
			if (error instanceof WebClientRequestException) {
				log.debug(applicationHealth.getApplicationName() + ": " + error.getCause());
				applicationHealth.setHttpCode(HttpStatus.GATEWAY_TIMEOUT.value());
			} else {
				log.debug(applicationHealth.getApplicationName() + ": Unidentified error type: " + error.getCause());
			}
			return Mono.just(response);
		};

		WebClientFlatMapInterface<ApplicationHealth> wCFM = clientResponse -> {
			if (clientResponse != null) {
				log.info(applicationHealth.getApplicationName() + ": Client response exists");
				// Extracts HttpCode and sets it to the applicationHealth object
				applicationHealth.setHttpCode(clientResponse.rawStatusCode());
				return clientResponse.bodyToMono(ApplicationHealth.class);
			} else {
				log.info(applicationHealth.getApplicationName() + ": Client response is null");
				return Mono.just(new ApplicationHealth());
			}
		};

		return restCallUtil.getWebClient(endpoint, applicationHealth, entity, webClient, wCFM, wCE);
	}

	/**
	 * Sets the attributes of applicationHealth given the value of response
	 * 
	 * @param applicationHealth - ApplicationHealth representing object to set
	 * @param response          - ApplicationHealth representing the response from
	 *                          the REST call
	 * @param withStatus        - boolean representing whether response is expected
	 *                          to have status populated
	 */
	private void setApplicationHealth(ApplicationHealth applicationHealth, ApplicationHealth response,
			boolean withStatus, boolean withDetails) {
		log.info(applicationHealth.getApplicationName() + ": Starting to setApplicationHealth details");
		// Ensure response exists and has a body and a status code
		if (response != null) {
			// Sets applicationHealth status, details, and HTTP code
			responseBodyToApplicationHealth(response, applicationHealth, withDetails);

			// If the response HTTP status code is an error, or an error message exists,
			// then the application is down
			if (applicationHealth.getHttpCode() == 0 || HttpStatus.valueOf(applicationHealth.getHttpCode()).isError()
					|| applicationHealth.getError() != null)
				applicationHealth.setStatus("DOWN");
			else {
				log.info(applicationHealth.getApplicationName() + " checking with status");

				// If the response body should contain a status, set application health status
				// based
				// on its value, otherwise, status is UP
				if (!withStatus || withStatus && applicationHealth.getStatus() != null
						&& "UP".equals(applicationHealth.getStatus()))
					applicationHealth.setStatus("UP");
				else
					applicationHealth.setStatus("DOWN");
			}
		}

		if (applicationHealth.getError() == null)
			applicationHealth.setError("NA");

		log.info("Finished with setApplicationHealth");
	}

	/**
	 * Sets the attributes of applicationHealth given response
	 * 
	 * @param response          - ApplicationHealth representing the REST call
	 *                          response
	 * @param applicationHealth - ApplicationHealth representing the object to set
	 */
	private void responseBodyToApplicationHealth(ApplicationHealth response, ApplicationHealth applicationHealth,
			boolean withDetails) {
		// Set appliicationHealth given response's body
		applicationHealth.setStatus(response.getStatus());
		applicationHealth.setError(response.getError());

		// Sets applicationHealth details
		if (withDetails) {
			log.debug("Picking up details for: " + applicationHealth.getApplicationName());
			applicationHealth.setDetails(response.getDetails());
		}

		// Sets applicationHealth HTTP code to the one given by response
		// This occurs when REST call errors
		if (applicationHealth.getHttpCode() == null) {
			if (response.getHttpCode() == null) {
				log.debug(applicationHealth.getApplicationName() + ": " + "Setting HTTP Code to 500");
				applicationHealth.setHttpCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			} else {
				applicationHealth.setHttpCode(response.getHttpCode());
			}

		}

		// Sets applicationHealth error message if it contains details and isn't already
		// populated
		if (applicationHealth.getDetails() != null && applicationHealth.getError() == null) {
			applicationHealth.setError(getErrorFromDetails(applicationHealth.getDetails()));
		}
	}

	/**
	 * Create headers for REST template call
	 * 
	 * @param location - String representation of where to look for header
	 *                 information in the properties
	 * @return an HttpEntity containing the header information
	 */
	private HttpEntity<String> createHeader(String location) {
		log.info("Starting to extract header details");
		HttpHeaders headers = new HttpHeaders();
		String[] headersArr = null;
		String[] tempHeader = null;
		int numHeader = 0;
		String headerName = null;
		String headerValue = null;

		// Extracts the number of headers the endpoint given by location has
		if (env.containsProperty(location + HEADER)) {
			headersArr = env.getProperty(location + HEADER).split(",");
			numHeader = headersArr.length;
		}

		// Sets the header for each one provided in properties file
		for (int i = 0; i < numHeader; i++) {
			tempHeader = headersArr[i].split(":");
			if (tempHeader.length == 2) {
				headerName = headersArr[i].split(":")[0].trim();
				headerValue = headersArr[i].split(":")[1].trim();
				if (headerName.length() != 0 && headerValue.length() != 0) {
					headers.set(headerName, headerValue);
				}
			}
		}
		headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

		log.info("Finished extracting header details");
		return new HttpEntity<>(headers);
	}

	@SuppressWarnings("unchecked")
	/**
	 * Extracts the error message from detailsObject by recursively going through
	 * the nested object
	 * 
	 * @param detailsObject - a Map representation of the contents of
	 *                      ApplicationHealth's details
	 * @return - a String representation of the first error message found in
	 *         detailsObject
	 */
	private String getErrorFromDetails(Map<String, Object> detailsObject) {
		String error = null;
		// Iterate through all the keys in detailsObject
		for (Entry<String, Object> entry : detailsObject.entrySet()) {
			// Ensure the value of the key is of type Map
			if (entry.getValue() instanceof Map && ((Map<String, Object>) entry.getValue()).containsKey("details")) {
				// If the value of the key holds a field for details, check for an error message
				Map<String, Object> detailsObjectValue = (Map<String, Object>) entry.getValue();
				Map<String, Object> details = (Map<String, Object>) detailsObjectValue.get("details");
				// Recursively call getErrorFromDetails if details doesn't contain an error
				// field
				if (details.containsKey("error")) {
					error = details.get("error").toString();
				} else {
					error = getErrorFromDetails(details);
				}
				if (error != null) {
					return error;
				}
			}
		}
		return error;
	}

	/**
	 * Extract the application health of services
	 * 
	 * @return JSON representing the status of services
	 */
	public String getServiceHealth() {
		String serviceEndpoint = SERVICE_ENDPOINT + SERVICE_TOTAL_ENDPOINT;

		Integer totalService = 0;
		List<String> services = new ArrayList<>();

		// Extract the total number of TC servers to perform status check
		if (env.containsProperty(serviceEndpoint)) {
			totalService = stringUtil.parseInt(env.getProperty(serviceEndpoint));
			if (totalService == null)
				totalService = 0;
		}

		// Extract the application name of the TC servers
		for (int i = 1; i <= totalService; i++) {
			if (env.containsProperty(SERVICE_ENDPOINT + i)) {
				String service = env.getProperty(SERVICE_ENDPOINT + i);
				services.add(service);
			}
		}

		// Extract the CMD of all running processes
		log.info("Starting to extract service health");
		long startTime = System.currentTimeMillis();
		List<String> processes = serverUtil.getAllProcessCmd();
		long finishTime = System.currentTimeMillis();
		long elapsedTime = finishTime - startTime;

		log.debug("Total time for processing services is: " + elapsedTime);
		String strServices = "{";

		// Iterate through the TC servers and set their status to UP only if it's
		// process is running
		for (String service : services) {

			ApplicationHealth appHealth = new ApplicationHealth();

			// Set application name as base name
			String appName = stringUtil.getBaseName(service);

			if (processes.toString().contains(service)) {
				appHealth.setStatus("UP");
			} else {
				appHealth.setStatus("DOWN");
			}

			String strAppHealth = stringUtil.mapToString(appHealth);

			if (strAppHealth != null)
				strServices += "\"" + appName + "\": " + stringUtil.mapToString(appHealth) + ",";
		}

		strServices = strServices.replaceAll(",$", "") + "}";
		log.info("Finished extracting service health");
		return strServices;
	}
}
