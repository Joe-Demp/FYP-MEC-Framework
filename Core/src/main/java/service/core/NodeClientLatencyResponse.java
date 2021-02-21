package service.core;

import java.net.URI;
import java.util.UUID;

public class NodeClientLatencyResponse extends Message {
    private UUID nodeId;
    private UUID clientId;
    private URI clientUri;
    private long latency;

    public NodeClientLatencyResponse(UUID nodeId, UUID clientId, URI clientUri, long latency) {
        super(MessageTypes.NODE_CLIENT_LATENCY_RESPONSE);
        this.nodeId = nodeId;
        this.clientId = clientId;
        this.clientUri = clientUri;
        this.latency = latency;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public UUID getClientId() {
        return clientId;
    }

    public void setClientId(UUID clientId) {
        this.clientId = clientId;
    }

    public URI getClientUri() {
        return clientUri;
    }

    public void setClientUri(URI clientUri) {
        this.clientUri = clientUri;
    }

    public long getLatency() {
        return latency;
    }

    public void setLatency(long latency) {
        this.latency = latency;
    }
}
