package service.core;

import java.util.UUID;

// todo maybe get rid of this if it's not getting used
//  e.g. the Orchestrator might use it to trigger a migration
//  Note -> if received by the orchestrator, ServiceRequest will not be caught in the switch.
public class ServiceRequest extends Message {
    private UUID requesterId;
    private String desiredServiceName;

    public ServiceRequest() {
        super(Message.MessageTypes.SERVICE_REQUEST);
    }

    public ServiceRequest(UUID requesterId, String desiredServiceName) {
        super(Message.MessageTypes.SERVICE_REQUEST);
        this.requesterId = requesterId;
        this.desiredServiceName = desiredServiceName;
    }

    public UUID getRequesterId() {
        return requesterId;
    }

    public String getDesiredServiceName() {
        return desiredServiceName;
    }

    @Override
    public String toString() {
        return String.format("ServiceRequest: requestorID=%s serviceName=%s", requesterId, desiredServiceName);
    }
}
