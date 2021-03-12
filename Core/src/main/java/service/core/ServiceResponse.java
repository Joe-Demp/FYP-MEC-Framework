package service.core;

import java.util.UUID;

/**
 * Message sent by a migration Source ServiceNode to the Orchestrator to relay to the Target ServiceNode.
 * <p>
 * Shows that the Source approves the transfer.
 */
public class ServiceResponse extends Message {
    private UUID targetNodeUuid;
    private UUID sourceNodeUuid;
    private String sourceServiceAddress;
    private String serviceName;

    // todo something about the comment below
    //idk if this is how i wanna do it but maybe

    public ServiceResponse() {
        super(Message.MessageTypes.SERVICE_RESPONSE);
    }

    public ServiceResponse(UUID targetNodeUuid, UUID sourceNodeUuid, String sourceServiceAddress, String serviceName) {
        this();
        this.targetNodeUuid = targetNodeUuid;
        this.sourceNodeUuid = sourceNodeUuid;
        this.sourceServiceAddress = sourceServiceAddress;
        this.serviceName = serviceName;
    }

    public UUID getTargetNodeUuid() {
        return targetNodeUuid;
    }

    public UUID getSourceNodeUuid() {
        return sourceNodeUuid;
    }

    public String getSourceServiceAddress() {
        return sourceServiceAddress;
    }

    public String getServiceName() {
        return serviceName;
    }
}
