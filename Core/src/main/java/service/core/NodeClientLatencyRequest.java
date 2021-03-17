package service.core;

import java.net.URI;
import java.util.UUID;

public class NodeClientLatencyRequest extends Message {
    private UUID nodeId;
    private UUID clientId;
    private URI clientUri;

    public NodeClientLatencyRequest() {
        super(MessageTypes.NODE_CLIENT_LATENCY_REQUEST);
    }

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

    /**
     * @return true if other is a {@code NodeClientLatencyRequest} with the same {@code nodeId} and {@code clientId}
     * as this.
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof NodeClientLatencyRequest) {
            NodeClientLatencyRequest nclRequest = (NodeClientLatencyRequest) other;

            return nodeId.equals(nclRequest.nodeId) && clientId.equals(nclRequest.clientId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return nodeId.hashCode() ^ clientId.hashCode();
    }
}
