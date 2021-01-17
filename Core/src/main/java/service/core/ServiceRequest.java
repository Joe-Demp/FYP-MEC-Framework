package service.core;

import java.util.UUID;

public class ServiceRequest extends Message{
    private UUID requesterId;
    private String serviceName;

    public ServiceRequest() {
        super(Message.MessageTypes.SERVICE_REQUEST);
    }

    public ServiceRequest(UUID requesterId, String serviceName) {
        super(Message.MessageTypes.SERVICE_REQUEST);
        this.requesterId = requesterId;
        this.serviceName = serviceName;
    }

    public UUID getRequesterId() {
        return requesterId;
    }

    public String getServiceName() {
        return serviceName;
    }

    @Override
    public String toString() {
        return String.format("ServiceRequest: requestorID=%s serviceName=%s", requesterId, serviceName);
    }
}
