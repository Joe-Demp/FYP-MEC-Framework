package service.orchestrator.nodes;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.core.NodeInfo;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.DoubleStream;

import static java.util.Objects.nonNull;

/**
 * A class to represent Service Nodes as they are visible to the Orchestrator.
 */
public class ServiceNode {
    private static final Logger logger = LoggerFactory.getLogger(ServiceNode.class);

    public UUID uuid;
    public WebSocket webSocket;
    public boolean serviceRunning;
    public Deque<Double> cpuLoad = new ArrayDeque<>();
    public Deque<Double> ramLoad = new ArrayDeque<>();
    public Deque<Long> storage = new ArrayDeque<>();
    public Deque<Long> mainMemory = new ArrayDeque<>();
    private Map<UUID, List<Long>> mobileClientLatencies = new Hashtable<>();
    private AtomicReference<State> stateAtomRef = new AtomicReference<>(State.STABLE);

    public boolean trustworthy = true;
    public URI serviceHostAddress;

    public ServiceNode(UUID uuid, WebSocket webSocket) {
        this.uuid = uuid;
        this.webSocket = webSocket;
    }

    public void update(NodeInfo nodeInfo) {
        // take the new values from nodeInfo and add them to the ServiceNode's fields
        if (!uuid.equals(nodeInfo.getUuid())) {
            logger.warn("Tried to update ServiceNode {} with NodeInfo {}", uuid, nodeInfo.getUuid());
            return;
        }

        recordWebSocket(nodeInfo);
        recordMetrics(nodeInfo);
        recordOtherFields(nodeInfo);
    }

    private void recordWebSocket(NodeInfo nodeInfo) {
        WebSocket otherWebSocket = nodeInfo.getWebSocket();
        webSocket = nonNull(otherWebSocket) ? otherWebSocket : webSocket;
    }

    private void recordMetrics(NodeInfo nodeInfo) {
        cpuLoad.addAll(nodeInfo.getCpuLoad());
        ramLoad.addAll(nodeInfo.getMemoryLoad());
        storage.addAll(nodeInfo.getStorage());
        mainMemory.addAll(nodeInfo.getMainMemory());
        addAllLatencies(nodeInfo.getLatencies());
    }

    private void recordOtherFields(NodeInfo nodeInfo) {
        serviceRunning = nodeInfo.isServiceRunning();
        serviceHostAddress = nodeInfo.getServiceHostAddress();
    }

    public void addAllLatencies(Map<UUID, List<Long>> latencies) {
        for (Map.Entry<UUID, List<Long>> entry : latencies.entrySet()) {
            addLatency(entry.getKey(), entry.getValue());
        }
    }

    public synchronized void addLatency(UUID uuid, List<Long> latencies) {
        if (!mobileClientLatencies.containsKey(uuid)) {
            mobileClientLatencies.put(uuid, new ArrayList<>());
        }
        mobileClientLatencies.get(uuid).addAll(latencies);
    }

    public synchronized Map<UUID, List<Long>> getLatencies() {
        return mobileClientLatencies;
    }

    public double getMeanCPU() {
        return getMean(cpuLoad);
    }

    public double getMeanRam() {
        return getMean(ramLoad);
    }

    public double getMeanStorage() {
        return getMean(storage);
    }

    /**
     * Gets the address that this orchestrator uses to communicate with the {@code ServiceNode}
     *
     * @return the remote address of the WebSocket owned by this class.
     */
    public InetSocketAddress getAddress() {
        return webSocket.getRemoteSocketAddress();
    }

    public State getState() {
        return stateAtomRef.get();
    }

    /**
     * Sets the {@code ServiceNode}'s state to {@code state} and returns the previous value of state.
     */
    public void setState(State state) {
        stateAtomRef.set(state);
    }

    private double getMean(Collection<? extends Number> numbers) {
        return numbers.stream()
                .map(Number::doubleValue)
                .flatMapToDouble(DoubleStream::of)
                .average()
                .orElse(Double.MAX_VALUE)
                ;
    }

    public boolean isServiceRunning() {
        return serviceRunning;
    }

    public double getCpuScore() {
        // todo implement more Score methods
        //  todo reconsider the Mean methods
        throw new UnsupportedOperationException("ServiceNode.getCpuScore not implemented");
    }

    @Override
    public String toString() {
        return String.format("UUID=%s remoteSA=%s, serviceName=%s, serviceHostAddress=%s",
                uuid,
                webSocket.getRemoteSocketAddress(),
                serviceRunning,
                serviceHostAddress
        );
    }

    public enum State {
        STABLE, MIGRATING
    }
}
