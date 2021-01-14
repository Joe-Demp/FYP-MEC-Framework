package service.core;

import java.util.UUID;

public class ServiceRequest extends Message{
    private UUID requestorID;
    private String serviceName;

    public ServiceRequest(){
        super(Message.MessageTypes.SERVICE_REQUEST);
    }
    public ServiceRequest(UUID requestorID, String serviceName) {
        super(Message.MessageTypes.SERVICE_REQUEST);
        this.requestorID = requestorID;
        this.serviceName=serviceName;
    }

    public UUID getRequestorID() {
        return requestorID;
    }

    public String getServiceName() {
        return serviceName;
    }

    @Override
    public String toString() {
        return String.format("ServiceRequest: requestorID=%s serviceName=%s", requestorID, serviceName);
    }
}
