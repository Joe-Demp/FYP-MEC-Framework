package service.core;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.UUID;

/**
 * Message sent by a migration Source ServiceNode to the Orchestrator to relay to the Target ServiceNode.
 * <p>
 * Shows that the Source approves the transfer.
 */
public class ServiceResponse extends Message {
    private UUID targetUuid;
    private UUID sourceUuid;
    /**
     * The address of the transfer server provided by the source ServiceNode during migration.
     */
    private List<InetSocketAddress> transferServerAddresses;
    private String serviceName;
    public ServiceResponse() {
        super(Message.MessageTypes.SERVICE_RESPONSE);
    }

    public ServiceResponse(UUID targetUuid, UUID sourceUuid, List<InetSocketAddress> transferServerAddresses,
                           String serviceName) {
        this();
        this.targetUuid = targetUuid;
        this.sourceUuid = sourceUuid;
        this.transferServerAddresses = transferServerAddresses;
        this.serviceName = serviceName;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public UUID getSourceUuid() {
        return sourceUuid;
    }

    public List<InetSocketAddress> getTransferServerAddresses() {
        return transferServerAddresses;
    }

    public void setTransferServerAddresses(List<InetSocketAddress> transferServerAddresses) {
        this.transferServerAddresses = transferServerAddresses;
    }

    public String getServiceName() {
        return serviceName;
    }

    @Override
    public String toString() {
        return "ServiceResponse{" +
                "targetUuid=" + targetUuid +
                ", sourceUuid=" + sourceUuid +
                ", transferServerAddresses=" + transferServerAddresses +
                ", serviceName='" + serviceName + '\'' +
                '}';
    }
}
