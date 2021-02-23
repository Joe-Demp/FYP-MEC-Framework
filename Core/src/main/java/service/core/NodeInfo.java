package service.core;

import org.java_websocket.WebSocket;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

public class NodeInfo extends Message {
    private UUID uuid;
    private WebSocket webSocket;
    private String serviceName;
    private Map<Integer, Double> CPUload;
    private Map<Integer, Double> RamLoad;
    private Map<Integer, Long> unusedStorage;
    private double rollingCPUScore;
    private double rollingRamScore;
    private boolean trustyworthy = true;
    private URI serviceHostAddress;

    public NodeInfo() {
        super(Message.MessageTypes.NODE_INFO);
    }

    // todo remove CPUload from here
    public NodeInfo(UUID uuid, Map<Integer, Double> CPUload, String serviceName) {
        super(Message.MessageTypes.NODE_INFO);
        this.uuid = uuid;
        this.CPUload = CPUload;
        this.serviceName = serviceName;
    }

    public Map<Integer, Long> getUnusedStorage() {
        return unusedStorage;
    }

    public void setUnusedStorage(Map<Integer, Long> unusedStorage) {
        this.unusedStorage = unusedStorage;
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

    public boolean isTrustyworthy() {
        return trustyworthy;
    }

    public void setTrustyworthy(boolean trustyworthy) {
        this.trustyworthy = trustyworthy;
    }

    public double getRollingCPUScore() {
        return rollingCPUScore;
    }

    public void setRollingCPUScore(double rollingCPUScore) {
        this.rollingCPUScore = rollingCPUScore;
    }

    public double getRollingRamScore() {
        return rollingRamScore;
    }

    public void setRollingRamScore(double rollingRamScore) {
        this.rollingRamScore = rollingRamScore;
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
