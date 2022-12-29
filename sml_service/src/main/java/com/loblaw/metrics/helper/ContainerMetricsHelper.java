package com.loblaw.metrics.helper;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.loblaw.metrics.shared.model.ContainerProcess;
import com.loblaw.metrics.shared.model.CpuDetails;
import com.loblaw.metrics.shared.model.LoadAverage;
import com.loblaw.metrics.shared.model.OutContainerRes;
import com.loblaw.metrics.shared.model.RamDetails;
import com.loblaw.metrics.shared.util.ServerUtil;
import com.loblaw.metrics.shared.util.StringUtil;

import lombok.extern.slf4j.Slf4j;
import oshi.software.os.OperatingSystem.ProcessSorting;

@Component
@Slf4j
public class ContainerMetricsHelper {
	@Autowired
	LogHelper logHelper;

	@Autowired
	ServerUtil serverUtil;

	@Autowired
	StringUtil stringUtil;

	@Value("#{'${container-metrics.drives}'.split(',')}")
	List<String> checkDisks;

	/**
	 * Extract system, CPU, memory, disk and the top five processes by CPU usage
	 * details on this server
	 * 
	 * @return - ContainerMetrics representing the start time of this application,
	 *         CPU usage details, RAM usage details, disk utilization of specified
	 *         disks, and the top five processes by CPU usage
	 */
	public OutContainerRes getServerDetails() {
		OutContainerRes containerMetrics = new OutContainerRes();

		// Extract system, CPU, memory, disk and process details
		try {
			setSystemDetails(containerMetrics);

			setCpuDetails(containerMetrics);

			setMemoryDetails(containerMetrics);

			setDiskDetails(containerMetrics);

			setTopFiveProcesses(containerMetrics);

			log.debug("Container Metrics: " + containerMetrics);
		} catch (Exception e) {
			log.error("Container Metrics Helper : Exception in getServerDetails : {}", e.toString(), e);
		}
		return containerMetrics;
	}

	/**
	 * Send container metrics to Splunk
	 * 
	 * @param outContainerRes - OutContainerMetrics containing metrics on the
	 *                        server's CPU, disk, memory, load average, application
	 *                        start time, and most CPU intensive applications.
	 */
	public void sendContainerMetricsToSplunk(OutContainerRes outContainerRes) {
		log.info("Starting to log container metrics to file");

		// Convert ContainerMetrics to JSON
		String strRes = stringUtil.mapToString(outContainerRes);

		// Wrap ContainerMetrics as an OutAppReq and log as a file to be picked up by
		// Splunk Forwarder
		if (strRes != null) {
			log.debug("Container metrics response: " + strRes);
			// Log JSON
			int outCount = logHelper.logOutResponse(strRes);
			log.info(String.format("Finished logging container metrics to file (OC: %d)", outCount));
		} else {
			log.info("No container metrics details to send");
		}
	}

	/**
	 * Set containerMetrics disk utilization for disks mounts identified by
	 * checkDisks
	 * 
	 * @param containerMetrics - ContainerMetrics representing the container metrics
	 *                         of this server
	 */
	private void setDiskDetails(OutContainerRes containerMetrics) {
		Map<String, String> diskDetails = serverUtil.getDiskSpace(checkDisks);

		containerMetrics.setDiskUtilization(diskDetails);
	}

	/**
	 * Set containerMetrics ram details for available ram, used ram, and total ram
	 * 
	 * @param containerMetrics - ContainterMetrics representing the container
	 *                         metrics of this server
	 */
	private void setMemoryDetails(OutContainerRes containerMetrics) {
		// Extract the available ram, used ram and total ram
		RamDetails ramDetails = serverUtil.getRamDetails();

		log.debug("Ram details: " + ramDetails);
		containerMetrics.setRamDetails(ramDetails);
	}

	/**
	 * Set containerMetrics idle CPU and IO wait time percentages, and load averages
	 * for 1, 5, and 15 minutes
	 * 
	 * @param containerMetrics - ContainerMetrics representing the container metrics
	 *                         of this server
	 */
	private void setCpuDetails(OutContainerRes containerMetrics) {
		CpuDetails cpuDetails = serverUtil.getCpuDetails();
		containerMetrics.setCpuDetails(cpuDetails);

		log.debug("CPU Details: " + cpuDetails);

		// Set load averages for 1 minute, 5 minutes, and 15 minutes
		LoadAverage loadAverage = serverUtil.getLoadAverage();
		containerMetrics.setLoadAverage(loadAverage);

		log.debug("Load average: " + loadAverage);
	}

	/**
	 * Set containerMetrics soft start time to the start time of this application
	 * 
	 * @param containerMetrics - ContainerMetrics representing the container metrics
	 *                         of this server
	 */
	private void setSystemDetails(OutContainerRes containerMetrics) {
		String strStartTime = "";

		// Extract the start time of this application
		Date serverStartTime = serverUtil.getStartTime();

		if (serverStartTime != null)
			strStartTime = serverStartTime.toString();

		containerMetrics.setSoftStartTime(strStartTime);

		log.debug("System start time: " + strStartTime);
	}

	/**
	 * Set containerMetrics top five processes, extracting its users, name, memory
	 * usage, and CPU usage
	 * 
	 * @param containerMetrics - ContainerMetrics representing the container metrics
	 *                         of this server
	 */
	private void setTopFiveProcesses(OutContainerRes containerMetrics) {
		List<ContainerProcess> topFiveProcesses = serverUtil.getContainerProcess(ProcessSorting.CPU_DESC, 5);
		containerMetrics.setTopFiveProcesses(topFiveProcesses);

		log.debug("Top 5 processes: " + topFiveProcesses);
	}
}
