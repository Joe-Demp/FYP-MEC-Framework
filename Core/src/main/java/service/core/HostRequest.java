package service.core;

import java.util.UUID;

public class HostRequest extends Message  {
    private UUID requestorID;
    private String serviceName;

    public HostRequest() {
        super(Message.MessageTypes.HOST_REQUEST);
    }

    public HostRequest(UUID requestorID, String serviceName) {
        super(Message.MessageTypes.HOST_REQUEST);
        this.requestorID = requestorID;
        this.serviceName = serviceName;
    }

    public UUID getRequestorID() {
        return requestorID;
    }

    public String getRequestedServiceName() {
        return serviceName;
    }

    @Override
    public String toString() {
        return String.format("HostRequest: requestorId=%s serviceName=%s", requestorID, serviceName);
    }
}
