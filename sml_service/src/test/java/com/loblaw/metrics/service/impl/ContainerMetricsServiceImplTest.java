package com.loblaw.metrics.service.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.loblaw.metrics.config.SmlConfiguration;
import com.loblaw.metrics.exception.UnexpectedContainerMetricsException;
import com.loblaw.metrics.helper.ContainerMetricsHelper;
import com.loblaw.metrics.helper.LogHelper;
import com.loblaw.metrics.shared.model.ContainerProcess;
import com.loblaw.metrics.shared.model.CpuDetails;
import com.loblaw.metrics.shared.model.LoadAverage;
import com.loblaw.metrics.shared.model.OutContainerRes;
import com.loblaw.metrics.shared.model.RamDetails;
import com.loblaw.metrics.shared.util.ServerUtil;
import com.loblaw.metrics.shared.util.StringUtil;

import oshi.software.os.OperatingSystem.ProcessSorting;

@RunWith(SpringJUnit4ClassRunner.class)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ContainerMetricsServiceImpl.class, ContainerMetricsHelper.class,
		SmlConfiguration.class })
@TestPropertySource(properties = { "container-metrics.drives=/home", "rest-template.timeout.connect=0",
		"rest-template.timeout.read=0" })
public class ContainerMetricsServiceImplTest {

	@MockBean
	LogHelper logHelper;

	@MockBean
	ServerUtil serverUtil;

	@Autowired
	StringUtil stringUtil;

	@Autowired
	ContainerMetricsHelper containerMetricsHelper;

	@Autowired
	ContainerMetricsServiceImpl containerMetricsServiceImpl;

	@Test
	@DisplayName("When container metrics are successfully extracted - then return the metrics in JSON format")
	public void sendContainerMetrics_metricsExtractedSuccessfully_ReturnsContainMetricsJson() {
		boolean sendToSplunk = true;
		String date = "2021/03/01 10:30:54";
		String store = "3144";
		String disk = "/home";

		Calendar calendar = Calendar.getInstance();
		calendar.set(2021, 2, 1, 0, 0, 24);
		Date softStartDate = calendar.getTime();

		LoadAverage loadAverage = new LoadAverage(4.5, 3.2, 2.1);
		CpuDetails cpuDetails = new CpuDetails("1.17", "0.00", "2.08", "95.85", "0.26", "0.00", "0.00", "0.65");
		RamDetails ramDetails = new RamDetails("3.75", "27.57", "31.31", "0.76", "8.19");

		List<String> checkDisks = new ArrayList<>();
		checkDisks.add(disk);
		Map<String, String> diskUtilization = new HashMap<>();
		diskUtilization.put(disk, "96");

		List<ContainerProcess> topFiveProcesses = new ArrayList<>();
		ContainerProcess cp1 = new ContainerProcess("csuser", "java", "9.54", "12.31");
		ContainerProcess cp2 = new ContainerProcess("db2inst1", "db2sysc", "17.27", "4.20");
		ContainerProcess cp3 = new ContainerProcess("mqm", "amqrmppa", "0.07", "2.95");
		topFiveProcesses.add(cp1);
		topFiveProcesses.add(cp2);
		topFiveProcesses.add(cp3);
		String expected = "{\"dateTime\":\"2021/03/01 10:30:54\",\"store\":\"3144\",\"methodName\":\"container_metrics\",\"soft_start_time\":\"Mon Mar 01 00:00:24 EST 2021\",\"load_average\":{\"1minute\":4.5,\"5minutes\":3.2,\"15minutes\":2.1},\"cpu_details\":{\"user\":\"1.17\",\"nice\":\"0.00\",\"sys\":\"2.08\",\"idle\":\"95.85\",\"irq\":\"0.00\",\"steal\":\"0.65\",\"io_wait\":\"0.26\",\"soft_irq\":\"0.00\"},\"ram_details\":{\"available_ram\":\"3.75\",\"used_ram\":\"27.57\",\"total_ram\":\"31.31\",\"buffers_ram\":\"0.76\",\"cached_ram\":\"8.19\"},\"disk_utilization\":{\"/home\":\"96\"},\"top_five_processes\":[{\"user\":\"csuser\",\"name\":\"java\",\"memory_usage\":\"9.54\",\"cpu_usage\":\"12.31\"},{\"user\":\"db2inst1\",\"name\":\"db2sysc\",\"memory_usage\":\"17.27\",\"cpu_usage\":\"4.20\"},{\"user\":\"mqm\",\"name\":\"amqrmppa\",\"memory_usage\":\"0.07\",\"cpu_usage\":\"2.95\"}]}";

		when(serverUtil.getContainerProcess(ProcessSorting.CPU_DESC, 5)).thenReturn(topFiveProcesses);
		when(serverUtil.getCpuDetails()).thenReturn(cpuDetails);
		when(serverUtil.getDiskSpace(checkDisks)).thenReturn(diskUtilization);
		when(serverUtil.getLoadAverage()).thenReturn(loadAverage);
		when(serverUtil.getRamDetails()).thenReturn(ramDetails);
		when(serverUtil.getStartTime()).thenReturn(softStartDate);

		doAnswer(invocation -> {
			OutContainerRes arg1 = invocation.getArgument(0);
			String arg2 = invocation.getArgument(1);
			arg1.setMethodName(arg2);
			arg1.setDateTime(date);
			arg1.setStore(store);

			return null;
		}).when(logHelper).updateOutContainerRes(any(OutContainerRes.class), anyString());

		String actual = containerMetricsServiceImpl.sendContainerMetrics(sendToSplunk);

		verify(serverUtil).getContainerProcess(ProcessSorting.CPU_DESC, 5);
		verify(serverUtil).getCpuDetails();
		verify(serverUtil).getDiskSpace(checkDisks);
		verify(serverUtil).getLoadAverage();
		verify(serverUtil).getRamDetails();
		verify(serverUtil).getStartTime();

		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("When several container metrics fields cannot be extracted - then return the metrics in JSON format without those fields")
	public void sendContainerMetrics_someNullContainMetricsFields_ReturnsContainMetricsJson() {
		boolean sendToSplunk = true;
		String date = "2021/03/01 09:01:23";
		String store = "3144";
		String disk = "/home";
		
		Calendar calendar = Calendar.getInstance();
		calendar.set(2021, 2, 1, 0, 0, 24);
		Date softStartDate = calendar.getTime();
		LoadAverage loadAverage = new LoadAverage(4.5, 3.2, 2.1);
		RamDetails ramDetails = new RamDetails("15.93", "15.84", "31.77", "0.92", "5.32");
		List<String> checkDisks = new ArrayList<>();
		checkDisks.add(disk);

		CpuDetails cpuDetails = null;
		Map<String, String> diskUtilization = null;
		List<ContainerProcess> topFiveProcesses = null;

		String expected = "{\"dateTime\":\"2021/03/01 09:01:23\",\"store\":\"3144\",\"methodName\":\"container_metrics\",\"soft_start_time\":\"Mon Mar 01 00:00:24 EST 2021\",\"load_average\":{\"1minute\":4.5,\"5minutes\":3.2,\"15minutes\":2.1},\"ram_details\":{\"available_ram\":\"15.93\",\"used_ram\":\"15.84\",\"total_ram\":\"31.77\",\"buffers_ram\":\"0.92\",\"cached_ram\":\"5.32\"}}";

		when(serverUtil.getContainerProcess(ProcessSorting.CPU_DESC, 5)).thenReturn(topFiveProcesses);
		when(serverUtil.getCpuDetails()).thenReturn(cpuDetails);
		when(serverUtil.getDiskSpace(checkDisks)).thenReturn(diskUtilization);
		when(serverUtil.getLoadAverage()).thenReturn(loadAverage);
		when(serverUtil.getRamDetails()).thenReturn(ramDetails);
		when(serverUtil.getStartTime()).thenReturn(softStartDate);

		doAnswer(invocation -> {
			OutContainerRes arg1 = invocation.getArgument(0);
			String arg2 = invocation.getArgument(1);
			arg1.setMethodName(arg2);
			arg1.setDateTime(date);
			arg1.setStore(store);

			return null;
		}).when(logHelper).updateOutContainerRes(any(OutContainerRes.class), anyString());

		String actual = containerMetricsServiceImpl.sendContainerMetrics(sendToSplunk);

		verify(serverUtil).getContainerProcess(ProcessSorting.CPU_DESC, 5);
		verify(serverUtil).getCpuDetails();
		verify(serverUtil).getDiskSpace(checkDisks);
		verify(serverUtil).getLoadAverage();
		verify(serverUtil).getRamDetails();
		verify(serverUtil).getStartTime();

		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("When none of the container metrics fields could be extracted - then return an empty JSON")
	public void sendContainerMetrics_allNullContainerMetricsFields_ReturnsContainMetricsJson() {
		boolean sendToSplunk = true;
		String date = "2021/03/01 17:24:11";
		String store = "3144";
		String disk = "/home";
		List<String> checkDisks = new ArrayList<>();
		checkDisks.add(disk);

		CpuDetails cpuDetails = null;
		Date softStartDate = null;
		Map<String, String> diskUtilization = null;
		LoadAverage loadAverage = null;
		RamDetails ramDetails = null;
		List<ContainerProcess> topFiveProcesses = null;

		String expected = "{\"dateTime\":\"2021/03/01 17:24:11\",\"store\":\"3144\",\"methodName\":\"container_metrics\"}";

		when(serverUtil.getContainerProcess(ProcessSorting.CPU_DESC, 5)).thenReturn(topFiveProcesses);
		when(serverUtil.getCpuDetails()).thenReturn(cpuDetails);
		when(serverUtil.getDiskSpace(checkDisks)).thenReturn(diskUtilization);
		when(serverUtil.getLoadAverage()).thenReturn(loadAverage);
		when(serverUtil.getRamDetails()).thenReturn(ramDetails);
		when(serverUtil.getStartTime()).thenReturn(softStartDate);

		doAnswer(invocation -> {
			OutContainerRes arg1 = invocation.getArgument(0);
			String arg2 = invocation.getArgument(1);
			arg1.setMethodName(arg2);
			arg1.setDateTime(date);
			arg1.setStore(store);

			return null;
		}).when(logHelper).updateOutContainerRes(any(OutContainerRes.class), anyString());

		String actual = containerMetricsServiceImpl.sendContainerMetrics(sendToSplunk);

		verify(serverUtil).getContainerProcess(ProcessSorting.CPU_DESC, 5);
		verify(serverUtil).getCpuDetails();
		verify(serverUtil).getDiskSpace(checkDisks);
		verify(serverUtil).getLoadAverage();
		verify(serverUtil).getRamDetails();
		verify(serverUtil).getStartTime();

		assertEquals(expected, actual);
	}

	@Test(expected = UnexpectedContainerMetricsException.class)
	@DisplayName("When updating out container res an exception is thrown - then catch the exception and throw an UnexpectedContainerMetricsException")
	public void sendContainerMetrics_logHelperThrowsException_thenThrowUnexpectedContainerMetricsException() {
		boolean sendToSplunk = true;
		doThrow(new RuntimeException()).when(logHelper).updateOutContainerRes(any(OutContainerRes.class), anyString());
		containerMetricsServiceImpl.sendContainerMetrics(sendToSplunk);
	}

	@Test
	@DisplayName("When container metrics are successfully extracted on a schedule")
	public void scheduledSendContainerMetrics() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(2021, 3, 1, 10, 0, 32);
		Date softStartDate = calendar.getTime();
		String date = "2021/03/01 10:00:32";
		String store = "3144";
		String disk = "/home";
		
		Map<String, String> diskUtilization = new HashMap<>();
		LoadAverage loadAverage = new LoadAverage(4.5, 3.2, 2.1);
		CpuDetails cpuDetails = new CpuDetails("1.17", "0.00", "2.08", "95.85", "0.26", "0.00", "0.00", "0.65");
		RamDetails ramDetails = new RamDetails("15.93", "15.84", "31.77", "0.32", "3.83");

		List<String> checkDisks = new ArrayList<>();
		checkDisks.add(disk);
		List<ContainerProcess> topFiveProcesses = new ArrayList<>();
		ContainerProcess cp1 = new ContainerProcess("csuser", "java", "9.54", "12.31");
		ContainerProcess cp2 = new ContainerProcess("db2inst1", "db2sysc", "17.27", "4.20");
		ContainerProcess cp3 = new ContainerProcess("mqm", "amqrmppa", "0.07", "2.95");
		diskUtilization.put(disk, "96");
		topFiveProcesses.add(cp1);
		topFiveProcesses.add(cp2);
		topFiveProcesses.add(cp3);

		when(logHelper.incInResponse()).thenReturn(1);
		when(serverUtil.getContainerProcess(ProcessSorting.CPU_DESC, 5)).thenReturn(topFiveProcesses);
		when(serverUtil.getCpuDetails()).thenReturn(cpuDetails);
		when(serverUtil.getDiskSpace(checkDisks)).thenReturn(diskUtilization);
		when(serverUtil.getLoadAverage()).thenReturn(loadAverage);
		when(serverUtil.getRamDetails()).thenReturn(ramDetails);
		when(serverUtil.getStartTime()).thenReturn(softStartDate);

		doAnswer(invocation -> {
			OutContainerRes arg1 = invocation.getArgument(0);
			String arg2 = invocation.getArgument(1);
			arg1.setMethodName(arg2);
			arg1.setDateTime(date);
			arg1.setStore(store);

			return null;
		}).when(logHelper).updateOutContainerRes(any(OutContainerRes.class), anyString());

		containerMetricsServiceImpl.scheduledSendContainerMetrics();

		verify(logHelper).incInResponse();
		verify(serverUtil).getContainerProcess(ProcessSorting.CPU_DESC, 5);
		verify(serverUtil).getCpuDetails();
		verify(serverUtil).getDiskSpace(checkDisks);
		verify(serverUtil).getLoadAverage();
		verify(serverUtil).getRamDetails();
		verify(serverUtil).getStartTime();
	}

	@Test
	@DisplayName("When extracting container metrics getStartTime throws an exception - then catch the exception")
	public void scheduledSendContainerMetrics_getServerDetailsThrowsException() {
		boolean sendToSplunk = false;

		when(serverUtil.getStartTime()).thenThrow(new RuntimeException());
		containerMetricsServiceImpl.sendContainerMetrics(sendToSplunk);
	}
}