package ie.ucd.dempsey.mecframework.metrics;

import ie.ucd.dempsey.mecframework.metrics.latency.LatencyRequestMonitor;
import ie.ucd.dempsey.mecframework.metrics.latency.LatencyRequestor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import service.core.NodeClientLatencyRequest;
import service.core.NodeInfo;

import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServiceNodeMetrics {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ScheduledExecutorService scheduleService = Executors.newScheduledThreadPool(5);

    // class fields
    private final SystemInfo nodeSystem = new SystemInfo();
    private final HardwareAbstractionLayer hal = nodeSystem.getHardware();
    private final OperatingSystem os = nodeSystem.getOperatingSystem();
    private final LatencyRequestMonitor latencyMonitor = new LatencyRequestMonitor();
    private final LatencyRequestor latencyRequestor = new LatencyRequestor(latencyMonitor);

    // metric values
    private final Vector<Double> cpuLoad = new Vector<>();
    private final Vector<Double> memoryLoad = new Vector<>();
    private final Vector<Long> mainMemory = new Vector<>();
    private final Vector<Long> storage = new Vector<>();
    private long[] cpuTicks = hal.getProcessor().getSystemCpuLoadTicks();

    public ServiceNodeMetrics() {
        // start scheduled tasks to check the CPU, Main Memory, Storage, Latencies?
        scheduleService.scheduleAtFixedRate(() -> {
            double load = hal.getProcessor().getSystemCpuLoadBetweenTicks(cpuTicks);
            cpuLoad.add(load);
            cpuTicks = hal.getProcessor().getSystemCpuLoadTicks();
        }, 3, 5, TimeUnit.SECONDS);

        scheduleService.scheduleAtFixedRate(() -> {
            double totalMemory = hal.getMemory().getTotal();
            long availableMemory = hal.getMemory().getAvailable();
            double fractionMemoryUsed = 1.0 - (availableMemory / totalMemory);

            memoryLoad.add(fractionMemoryUsed);
            mainMemory.add(availableMemory);
        }, 5, 5, TimeUnit.SECONDS);

        scheduleService.scheduleAtFixedRate(() -> {
            long usableSpace = os.getFileSystem().getFileStores().stream()
                    .mapToLong(OSFileStore::getUsableSpace)
                    .sum();
            storage.add(usableSpace);
        }, 10, 60, TimeUnit.SECONDS);
    }

    public void populateNodeInfo(NodeInfo nodeInfo) {
        logger.warn("populateNodeInfo NOT IMPLEMENTED!");
    }

    public void registerLatencyRequest(NodeClientLatencyRequest request) {
        latencyRequestor.registerRequest(request);
    }
}
