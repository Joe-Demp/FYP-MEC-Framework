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

import java.util.ArrayList;
import java.util.List;
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
    private final List<Double> cpuLoad = new ArrayList<>();
    private final List<Double> memoryLoad = new ArrayList<>();
    private final List<Long> mainMemory = new ArrayList<>();
    private final List<Long> storage = new ArrayList<>();
    private long[] cpuTicks = hal.getProcessor().getSystemCpuLoadTicks();

    public ServiceNodeMetrics() {
        // todo extract these out into separate methods
        scheduleService.scheduleAtFixedRate(() -> {
            double load = hal.getProcessor().getSystemCpuLoadBetweenTicks(cpuTicks);
            synchronized (cpuLoad) {
                cpuLoad.add(load);
            }
            cpuTicks = hal.getProcessor().getSystemCpuLoadTicks();
        }, 3, 5, TimeUnit.SECONDS);

        scheduleService.scheduleAtFixedRate(() -> {
            double totalMemory = hal.getMemory().getTotal();
            long availableMemory = hal.getMemory().getAvailable();
            double fractionMemoryUsed = 1.0 - (availableMemory / totalMemory);

            synchronized (memoryLoad) {
                memoryLoad.add(fractionMemoryUsed);
            }
            synchronized (mainMemory) {
                mainMemory.add(availableMemory);
            }
        }, 5, 5, TimeUnit.SECONDS);

        scheduleService.scheduleAtFixedRate(() -> {
            long usableSpace = os.getFileSystem().getFileStores().stream()
                    .mapToLong(OSFileStore::getUsableSpace)
                    .sum();
            synchronized (storage) {
                storage.add(usableSpace);
            }
        }, 10, 60, TimeUnit.SECONDS);
    }

    // todo make this more concise
    //  extract into different classes
    public void populateNodeInfo(NodeInfo nodeInfo) {
        List<Double> cpuCopy;
        synchronized (cpuLoad) {
            cpuCopy = new ArrayList<>(cpuLoad);
            cpuLoad.clear();
        }
        nodeInfo.setCpuLoad(cpuCopy);

        List<Double> memoryLoadCopy;
        synchronized (memoryLoad) {
            memoryLoadCopy = new ArrayList<>(memoryLoad);
            memoryLoad.clear();
        }
        nodeInfo.setMemoryLoad(memoryLoadCopy);

        List<Long> mainMemoryCopy;
        synchronized (mainMemory) {
            mainMemoryCopy = new ArrayList<>(mainMemory);
            mainMemory.clear();
        }
        nodeInfo.setMainMemory(mainMemoryCopy);

        List<Long> storageCopy;
        synchronized (storage) {
            storageCopy = new ArrayList<>(storage);
            storage.clear();
        }
        nodeInfo.setStorage(storageCopy);
    }

    public void registerLatencyRequest(NodeClientLatencyRequest request) {
        latencyRequestor.registerRequest(request);
    }
}
