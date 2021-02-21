package service.core;

import java.net.URI;
import java.util.UUID;

public class NodeClientLatencyResponse extends Message {
    public UUID nodeId;
    public UUID clientId;
    public URI clientUri;
    public long latency;

    public NodeClientLatencyResponse(UUID nodeId, UUID clientId, URI clientUri, long latency) {
        super(MessageTypes.NODE_CLIENT_LATENCY_RESPONSE);
        this.nodeId = nodeId;
        this.clientId = clientId;
        this.clientUri = clientUri;
        this.latency = latency;
    }
}
