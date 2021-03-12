package service.cloud.connections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.core.NodeClientLatencyRequest;

import java.util.List;
import java.util.Vector;

public class LatencyRequestor implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(LatencyRequestor.class);
    private final List<NodeClientLatencyRequest> requests = new Vector<>();
    private final LatencyRequestMonitor latencyRequestMonitor;

    public LatencyRequestor(LatencyRequestMonitor monitor) {
        this.latencyRequestMonitor = monitor;
    }

    public void registerRequest(NodeClientLatencyRequest request) {
        requests.add(request);
    }

    @Override
    public void run() {
        logger.debug("Running LatencyRequestor");
        for (NodeClientLatencyRequest nclRequest : requests) {
            latencyRequestMonitor.startLatencyRequest(nclRequest);
        }
    }
}
