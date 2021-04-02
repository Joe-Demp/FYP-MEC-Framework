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

    public UUID getRequestorID() {
        return requestorID;
    }

    public URI getServiceHostAddress() {
        return serviceHostAddress;
    }

    @Override
    public String toString() {
        return String.format("HostResponse: requestorId=%s serviceHostAddress=%s", requestorID, serviceHostAddress);
    }
}
