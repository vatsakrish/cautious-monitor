package com.loblaw.metrics.shared;

/**
 * Project constants
 */
public class SmlSharedConstants {
	private SmlSharedConstants() {
	}

	// Must be same as first 2 number of ${project.version} from pom.xml
	// for ex. if pom.xml version is 2.1.5 than api version must be v2.1
	public static final String API_VERSION = "/v1";

	// URL for metric consuming API
	public static final String APP_METRICS_URL = "/appmetrics";

	// URL for metric consuming API with version
	public static final String APP_METRICS_VER_URL = API_VERSION + APP_METRICS_URL;

	// URL for application health consuming API
	public static final String APPHEALTH_METRICS_URL = "/apphealthmetrics";

	// URL for service health consuming API
	public static final String DATABASE_QUERIES_URL = "/getdbdata";

	// URL for container health consuming API
	public static final String CONTAINERHEALTH_METRICS_URL = "/containerhealthmetrics";

	// URL for log extraction
	public static final String LOG_SUMMARY_URL = "/logextractor";

	// URL for sending data directly to Splunk
	public static final String DATA_TO_SPLUNK_URL = "/datatosplunk";
}
