package service.core;

import java.net.InetSocketAddress;
import java.util.UUID;

/**
 * Message sent by a migration Source ServiceNode to the Orchestrator to relay to the Target ServiceNode.
 * <p>
 * Shows that the Source approves the transfer.
 */
public class ServiceResponse extends Message {
    private UUID targetNodeUuid;
    private UUID sourceNodeUuid;

    public void setTransferServerAddress(InetSocketAddress transferServerAddress) {
        this.transferServerAddress = transferServerAddress;
    }

    /**
     * The address of the transfer server provided by the source ServiceNode during migration.
     */
    private InetSocketAddress transferServerAddress;
    private String serviceName;

    public ServiceResponse() {
        super(Message.MessageTypes.SERVICE_RESPONSE);
    }

    public ServiceResponse(UUID targetNodeUuid, UUID sourceNodeUuid, InetSocketAddress transferServerAddress,
                           String serviceName) {
        this();
        this.targetNodeUuid = targetNodeUuid;
        this.sourceNodeUuid = sourceNodeUuid;
        this.transferServerAddress = transferServerAddress;
        this.serviceName = serviceName;
    }

    public UUID getTargetNodeUuid() {
        return targetNodeUuid;
    }

    public UUID getSourceNodeUuid() {
        return sourceNodeUuid;
    }

    public InetSocketAddress getTransferServerAddress() {
        return transferServerAddress;
    }

    public String getServiceName() {
        return serviceName;
    }
}
