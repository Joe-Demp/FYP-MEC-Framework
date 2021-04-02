package ie.ucd.dempsey.mecframework.metrics;

import ie.ucd.dempsey.mecframework.metrics.latency.LatencyRequestMonitor;
import ie.ucd.dempsey.mecframework.metrics.latency.LatencyRequestor;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;
import service.core.NodeClientLatencyRequest;
import service.core.NodeInfo;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;

public class ServiceNodeMetrics {
    private final ScheduledExecutorService scheduleService = Executors.newScheduledThreadPool(5);

    // class fields
    private final SystemInfo nodeSystem = new SystemInfo();
    private final HardwareAbstractionLayer hal = nodeSystem.getHardware();
    private final OperatingSystem os = nodeSystem.getOperatingSystem();
    private final LatencyRequestMonitor latencyMonitor = new LatencyRequestMonitor();
    private final LatencyRequestor latencyRequestor = new LatencyRequestor(latencyMonitor);
    private final long pingDelay;

    // metric values
    private final List<Double> cpuLoad = new ArrayList<>();
    private final List<Double> memoryLoad = new ArrayList<>();
    private final List<Long> mainMemory = new ArrayList<>();
    private final List<Long> storage = new ArrayList<>();
    private long[] cpuTicks = hal.getProcessor().getSystemCpuLoadTicks();

    public ServiceNodeMetrics(long pingDelay) {
        this.pingDelay = pingDelay;

//        scheduleService.scheduleAtFixedRate(() -> {
//            // CPU
//            double load = hal.getProcessor().getSystemCpuLoadBetweenTicks(cpuTicks);
//            synchronized (cpuLoad) {
//                cpuLoad.add(load);
//            }
//            cpuTicks = hal.getProcessor().getSystemCpuLoadTicks();
//
//            // Memory
//            double totalMemory = hal.getMemory().getTotal();
//            long availableMemory = hal.getMemory().getAvailable();
//            double fractionMemoryUsed = 1.0 - (availableMemory / totalMemory);
//
//            synchronized (memoryLoad) {
//                memoryLoad.add(fractionMemoryUsed);
//            }
//            synchronized (mainMemory) {
//                mainMemory.add(availableMemory);
//            }
//        }, 5, 1, TimeUnit.SECONDS);
//
//        scheduleService.scheduleAtFixedRate(() -> {
//            long usableSpace = os.getFileSystem().getFileStores().stream()
//                    .mapToLong(OSFileStore::getUsableSpace)
//                    .sum();
//            synchronized (storage) {
//                storage.add(usableSpace);
//            }
//        }, 10, 60, TimeUnit.SECONDS);

        scheduleService.scheduleAtFixedRate(latencyRequestor, 3, 5, TimeUnit.SECONDS);
        scheduleService.scheduleAtFixedRate(latencyMonitor, 5, 5, TimeUnit.SECONDS);
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

        Map<UUID, List<Long>> delayedLatencies = latenciesWithDelay(latencyMonitor.takeLatencySnapshot());
        nodeInfo.setLatencies(delayedLatencies);
    }

    private Map<UUID, List<Long>> latenciesWithDelay(Map<UUID, List<Long>> latencies) {
        Map<UUID, List<Long>> delayedLatencies = new HashMap<>();

        for (Map.Entry<UUID, List<Long>> entry : latencies.entrySet()) {
            List<Long> delayed = entry.getValue().stream()
                    .map(latency -> latency + pingDelay)
                    .collect(toList());
            delayedLatencies.put(entry.getKey(), delayed);
        }
        return Collections.unmodifiableMap(delayedLatencies);
    }

    public void registerLatencyRequest(NodeClientLatencyRequest request) {
        latencyRequestor.registerRequest(request);
    }
}
