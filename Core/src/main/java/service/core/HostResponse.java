package service.core;

import java.net.URI;
import java.util.UUID;

public class HostResponse extends Message  {
    private UUID requestorID;
    private URI serviceHostAddress;

    public HostResponse() {
        super(MessageTypes.HOST_RESPONSE);
    }

    public HostResponse(UUID requestorID, URI serviceHostAddress) {
        this();
        this.requestorID = requestorID;
        this.serviceHostAddress = serviceHostAddress;
    }

    @SuppressWarnings("unused")
    public URI getServiceHostAddress() {
        return serviceHostAddress;
    }

    @Override
    public String toString() {
        return "HostResponse{" +
                "requestorID=" + requestorID +
                ", serviceHostAddress=" + serviceHostAddress +
                '}';
    }
}
