package service.orchestrator.nodes;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.core.NodeInfo;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.*;
import java.util.stream.DoubleStream;

import static java.util.Objects.nonNull;

/**
 * A class to represent Service Nodes as they are visible to the Orchestrator.
 */
public class ServiceNode {
    private static final Logger logger = LoggerFactory.getLogger(ServiceNode.class);

    public UUID uuid;
    public WebSocket webSocket;
    /**
     * The name of the service running on the node.
     * <p>
     * Todo check if this is necessary? This could be defined by the Orchestrator properties
     * and used to make sure clients are in the right place
     */
    public String serviceName;
    public Deque<Double> CPUload = new ArrayDeque<>();
    public Deque<Double> RamLoad = new ArrayDeque<>();
    public Deque<Long> unusedStorage = new ArrayDeque<>();
    public Map<UUID, List<Long>> mobileClientLatencies = new HashMap<>();

    // todo these scores need to be born out of methods. Remove the fields if not necessary
        /*
        private double rollingCPUScore;
        private double rollingRamScore;
        */

    public boolean trustyworthy = true;
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

        // todo extract
        WebSocket otherWebSocket = nodeInfo.getWebSocket();
        webSocket = nonNull(otherWebSocket) ? otherWebSocket : webSocket;

        // todo remove the maps from here: NodeInfo should only send the actual scores
        SortedMap<Integer, Double> cpuMap = new TreeMap<>(nodeInfo.getCPUload());
        SortedMap<Integer, Double> ramMap = new TreeMap<>(nodeInfo.getRamLoad());
        SortedMap<Integer, Long> unusedStorageMap = new TreeMap<>(nodeInfo.getUnusedStorage());

        CPUload.addAll(cpuMap.values());
        RamLoad.addAll(ramMap.values());
        unusedStorage.addAll(unusedStorageMap.values());

        // todo maybe extract these to different methods
        // update the semi static fields
        serviceName = nodeInfo.getServiceName();
        serviceHostAddress = nodeInfo.getServiceHostAddress();
    }

    public void addLatency(UUID uuid, List<Long> latencies) {
        if (!mobileClientLatencies.containsKey(uuid)) {
            mobileClientLatencies.put(uuid, new ArrayList<>());
        }
        mobileClientLatencies.get(uuid).addAll(latencies);
    }

    public double getMeanCPU() {
        return getMean(CPUload);
    }

    public double getMeanRam() {
        return getMean(RamLoad);
    }

    public double getMeanStorage() {
        return getMean(unusedStorage);
    }

    /**
     * Gets the address that this orchestrator uses to communicate with the {@code ServiceNode}
     *
     * @return the remote address of the WebSocket owned by this class.
     */
    public InetSocketAddress getAddress() {
        return webSocket.getRemoteSocketAddress();
    }

    private double getMean(Collection<? extends Number> numbers) {
        return numbers.stream()
                .map(Number::doubleValue)
                .flatMapToDouble(DoubleStream::of)
                .average()
                .orElse(Double.MAX_VALUE)
                ;
    }

    @Override
    public String toString() {
        return String.format("UUID=%s remoteSA=%s, serviceName=%s, serviceHostAddress=%s",
                uuid,
                webSocket.getRemoteSocketAddress(),
                serviceName,
                serviceHostAddress
        );
    }
}
