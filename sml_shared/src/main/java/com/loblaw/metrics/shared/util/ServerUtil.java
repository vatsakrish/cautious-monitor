package com.loblaw.metrics.shared.util;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;

import com.loblaw.metrics.shared.model.ContainerProcess;
import com.loblaw.metrics.shared.model.CpuDetails;
import com.loblaw.metrics.shared.model.LoadAverage;
import com.loblaw.metrics.shared.model.RamDetails;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import oshi.software.os.OperatingSystem.ProcessFiltering;
import oshi.util.Util;

public class ServerUtil {
	private static final String MEMINFO_LOCATION = "/proc/meminfo";

	private final DecimalFormat df = new DecimalFormat("0.00");
	private SystemInfo si = new SystemInfo();
	private OperatingSystem os = si.getOperatingSystem();
	private HardwareAbstractionLayer hal = si.getHardware();
	private CentralProcessor processor = hal.getProcessor();
	private GlobalMemory memory = hal.getMemory();

	private NumberUtil numberUtil = new NumberUtil();
	private FileUtil fileUtil = new FileUtil();
	private StringUtil stringUtil = new StringUtil();

	@Value("${container-metrics.os-system}")
	private String osSystem;

	/**
	 * Get the start time of this application
	 * 
	 * @return Date representing time this application was started
	 */
	public Date getStartTime() {
		long time = ManagementFactory.getRuntimeMXBean().getStartTime();
		return new Date(time);
	}

	/**
	 * Extract the CPU usage of user, nice, system, idle, IO wait, hardware
	 * interrupt, software interrupt, and steal;
	 * 
	 * @return CpuDetails containing the percentage usage of user, nice, system,
	 *         idle, IO wait, hardware and software interrupts, and steal
	 */
	public CpuDetails getCpuDetails() {

		// Get the previous tick data
		long[] prevTicks = processor.getSystemCpuLoadTicks();

		// Wait for 1 second to get different CPU load ticks
		Util.sleep(1000);

		// Get the current tick data
		long[] ticks = processor.getSystemCpuLoadTicks();
		long user = ticks[CentralProcessor.TickType.USER.getIndex()]
				- prevTicks[CentralProcessor.TickType.USER.getIndex()];
		long nice = ticks[CentralProcessor.TickType.NICE.getIndex()]
				- prevTicks[CentralProcessor.TickType.NICE.getIndex()];
		long sys = ticks[CentralProcessor.TickType.SYSTEM.getIndex()]
				- prevTicks[CentralProcessor.TickType.SYSTEM.getIndex()];
		long idle = ticks[CentralProcessor.TickType.IDLE.getIndex()]
				- prevTicks[CentralProcessor.TickType.IDLE.getIndex()];
		long iowait = ticks[CentralProcessor.TickType.IOWAIT.getIndex()]
				- prevTicks[CentralProcessor.TickType.IOWAIT.getIndex()];
		long irq = ticks[CentralProcessor.TickType.IRQ.getIndex()]
				- prevTicks[CentralProcessor.TickType.IRQ.getIndex()];
		long softirq = ticks[CentralProcessor.TickType.SOFTIRQ.getIndex()]
				- prevTicks[CentralProcessor.TickType.SOFTIRQ.getIndex()];
		long steal = ticks[CentralProcessor.TickType.STEAL.getIndex()]
				- prevTicks[CentralProcessor.TickType.STEAL.getIndex()];
		long totalCpu = user + nice + sys + idle + iowait + irq + softirq + steal;

		// Calculate CPU percentages and set CpuDetails
		String userPerc = df.format(100d * user / totalCpu);
		String nicePerc = df.format(100d * nice / totalCpu);
		String sysPerc = df.format(100d * sys / totalCpu);
		String idlePerc = df.format(100d * idle / totalCpu);
		String ioWaitPerc = df.format(100d * iowait / totalCpu);
		String irqPerc = df.format(100d * irq / totalCpu);
		String softIrqPerc = df.format(100d * softirq / totalCpu);
		String stealPerc = df.format(100d * steal / totalCpu);

		return new CpuDetails(userPerc, nicePerc, sysPerc, idlePerc, ioWaitPerc, irqPerc, softIrqPerc, stealPerc);
	}

	/**
	 * Extract the load averages for 1, 5, and 15 minutes
	 * 
	 * @return LoadAverage containing the load averages across 1, 5, and 15 minutes
	 */
	public LoadAverage getLoadAverage() {
		if (osSystem.toLowerCase().contains("windows"))
			return null;
		else {
			double[] loadAveragesArr = processor.getSystemLoadAverage(3);
			LoadAverage loadAverage = new LoadAverage();

			loadAverage.setOneMinute(loadAveragesArr[0]);
			loadAverage.setFiveMinutes(loadAveragesArr[1]);
			loadAverage.setFifteenMinutes(loadAveragesArr[2]);

			return loadAverage;
		}
	}

	/**
	 * Extract the disk utilization on the system
	 * 
	 * @param checkDisks - List of disk mounts of extract
	 * @return a Map of checkDisks mounts on the system to their respective used
	 *         disk space
	 */
	public Map<String, String> getDiskSpace(List<String> checkDisks) {
		Map<String, String> diskSpace = new HashMap<>();
		FileSystem fileSystem = os.getFileSystem();
		List<OSFileStore> fileStores = fileSystem.getFileStores();

		// Iterate over all the disk spaces on the server
		for (OSFileStore disk : fileStores) {
			// Extract the name of the mount
			String diskName = disk.getMount();

			if (checkDisks.contains(diskName)) {
				long totalDiskSpace = disk.getTotalSpace();
				long usedDiskSpace = totalDiskSpace - disk.getUsableSpace();

				// Calculate the used disk percentage
				double usedPercentage = usedDiskSpace / (double) totalDiskSpace * 100;

				diskSpace.put(diskName, String.format("%.0f", usedPercentage));
			}
		}

		return diskSpace;
	}

	/**
	 * Extract the memory utilization of the system from fileMemInfo
	 * 
	 * @return RamDetails containing the available, used and total memory of the
	 *         system
	 */
	public RamDetails getRamDetails() {
		List<String> memInfo = new ArrayList<>();
		RamDetails ramDetails = new RamDetails();
		long totalMem = 0;
		long freeMem = 0;
		long buffMem = 0;
		long cachedMem = 0;

		if (osSystem.toLowerCase().contains("windows")) {
			OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
			if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
				com.sun.management.OperatingSystemMXBean nativeOsBean = (com.sun.management.OperatingSystemMXBean) osBean;
				totalMem = nativeOsBean.getTotalPhysicalMemorySize();
				freeMem = nativeOsBean.getFreePhysicalMemorySize();

				ramDetails.setAvailableRam(df.format(numberUtil.bytesToGb(freeMem)));
				ramDetails.setUsedRam(df.format(numberUtil.bytesToGb(totalMem - freeMem)));
				ramDetails.setTotalRam(df.format(numberUtil.bytesToGb(totalMem)));
				ramDetails.setBuffersRam(df.format(numberUtil.bytesToGb(buffMem)));
				ramDetails.setCachedRam(df.format(numberUtil.bytesToGb(cachedMem)));
			}
		} else {
			// Extract memory details from /proc/meminfo
			int retValue = fileUtil.readFile(MEMINFO_LOCATION, memInfo);

			// File was read successfully
			if (retValue == 1) {
				for (String line : memInfo) {
					Long tempMem;
					line = line.replaceAll("\\s+", ",");
					String[] splitLine = line.split(",");

					if (splitLine.length > 1 && stringUtil.parseLong(splitLine[1]) != null) {
						tempMem = stringUtil.parseLong(splitLine[1]);

						switch (splitLine[0]) {
						case "MemTotal:":
							totalMem = tempMem;
							break;
						case "MemFree:":
							freeMem = tempMem;
							break;
						case "Buffers:":
							buffMem = tempMem;
							break;
						case "Cached:":
							cachedMem = tempMem;
							break;
						default:
							break;
						}
					}
				}

				ramDetails.setAvailableRam(df.format(numberUtil.kilobytesToGb(freeMem)));
				ramDetails.setUsedRam(df.format(numberUtil.kilobytesToGb(totalMem - freeMem)));
				ramDetails.setTotalRam(df.format(numberUtil.kilobytesToGb(totalMem)));
				ramDetails.setBuffersRam(df.format(numberUtil.kilobytesToGb(buffMem)));
				ramDetails.setCachedRam(df.format(numberUtil.kilobytesToGb(cachedMem)));
			}
		}

		return ramDetails;
	}

	/**
	 * Extract limit processes in sort order
	 * 
	 * @param sort  - Comparator representing the ordering of processes
	 * @param limit - int representing number of processes to extract
	 * @return - List of processes based on sort and limit
	 */
	public List<ContainerProcess> getContainerProcess(Comparator<OSProcess> sort, int limit) {
		List<ContainerProcess> topProcesses = new ArrayList<>();
		List<OSProcess> procs = os.getProcesses(ProcessFiltering.ALL_PROCESSES, sort, limit);

		for (int i = 0; i < procs.size() && i < limit; i++) {
			OSProcess p = procs.get(i);
			// Calculate the memory usage and CPU usage percentage
			String memoryUsage = df.format(100d * p.getResidentSetSize() / memory.getTotal());
			String cpuUsage = df.format(100d * (p.getKernelTime() + p.getUserTime()) / p.getUpTime());
			// Extract the user who initiated the process and the name of its process
			ContainerProcess process = new ContainerProcess();
			process.setUser(p.getUser());
			process.setName(p.getName());
			process.setMemoryUsage(memoryUsage);
			process.setCpuUsage(cpuUsage);

			topProcesses.add(process);
		}

		return topProcesses;
	}

	/**
	 * Extract the CMDs of all processes running on the system
	 * 
	 * @return - List of CMDs of all processes running on the system
	 */
	public List<String> getAllProcessCmd() {
		List<OSProcess> procs = os.getProcesses();

		if (osSystem.toLowerCase().contains("windows"))
			return procs.stream().map(OSProcess::getName).collect(Collectors.toList());
		else
			return procs.stream().map(OSProcess::getCommandLine).collect(Collectors.toList());
	}
}
