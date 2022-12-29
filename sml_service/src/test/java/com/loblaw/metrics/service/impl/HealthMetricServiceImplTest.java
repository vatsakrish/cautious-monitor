package com.loblaw.metrics.service.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.reactive.function.client.WebClient;

import com.loblaw.metrics.config.SmlConfiguration;
import com.loblaw.metrics.config.SmlProperties;
import com.loblaw.metrics.exception.UnexpectedApplicationHealthException;
import com.loblaw.metrics.helper.HealthHelper;
import com.loblaw.metrics.helper.LogHelper;
import com.loblaw.metrics.helper.TestHelper;
import com.loblaw.metrics.shared.model.AppReq;
import com.loblaw.metrics.shared.model.ApplicationHealth;
import com.loblaw.metrics.shared.model.OutAppReq;
import com.loblaw.metrics.shared.util.RestCallUtil;
import com.loblaw.metrics.shared.util.ServerUtil;
import com.loblaw.metrics.shared.util.WebClientErrorInterface;
import com.loblaw.metrics.shared.util.WebClientFlatMapInterface;

import reactor.core.publisher.Mono;

@RunWith(SpringJUnit4ClassRunner.class)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { HealthMetricServiceImpl.class, HealthHelper.class, SmlConfiguration.class })
@TestPropertySource(properties = { "rest-template.timeout.connect=0", "rest-template.timeout.read=0" })
public class HealthMetricServiceImplTest {
	@MockBean
	private SmlProperties smlProperties;
	@MockBean
	private WebClient webClient;
	@MockBean
	private ServerUtil serverUtil;
	@MockBean
	private RestCallUtil restCallUtil;
	@MockBean
	private LogHelper logHelper;
	@MockBean
	private Environment env;
	@Autowired
	HealthMetricServiceImpl healthMetricServiceImpl;

	private static final String DATE_FORMAT = "yyyy/MM/dd HH:mm:ss";
	private static final String APPLICATION_ENDPOINT_WITH_STATUS = "application.endpoint.with-status.";
	private static final String APPLICATION_ENDPOINT_WITHOUT_STATUS = "application.endpoint.without-status.";
	private static final String APPLICATION_ENDPOINT_WITH_STATUS_TOTAL = "application.endpoint.with-status.total";
	private static final String APPLICATION_ENDPOINT_WITHOUT_STATUS_TOTAL = "application.endpoint.without-status.total";
	private static final String APPLICATION_SERVICE_ENDPOINT = "application.service.";
	private static final String APPLICATION_SERVICE_ENDPOINT_TOTAL = "application.service.total";
	private static final String PROJECT_NAME = "Application_Health_Metrics";
	private static final String METHOD_NAME = "app_health";
	private static final String OUTAPPREQ_FORMAT = "{\"dateTime\":\"%s\",\"uniqueid\":\"%s\",\"projectName\":\"%s\",\"host\":\"%s\",\"province\":\"%s\",\"responseTime\":%d,\"methodName\":\"%s\",\"buildVersion\":\"%s\",\"app_status\":\"%s\",\"IP\":\"%s\",\"service_status\":\"%s\"}";
	private static final String UP_STATUS = "UP";
	private static final String DOWN_STATUS = "DOWN";
	private static final Integer UP_STATUSCODE = Integer.valueOf(200);
	private static final Integer DOWN_STATUSCODE = Integer.valueOf(500);

	private TestHelper testHelper;

	@Before
	public void init() {
		testHelper = new TestHelper();
	}

	@Test
	@DisplayName("When there are multiple application health endpoints with and without statuses to extract - then return the JSON representation of the response")
	public void scheduleSendApplicationHealth_multipleWithOrWithoutStatus_thenReturnValidResponse() {
		LocalDateTime date = LocalDateTime.now();
		DateTimeFormatter format = DateTimeFormatter.ofPattern(DATE_FORMAT);
		String uuid = "12345";
		String storeNum = "3144";
		int responseTime = 0;
		String buildVersion = "2.10";
		String ipAddress = "127.0.0.1";
		String province = "ON";

		int numContainsProperty = 22;

		String actuatorHealth = "http://actuator/health/";
		String withStatus1 = "delta";
		String withStatus2 = "ber";
		String withoutStatus1 = "csdf";
		String withoutStatus2 = "csjs";
		String service1 = "instances/confTCServer";
		String service2 = "instances/imzTCServer";

		ApplicationHealth appWithStatus1 = new ApplicationHealth(UP_STATUS, UP_STATUSCODE, null, null, withStatus1);
		ApplicationHealth appWithStatus2 = new ApplicationHealth(DOWN_STATUS, DOWN_STATUSCODE, null, null, withStatus2);
		ApplicationHealth appWithoutStatus1 = new ApplicationHealth(UP_STATUS, UP_STATUSCODE, null, null,
				withoutStatus1);
		ApplicationHealth appWithoutStatus2 = new ApplicationHealth(DOWN_STATUS, DOWN_STATUSCODE, null, null,
				withoutStatus2);

		List<String> listWithStatus = new ArrayList<>();
		listWithStatus.add(withStatus1);
		listWithStatus.add(withStatus2);
		List<String> listWithoutStatus = new ArrayList<>();
		listWithoutStatus.add(withoutStatus1);
		listWithoutStatus.add(withoutStatus2);
		List<String> listStatusNames = new ArrayList<>();
		listStatusNames.add(withStatus1);
		listStatusNames.add(withStatus2);
		listStatusNames.add(withoutStatus1);
		listStatusNames.add(withoutStatus2);
		List<ApplicationHealth> listAppHealth = new ArrayList<>();
		listAppHealth.add(appWithStatus1);
		listAppHealth.add(appWithStatus2);
		listAppHealth.add(appWithoutStatus1);
		listAppHealth.add(appWithoutStatus2);
		List<String> services = new ArrayList<>();
		services.add(service1);
		services.add(service2);
		List<String> serverProcessCmd = new ArrayList<>();

		when(logHelper.incInResponse()).thenReturn(1);
		when(env.containsProperty(anyString())).thenReturn(true);
		when(logHelper.logOutResponse(anyString())).thenReturn(1);

		whenApplicationHealth(actuatorHealth, listWithStatus, APPLICATION_ENDPOINT_WITH_STATUS_TOTAL,
				APPLICATION_ENDPOINT_WITH_STATUS);
		whenApplicationHealth(actuatorHealth, listWithoutStatus, APPLICATION_ENDPOINT_WITHOUT_STATUS_TOTAL,
				APPLICATION_ENDPOINT_WITHOUT_STATUS);
		whenWebClient(actuatorHealth, listStatusNames, listAppHealth);
		whenservice(services, serverProcessCmd);

		doAnswer(invocation -> {
			AppReq argAppReq = invocation.getArgument(0);
			OutAppReq resOutAppReq = testHelper.createOutAppReq(argAppReq, date.format(format), uuid, storeNum,
					buildVersion, ipAddress, province);
			resOutAppReq.setResponseTime(responseTime);

			return resOutAppReq;
		}).when(logHelper).reqHelper(any(AppReq.class));

		healthMetricServiceImpl.scheduledSendApplicationHealth();

		verify(logHelper).incInResponse();
		verify(env, times(numContainsProperty)).containsProperty(anyString());

		verifyApplicationHealth(listWithStatus.size(), APPLICATION_ENDPOINT_WITH_STATUS_TOTAL,
				APPLICATION_ENDPOINT_WITH_STATUS);
		verifyApplicationHealth(listWithoutStatus.size(), APPLICATION_ENDPOINT_WITHOUT_STATUS_TOTAL,
				APPLICATION_ENDPOINT_WITHOUT_STATUS);
		verifyWebClient(actuatorHealth, listStatusNames, listAppHealth);
		verifyservice(services, serverProcessCmd);
		verify(logHelper).reqHelper(any(AppReq.class));
		verify(logHelper).logOutResponse(anyString());
	}

	@Test
	@DisplayName("When the property for total application endpoints is invalid - then the exception is handled")
	public void scheduleSendApplicationHealth_parseApplicationEndpointTotalThrowException() {
		when(logHelper.incInResponse()).thenReturn(1);
		when(env.getProperty(APPLICATION_ENDPOINT_WITH_STATUS_TOTAL)).thenReturn("Text instead of integer");
		healthMetricServiceImpl.scheduledSendApplicationHealth();
	}

	@Test
	@DisplayName("When scheduled extraction of application health has multiple with and without endpoints to extract ")
	public void sendApplicationHealth_multipleWithAndWithoutStatus_thenReturnValidAppReq() {
		LocalDateTime date = LocalDateTime.of(2021, 04, 01, 15, 31, 35);
		DateTimeFormatter format = DateTimeFormatter.ofPattern(DATE_FORMAT);
		String uuid = "12345";
		String storeNum = "3144";
		int responseTime = 0;
		String buildVersion = "2.10";
		String ipAddress = "127.0.0.1";
		String province = "ON";

		int numContainsProperty = 21;

		String actuatorHealth = "http://actuator/health/";
		String withStatus1 = "delta";
		String withStatus2 = "ber";
		String withoutStatus1 = "csdf";
		String withoutStatus2 = "csjs";
		String service1 = "instances/confTCServer";
		String service2 = "instances/imzTCServer";

		ApplicationHealth appWithStatus1 = new ApplicationHealth(UP_STATUS, UP_STATUSCODE, null, null, withStatus1);
		ApplicationHealth appWithStatus2 = new ApplicationHealth(DOWN_STATUS, DOWN_STATUSCODE, null, null, withStatus2);
		ApplicationHealth appWithoutStatus1 = new ApplicationHealth(UP_STATUS, UP_STATUSCODE, null, null,
				withoutStatus1);
		ApplicationHealth appWithoutStatus2 = new ApplicationHealth(DOWN_STATUS, DOWN_STATUSCODE, null, null,
				withoutStatus2);

		List<String> listWithStatus = new ArrayList<>();
		listWithStatus.add(withStatus1);
		listWithStatus.add(withStatus2);
		List<String> listWithoutStatus = new ArrayList<>();
		listWithoutStatus.add(withoutStatus1);
		listWithoutStatus.add(withoutStatus2);
		List<String> listStatusNames = new ArrayList<>();
		listStatusNames.add(withStatus1);
		listStatusNames.add(withStatus2);
		listStatusNames.add(withoutStatus1);
		listStatusNames.add(withoutStatus2);
		List<ApplicationHealth> listAppHealth = new ArrayList<>();
		listAppHealth.add(appWithStatus1);
		listAppHealth.add(appWithStatus2);
		listAppHealth.add(appWithoutStatus1);
		listAppHealth.add(appWithoutStatus2);
		List<String> services = new ArrayList<>();
		services.add(service1);
		services.add(service2);
		List<String> serverProcessCmd = new ArrayList<>();
		serverProcessCmd.add(service1);

		String withStatus = "\"delta\": {\"status\":\"UP\",\"httpCode\":200,\"error\":\"NA\"}, \"ber\": {\"status\":\"DOWN\",\"httpCode\":500,\"error\":\"NA\"}, ";
		String withoutStatus = "\"csjs\": {\"status\":\"DOWN\",\"httpCode\":500,\"error\":\"NA\"}, \"csdf\": {\"status\":\"UP\",\"httpCode\":200,\"error\":\"NA\"}, ";
		String serviceStatus = testHelper
				.createAppStatus("\"confTCServer\": {\"status\":\"UP\"},\"imzTCServer\": {\"status\":\"DOWN\"}");

		String outAppReqJson = String.format(OUTAPPREQ_FORMAT, date.format(format), uuid, PROJECT_NAME, storeNum,
				province, responseTime, METHOD_NAME, buildVersion,
				testHelper.createAppStatus(withStatus + withoutStatus), ipAddress, serviceStatus);

		when(env.containsProperty(anyString())).thenReturn(true);
		when(logHelper.logOutResponse(anyString())).thenReturn(1);
		whenApplicationHealth(actuatorHealth, listWithStatus, APPLICATION_ENDPOINT_WITH_STATUS_TOTAL,
				APPLICATION_ENDPOINT_WITH_STATUS);
		whenApplicationHealth(actuatorHealth, listWithoutStatus, APPLICATION_ENDPOINT_WITHOUT_STATUS_TOTAL,
				APPLICATION_ENDPOINT_WITHOUT_STATUS);
		whenWebClient(actuatorHealth, listStatusNames, listAppHealth);
		whenservice(services, serverProcessCmd);

		doAnswer(invocation -> {
			AppReq argAppReq = invocation.getArgument(0);
			OutAppReq resOutAppReq = testHelper.createOutAppReq(argAppReq, date.format(format), uuid, storeNum,
					buildVersion, ipAddress, province);
			resOutAppReq.setResponseTime(responseTime);

			return resOutAppReq;
		}).when(logHelper).reqHelper(any(AppReq.class));

		when(logHelper.logOutResponse(anyString())).thenReturn(1);

		String actual = healthMetricServiceImpl.sendApplicationHealth();

		verifyApplicationHealth(listWithStatus.size(), APPLICATION_ENDPOINT_WITH_STATUS_TOTAL,
				APPLICATION_ENDPOINT_WITH_STATUS);
		verifyApplicationHealth(listWithoutStatus.size(), APPLICATION_ENDPOINT_WITHOUT_STATUS_TOTAL,
				APPLICATION_ENDPOINT_WITHOUT_STATUS);
		verifyWebClient(actuatorHealth, listStatusNames, listAppHealth);
		verifyservice(services, serverProcessCmd);
		verify(env, times(numContainsProperty)).containsProperty(anyString());
		verify(logHelper).reqHelper(any(AppReq.class));
		verify(logHelper).logOutResponse(anyString());

		assertEquals(outAppReqJson, actual);
	}

	@Test(expected = UnexpectedApplicationHealthException.class)
	@DisplayName("When the property for total application endpoints is invalid - then catch the exception and throw a UnexpectedApplicationHealthException")
	public void sendApplicationHealth_parseApplicationEndpointTotalThrowException_thenThrowUnexpectedApplicationHealthException() {
		when(logHelper.logOutResponse(anyString())).thenThrow(new RuntimeException());
		healthMetricServiceImpl.sendApplicationHealth();
	}

	@Test
	@DisplayName("When there are multiple application endpoints with statuses to extract - then return a valid request")
	public void sendApplicationHealth_multipleWithStatus_thenReturnValidAppReq() {
		LocalDateTime date = LocalDateTime.of(2021, 04, 01, 03, 22, 01);
		DateTimeFormatter format = DateTimeFormatter.ofPattern(DATE_FORMAT);
		String uuid = "3943";
		String storeNum = "3144";
		int responseTime = 0;
		String buildVersion = "2.22";
		String ipAddress = "127.0.0.1";
		String province = "ON";

		int numContainsProperty = 11;

		String actuatorHealth = "http://actuator/health/";
		String withStatus1 = "delta";
		String withStatus2 = "ber";

		ApplicationHealth appWithStatus1 = new ApplicationHealth(UP_STATUS, UP_STATUSCODE, null, null, withStatus1);
		ApplicationHealth appWithStatus2 = new ApplicationHealth(DOWN_STATUS, DOWN_STATUSCODE, null, null, withStatus2);

		List<String> listWithStatus = new ArrayList<>();
		listWithStatus.add(withStatus1);
		listWithStatus.add(withStatus2);
		List<String> listWithoutStatus = new ArrayList<>();
		List<String> listStatusNames = new ArrayList<>();
		listStatusNames.add(withStatus1);
		listStatusNames.add(withStatus2);
		List<ApplicationHealth> listAppHealth = new ArrayList<>();
		listAppHealth.add(appWithStatus1);
		listAppHealth.add(appWithStatus2);
		List<String> services = new ArrayList<>();
		List<String> serverProcessCmd = new ArrayList<>();

		String withStatus = "\"delta\": {\"status\":\"UP\",\"httpCode\":200,\"error\":\"NA\"}, \"ber\": {\"status\":\"DOWN\",\"httpCode\":500,\"error\":\"NA\"}, ";
		String withoutStatus = "";
		String serviceStatus = testHelper.createAppStatus("");

		String outAppReqJson = String.format(OUTAPPREQ_FORMAT, date.format(format), uuid, PROJECT_NAME, storeNum,
				province, responseTime, METHOD_NAME, buildVersion,
				testHelper.createAppStatus(withStatus + withoutStatus), ipAddress, serviceStatus);

		when(env.containsProperty(anyString())).thenReturn(true);
		when(logHelper.logOutResponse(anyString())).thenReturn(1);
		whenApplicationHealth(actuatorHealth, listWithStatus, APPLICATION_ENDPOINT_WITH_STATUS_TOTAL,
				APPLICATION_ENDPOINT_WITH_STATUS);
		whenApplicationHealth(actuatorHealth, listWithoutStatus, APPLICATION_ENDPOINT_WITHOUT_STATUS_TOTAL,
				APPLICATION_ENDPOINT_WITHOUT_STATUS);
		whenWebClient(actuatorHealth, listStatusNames, listAppHealth);
		whenservice(services, serverProcessCmd);

		doAnswer(invocation -> {
			AppReq argAppReq = invocation.getArgument(0);
			OutAppReq resOutAppReq = testHelper.createOutAppReq(argAppReq, date.format(format), uuid, storeNum,
					buildVersion, ipAddress, province);
			resOutAppReq.setResponseTime(responseTime);

			return resOutAppReq;
		}).when(logHelper).reqHelper(any(AppReq.class));

		when(logHelper.logOutResponse(anyString())).thenReturn(1);

		String actual = healthMetricServiceImpl.sendApplicationHealth();

		verifyApplicationHealth(listWithStatus.size(), APPLICATION_ENDPOINT_WITH_STATUS_TOTAL,
				APPLICATION_ENDPOINT_WITH_STATUS);
		verifyApplicationHealth(listWithoutStatus.size(), APPLICATION_ENDPOINT_WITHOUT_STATUS_TOTAL,
				APPLICATION_ENDPOINT_WITHOUT_STATUS);
		verifyWebClient(actuatorHealth, listStatusNames, listAppHealth);
		verifyservice(services, serverProcessCmd);
		verify(env, times(numContainsProperty)).containsProperty(anyString());
		verify(logHelper).reqHelper(any(AppReq.class));
		verify(logHelper).logOutResponse(anyString());

		assertEquals(outAppReqJson, actual);
	}

	@Test
	@DisplayName("When there are multiple application endpoints without statuses to extract - then return a valid request")
	public void sendApplicationHealth_multipleWithoutStatus_thenReturnValidAppReq() {
		LocalDateTime date = LocalDateTime.of(2021, 01, 07, 17, 26, 11);
		DateTimeFormatter format = DateTimeFormatter.ofPattern(DATE_FORMAT);
		String uuid = "7345";
		String storeNum = "3144";
		int responseTime = 0;
		String buildVersion = "1.36";
		String ipAddress = "127.0.0.1";
		String province = "ON";

		int numContainsProperty = 13;

		String actuatorHealth = "http://actuator/health/";
		String withoutStatus1 = "csdf";
		String withoutStatus2 = "csjs";
		String service1 = "instances/uaaTCServer";
		String service2 = "instances/hwTCServer";

		ApplicationHealth appWithoutStatus1 = new ApplicationHealth(UP_STATUS, UP_STATUSCODE, null, null,
				withoutStatus1);
		ApplicationHealth appWithoutStatus2 = new ApplicationHealth(DOWN_STATUS, DOWN_STATUSCODE, null, null,
				withoutStatus2);

		List<String> listWithStatus = new ArrayList<>();
		List<String> listWithoutStatus = new ArrayList<>();
		listWithoutStatus.add(withoutStatus1);
		listWithoutStatus.add(withoutStatus2);
		List<String> listStatusNames = new ArrayList<>();
		listStatusNames.add(withoutStatus1);
		listStatusNames.add(withoutStatus2);
		List<ApplicationHealth> listAppHealth = new ArrayList<>();
		listAppHealth.add(appWithoutStatus1);
		listAppHealth.add(appWithoutStatus2);
		List<String> services = new ArrayList<>();
		services.add(service1);
		services.add(service2);
		List<String> serverProcessCmd = new ArrayList<>();
		serverProcessCmd.add(service1);

		String withStatus = "";
		String withoutStatus = "\"csjs\": {\"status\":\"DOWN\",\"httpCode\":500,\"error\":\"NA\"}, \"csdf\": {\"status\":\"UP\",\"httpCode\":200,\"error\":\"NA\"}, ";
		String serviceStatus = testHelper
				.createAppStatus("\"uaaTCServer\": {\"status\":\"UP\"},\"hwTCServer\": {\"status\":\"DOWN\"}");

		String outAppReqJson = String.format(OUTAPPREQ_FORMAT, date.format(format), uuid, PROJECT_NAME, storeNum,
				province, responseTime, METHOD_NAME, buildVersion,
				testHelper.createAppStatus(withStatus + withoutStatus), ipAddress, serviceStatus);

		when(env.containsProperty(anyString())).thenReturn(true);
		when(logHelper.logOutResponse(anyString())).thenReturn(1);
		whenApplicationHealth(actuatorHealth, listWithStatus, APPLICATION_ENDPOINT_WITH_STATUS_TOTAL,
				APPLICATION_ENDPOINT_WITH_STATUS);
		whenApplicationHealth(actuatorHealth, listWithoutStatus, APPLICATION_ENDPOINT_WITHOUT_STATUS_TOTAL,
				APPLICATION_ENDPOINT_WITHOUT_STATUS);
		whenWebClient(actuatorHealth, listStatusNames, listAppHealth);
		whenservice(services, serverProcessCmd);

		doAnswer(invocation -> {
			AppReq argAppReq = invocation.getArgument(0);
			OutAppReq resOutAppReq = testHelper.createOutAppReq(argAppReq, date.format(format), uuid, storeNum,
					buildVersion, ipAddress, province);
			resOutAppReq.setResponseTime(responseTime);

			return resOutAppReq;
		}).when(logHelper).reqHelper(any(AppReq.class));

		when(logHelper.logOutResponse(anyString())).thenReturn(1);

		String actual = healthMetricServiceImpl.sendApplicationHealth();

		verifyApplicationHealth(listWithStatus.size(), APPLICATION_ENDPOINT_WITH_STATUS_TOTAL,
				APPLICATION_ENDPOINT_WITH_STATUS);
		verifyApplicationHealth(listWithoutStatus.size(), APPLICATION_ENDPOINT_WITHOUT_STATUS_TOTAL,
				APPLICATION_ENDPOINT_WITHOUT_STATUS);
		verifyWebClient(actuatorHealth, listStatusNames, listAppHealth);
		verifyservice(services, serverProcessCmd);
		verify(env, times(numContainsProperty)).containsProperty(anyString());
		verify(logHelper).reqHelper(any(AppReq.class));
		verify(logHelper).logOutResponse(anyString());

		assertEquals(outAppReqJson, actual);
	}

	@Test
	@DisplayName("When there are no application endpoints to extract - then return a valid request")
	public void sendApplicationHealth_noEndpoints_thenReturnValidAppReq() {
		LocalDateTime date = LocalDateTime.of(2021, 03, 21, 18, 42, 00);
		DateTimeFormatter format = DateTimeFormatter.ofPattern(DATE_FORMAT);
		String uuid = "6893";
		String storeNum = "3144";
		int responseTime = 0;
		String buildVersion = "1.23";
		String ipAddress = "127.0.0.1";
		String province = "ON";

		int numContainsProperty = 5;

		String actuatorHealth = "http://actuator/health/";
		String service1 = "instances/servicesTCServer";
		String service2 = "instances/hwTCServer";

		List<String> listWithStatus = new ArrayList<>();
		List<String> listWithoutStatus = new ArrayList<>();
		List<String> listStatusNames = new ArrayList<>();
		List<ApplicationHealth> listAppHealth = new ArrayList<>();
		List<String> services = new ArrayList<>();
		services.add(service1);
		services.add(service2);
		List<String> serverProcessCmd = new ArrayList<>();
		serverProcessCmd.add(service1);

		String withStatus = "";
		String withoutStatus = "";
		String serviceStatus = testHelper
				.createAppStatus("\"servicesTCServer\": {\"status\":\"UP\"},\"hwTCServer\": {\"status\":\"DOWN\"}");

		String outAppReqJson = String.format(OUTAPPREQ_FORMAT, date.format(format), uuid, PROJECT_NAME, storeNum,
				province, responseTime, METHOD_NAME, buildVersion,
				testHelper.createAppStatus(withStatus + withoutStatus), ipAddress, serviceStatus);

		when(env.containsProperty(anyString())).thenReturn(true);
		when(logHelper.logOutResponse(anyString())).thenReturn(1);
		whenApplicationHealth(actuatorHealth, listWithStatus, APPLICATION_ENDPOINT_WITH_STATUS_TOTAL,
				APPLICATION_ENDPOINT_WITH_STATUS);
		whenApplicationHealth(actuatorHealth, listWithoutStatus, APPLICATION_ENDPOINT_WITHOUT_STATUS_TOTAL,
				APPLICATION_ENDPOINT_WITHOUT_STATUS);
		whenWebClient(actuatorHealth, listStatusNames, listAppHealth);
		whenservice(services, serverProcessCmd);

		doAnswer(invocation -> {
			AppReq argAppReq = invocation.getArgument(0);
			OutAppReq resOutAppReq = testHelper.createOutAppReq(argAppReq, date.format(format), uuid, storeNum,
					buildVersion, ipAddress, province);
			resOutAppReq.setResponseTime(responseTime);

			return resOutAppReq;
		}).when(logHelper).reqHelper(any(AppReq.class));

		when(logHelper.logOutResponse(anyString())).thenReturn(1);

		String actual = healthMetricServiceImpl.sendApplicationHealth();

		verifyApplicationHealth(listWithStatus.size(), APPLICATION_ENDPOINT_WITH_STATUS_TOTAL,
				APPLICATION_ENDPOINT_WITH_STATUS);
		verifyApplicationHealth(listWithoutStatus.size(), APPLICATION_ENDPOINT_WITHOUT_STATUS_TOTAL,
				APPLICATION_ENDPOINT_WITHOUT_STATUS);
		verifyWebClient(actuatorHealth, listStatusNames, listAppHealth);
		verifyservice(services, serverProcessCmd);
		verify(env, times(numContainsProperty)).containsProperty(anyString());
		verify(logHelper).reqHelper(any(AppReq.class));
		verify(logHelper).logOutResponse(anyString());

		assertEquals(outAppReqJson, actual);
	}

	public void whenApplicationHealth(String actuatorHealth, List<String> listStatus, String totalEndpoint,
			String applicationEndpoint) {
		int numStatus = listStatus != null ? listStatus.size() : 0;

		when(env.getProperty(totalEndpoint)).thenReturn(String.valueOf(numStatus));

		for (int i = 0; i < numStatus; i++) {
			int index = i + 1;
			when(env.getProperty(applicationEndpoint + index + ".name")).thenReturn(listStatus.get(i));
			when(env.getProperty(applicationEndpoint + index + ".endpoint"))
					.thenReturn(actuatorHealth + listStatus.get(i));
			when(env.getProperty(applicationEndpoint + index + ".with-details")).thenReturn("false");
			when(env.getProperty(applicationEndpoint + index + ".header")).thenReturn("Authorization:Basic testauth");
		}
	}

	public void whenWebClient(String actuatorHealth, List<String> status, List<ApplicationHealth> appStatus) {
		for (int i = 0; i < Math.min(status.size(), appStatus.size()); i++) {
			when(restCallUtil.getWebClient(eq(actuatorHealth + status.get(i)),
					ArgumentMatchers.<ApplicationHealth>any(), ArgumentMatchers.<HttpEntity<String>>any(),
					any(WebClient.class), ArgumentMatchers.<WebClientFlatMapInterface<ApplicationHealth>>any(),
					ArgumentMatchers.<WebClientErrorInterface<ApplicationHealth>>any()))
							.thenReturn(Mono.just(appStatus.get(i)));
		}
	}

	public void whenservice(List<String> services, List<String> serverProcessCmd) {
		int numServers = services != null ? services.size() : 0;

		when(env.getProperty(APPLICATION_SERVICE_ENDPOINT_TOTAL)).thenReturn(String.valueOf(numServers));

		for (int i = 0; i < services.size(); i++) {
			when(env.getProperty(APPLICATION_SERVICE_ENDPOINT + String.valueOf(i + 1)))
					.thenReturn(services.get(i));
		}

		when(serverUtil.getAllProcessCmd()).thenReturn(serverProcessCmd);
	}

	public void verifyApplicationHealth(int numStatus, String totalEndpoint, String applicationEndpoint) {
		verify(env).getProperty(totalEndpoint);

		for (int i = 1; i <= numStatus; i++) {
			verify(env).getProperty(applicationEndpoint + i + ".name");
			verify(env).getProperty(applicationEndpoint + i + ".endpoint");
			verify(env).getProperty(applicationEndpoint + i + ".with-details");
		}
	}

	public void verifyWebClient(String actuatorHealth, List<String> status, List<ApplicationHealth> appStatus) {
		for (int i = 0; i < Math.min(status.size(), appStatus.size()); i++) {
			verify(restCallUtil).getWebClient(eq(actuatorHealth + status.get(i)),
					ArgumentMatchers.<ApplicationHealth>any(), ArgumentMatchers.<HttpEntity<String>>any(),
					any(WebClient.class), ArgumentMatchers.<WebClientFlatMapInterface<ApplicationHealth>>any(),
					ArgumentMatchers.<WebClientErrorInterface<ApplicationHealth>>any());
		}
	}

	public void verifyservice(List<String> services, List<String> serverProcessCmd) {
		for (int i = 0; i < services.size(); i++) {
			verify(env).getProperty(APPLICATION_SERVICE_ENDPOINT_TOTAL);
			verify(env).getProperty(APPLICATION_SERVICE_ENDPOINT + String.valueOf(i + 1));
		}

		verify(serverUtil).getAllProcessCmd();
	}
}