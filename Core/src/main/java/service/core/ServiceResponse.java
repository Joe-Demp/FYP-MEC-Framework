package service.core;

import java.net.InetSocketAddress;
import java.util.UUID;

/**
 * Message sent by a migration Source ServiceNode to the Orchestrator to relay to the Target ServiceNode.
 * <p>
 * Shows that the Source approves the transfer.
 */
public class ServiceResponse extends Message {
    private UUID targetUuid;
    private UUID sourceUuid;

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

    public ServiceResponse(UUID targetUuid, UUID sourceUuid, InetSocketAddress transferServerAddress,
                           String serviceName) {
        this();
        this.targetUuid = targetUuid;
        this.sourceUuid = sourceUuid;
        this.transferServerAddress = transferServerAddress;
        this.serviceName = serviceName;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public UUID getSourceUuid() {
        return sourceUuid;
    }

    public InetSocketAddress getTransferServerAddress() {
        return transferServerAddress;
    }

    public String getServiceName() {
        return serviceName;
    }

    @Override
    public String toString() {
        return "ServiceResponse{" +
                "targetUuid=" + targetUuid +
                ", sourceUuid=" + sourceUuid +
                ", transferServerAddress=" + transferServerAddress +
                ", serviceName='" + serviceName + '\'' +
                '}';
    }
}
