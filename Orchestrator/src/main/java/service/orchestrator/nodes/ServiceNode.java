package service.orchestrator.nodes;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.core.NodeInfo;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.DoubleStream;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * A class to represent Service Nodes as they are visible to the Orchestrator.
 */
public class ServiceNode {
    private static final Logger logger = LoggerFactory.getLogger(ServiceNode.class);

    public UUID uuid;
    public WebSocket webSocket;
    public boolean serviceRunning;
    public List<Double> cpuLoad = new ArrayList<>();
    public List<Double> ramLoad = new ArrayList<>();
    public List<Long> storage = new ArrayList<>();
    public List<Long> mainMemory = new ArrayList<>();
    public URI serviceHostAddress;
    public InetAddress globalIpAddress;
    private Map<UUID, List<Long>> mobileClientLatencies = new Hashtable<>();
    private AtomicReference<State> stateAtomRef = new AtomicReference<>(State.STABLE);

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
        updateGlobalIp(nodeInfo.getGlobalIpAddress());
    }

    private void updateGlobalIp(InetAddress address) {
        globalIpAddress = isNull(address) ? globalIpAddress : address;
    }

    public void addAllLatencies(Map<UUID, List<Long>> latencies) {
        for (Map.Entry<UUID, List<Long>> entry : latencies.entrySet()) {
            addLatencies(entry.getKey(), entry.getValue());
        }
    }

    public synchronized void addLatencies(UUID uuid, List<Long> latencies) {
        if (!mobileClientLatencies.containsKey(uuid)) {
            mobileClientLatencies.put(uuid, new ArrayList<>());
        }
        mobileClientLatencies.get(uuid).addAll(latencies);
    }

    public synchronized Set<Map.Entry<UUID, List<Long>>> latencyEntries() {
        return mobileClientLatencies.entrySet();
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

    /**
     * @return the mean of the past 10 cpu utilization scores.
     */
    public double getCpuScore() {
        int upperBound = cpuLoad.size();
        int lowerBound = Integer.max(upperBound - 10, 0);
        List<Double> cpuUtilizations = cpuLoad.subList(lowerBound, upperBound);
        return getMean(cpuUtilizations);
    }

    public double getMeanLatency(UUID clientUuid) {
        List<Long> latencies = mobileClientLatencies.get(clientUuid);
        if (isNull(latencies)) return Double.MAX_VALUE;

        int cutoff = latencies.size();
        List<Long> snapshot = latencies.subList(0, cutoff);
        return getMean(snapshot);
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

    public URI getServiceAddressUri() {
        return URI.create("http://" + globalIpAddress.getHostAddress() + ":" + serviceHostAddress.getPort());
    }

    public enum State {
        STABLE, MIGRATING
    }
}
