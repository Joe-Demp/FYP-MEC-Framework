package ie.ucd.dempsey.mecframework.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;
import service.core.NodeInfo;

// todo fill in
public class ServiceNodeMetrics {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    // CPU
    // Main Memory
    // Storage
    // Latencies

    private final SystemInfo nodeSystem = new SystemInfo();
    private final HardwareAbstractionLayer hal = nodeSystem.getHardware();
    private final OperatingSystem os = nodeSystem.getOperatingSystem();

    public void populateNodeInfo(NodeInfo nodeInfo) {
        logger.warn("populateNodeInfo NOT IMPLEMENTED!");
    }
}
