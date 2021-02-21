package service.core;

import java.net.URI;
import java.util.UUID;

public class NodeClientLatencyRequest extends Message {
    public UUID nodeId;
    public UUID clientId;
    public URI clientUri;

    public NodeClientLatencyRequest(UUID nodeId, UUID clientId, URI clientUri) {
        super(MessageTypes.NODE_CLIENT_LATENCY_REQUEST);
        this.nodeId = nodeId;
        this.clientId = clientId;
        this.clientUri = clientUri;
    }
}
