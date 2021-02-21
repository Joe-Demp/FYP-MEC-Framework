package service.core;

import java.net.URI;
import java.util.UUID;

public class NodeClientLatencyRequest extends Message {
    private UUID nodeId;
    private UUID clientId;
    private URI clientUri;

    public NodeClientLatencyRequest(UUID nodeId, UUID clientId, URI clientUri) {
        super(MessageTypes.NODE_CLIENT_LATENCY_REQUEST);
        this.nodeId = nodeId;
        this.clientId = clientId;
        this.clientUri = clientUri;
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
}
