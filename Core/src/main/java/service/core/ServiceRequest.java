package service.core;

import java.util.UUID;

// todo maybe get rid of this if it's not getting used
//  e.g. the Orchestrator might use it to trigger a migration
//  Note -> if received by the orchestrator, ServiceRequest will not be caught in the switch.

// todo question the wisdom of sending the desiredServiceName all over the framework, when it's constant

public class ServiceRequest extends Message {
    private UUID targetNodeUuid;
    private String desiredServiceName;

    public ServiceRequest() {
        super(Message.MessageTypes.SERVICE_REQUEST);
    }

    public ServiceRequest(UUID targetNodeUuid, String desiredServiceName) {
        this();
        this.targetNodeUuid = targetNodeUuid;
        this.desiredServiceName = desiredServiceName;
    }

    public UUID getTargetNodeUuid() {
        return targetNodeUuid;
    }

    public String getDesiredServiceName() {
        return desiredServiceName;
    }

    @Override
    public String toString() {
        return String.format("ServiceRequest: requestorID=%s serviceName=%s", targetNodeUuid, desiredServiceName);
    }
}
