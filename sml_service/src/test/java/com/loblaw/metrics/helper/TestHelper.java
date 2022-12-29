package com.loblaw.metrics.helper;

import org.junit.Ignore;

import com.loblaw.metrics.shared.model.AppReq;
import com.loblaw.metrics.shared.model.OutAppReq;

@Ignore
public class TestHelper {

	/**
	 * Create an AppReq with projectName, appStatus, methodNAme, and responseTime
	 * 
	 * @param projectName  - String representing the name of the project
	 * @param appStatus    - String representing the application status JSON
	 * @param methodName   - String representing name of the method
	 * @param responseTime - int representing the time taken to extract the data
	 * @return - an AppReq with populated fields for the supplied projectName,
	 *         appStatus, methodName, and responseTime
	 */
	public AppReq createAppReq(String projectName, String appStatus, String methodName, int responseTime) {
		AppReq appReq = new AppReq();
		appReq.setAppStatus(appStatus);
		appReq.setProjectName(projectName);
		appReq.setMethodName(methodName);
		appReq.setResponseTime(responseTime);

		return appReq;
	}

	/**
	 * Create an OutAppReq with appReq's properties, date, format, uuid and storeNum
	 * 
	 * @param appReq       AppReq representing the response to wrap around
	 * @param dateTime     String representing a date time object
	 * @param uuid         String representing a unique identifier for the response
	 * @param host     String representing the store data was extracted from
	 * @param buildVersion String representing the build version of the server
	 * @param ipAddress    String representing the server's IP address
	 * @param province     String representing the server's province
	 * @return an OutAppReq with fields populated for appReq attributes, dateTime,
	 *         uniqueId, store number, build version, IP address and province
	 */
	public OutAppReq createOutAppReq(AppReq appReq, String dateTime, String uuid, String host, String buildVersion,
			String ipAddress, String province) {
		OutAppReq outAppReq = new OutAppReq();

		// Map AppReq attributes to OutAppReq
		outAppReq.setAppStatus(appReq.getAppStatus());
		outAppReq.setMethodName(appReq.getMethodName());
		outAppReq.setProjectName(appReq.getProjectName());
		outAppReq.setResponseTime(appReq.getResponseTime());
		outAppReq.setServiceStatus(appReq.getServiceStatus());

		// Set OutAppReq attributes
		outAppReq.setDateTime(dateTime);
		outAppReq.setUniqueid(uuid);
		outAppReq.setHost(host);
		outAppReq.setBuildVersion(buildVersion);
		outAppReq.setIpAddress(ipAddress);
		outAppReq.setProvince(province);

		return outAppReq;
	}

	/**
	 * Create the application status JSON with appStatus
	 * 
	 * @param appStatus - String representing the list of application health
	 *                  statuses
	 * @return = String representing the JSON of an applicaiton status
	 */
	public String createAppStatus(String appStatus) {
		return String.format("{%s}", appStatus.replaceAll(", $", "").replaceAll("\"", "\\\\\""));
	}
}
