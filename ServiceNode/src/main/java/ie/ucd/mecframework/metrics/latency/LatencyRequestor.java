package ie.ucd.mecframework.metrics.latency;

import service.core.NodeClientLatencyRequest;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class LatencyRequestor implements Runnable {
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
        for (NodeClientLatencyRequest nclRequest : requests) {
            latencyRequestMonitor.startLatencyRequest(nclRequest);
        }
    }
}
