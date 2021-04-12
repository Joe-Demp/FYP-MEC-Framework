package ie.ucd.dempsey.mecframework.metrics.latency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.core.NodeClientLatencyRequest;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class LatencyRequestor implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(LatencyRequestor.class);
    private final Set<NodeClientLatencyRequest> requests = Collections.synchronizedSet(new HashSet<>());
    private final LatencyRequestMonitor latencyRequestMonitor;

    public LatencyRequestor(LatencyRequestMonitor monitor) {
        this.latencyRequestMonitor = monitor;
    }

    public void registerRequest(NodeClientLatencyRequest request) {
        requests.add(request);
    }

    @Override
    public void run() {
        logger.debug("Starting Latency Requests");
        for (NodeClientLatencyRequest nclRequest : requests) {
            latencyRequestMonitor.startLatencyRequest(nclRequest);
        }
    }
}
