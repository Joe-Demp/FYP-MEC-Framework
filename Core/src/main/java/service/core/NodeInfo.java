package service.core;

import org.java_websocket.WebSocket;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NodeInfo extends Message {
    // todo remove unnecessary fields from here. Some are now held in ServiceNode

    private UUID uuid;
    private WebSocket webSocket;
    private String serviceName = "";
    private Map<Integer, Double> CPUload = Collections.emptyMap();
    private Map<Integer, Double> RamLoad = Collections.emptyMap();
    private Map<Integer, Long> unusedStorage = Collections.emptyMap();
    private Map<UUID, List<Long>> latencies = Collections.emptyMap();
    private boolean trustworthy = true;
    private URI serviceHostAddress;

    public NodeInfo() {
        super(Message.MessageTypes.NODE_INFO);
    }

    public NodeInfo(UUID uuid, String serviceName, URI serviceUri) {
        this();
        this.uuid = uuid;
        this.serviceName = serviceName;
        this.serviceHostAddress = serviceUri;
    }

    public Map<Integer, Long> getUnusedStorage() {
        return unusedStorage;
    }

    public void setUnusedStorage(Map<Integer, Long> unusedStorage) {
        this.unusedStorage = unusedStorage;
    }

    public Map<UUID, List<Long>> getLatencies() {
        return latencies;
    }

    public void setLatencies(Map<UUID, List<Long>> latencies) {
        this.latencies = latencies;
    }

    public WebSocket getWebSocket() {
        return webSocket;
    }

    public void setWebSocket(WebSocket webSocket) {
        this.webSocket = webSocket;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Map<Integer, Double> getCPUload() {
        return CPUload;
    }

    public void setCPUload(Map<Integer, Double> CPUload) {
        this.CPUload = CPUload;
    }

    public Map<Integer, Double> getRamLoad() {
        return RamLoad;
    }

    public void setRamLoad(Map<Integer, Double> ramLoad) {
        RamLoad = ramLoad;
    }

    public boolean isTrustworthy() {
        return trustworthy;
    }

    public void setTrustworthy(boolean trustworthy) {
        this.trustworthy = trustworthy;
    }

    public URI getServiceHostAddress() {
        return serviceHostAddress;
    }

    public void setServiceHostAddress(URI serviceHostAddress) {
        this.serviceHostAddress = serviceHostAddress;
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
