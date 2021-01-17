package service.core;

import java.util.UUID;

//this step is kinda acting like the owner of a service acknowleding the response and approving the transfer
public class ServiceResponse extends Message {
    private UUID requesterId;
    private UUID serviceOwnerID;
    private String serviceOwnerAddress;
    private String serviceName;

    // todo something about the comment below
    //idk if this is how i wanna do it but maybe

    public ServiceResponse() {
        super(Message.MessageTypes.SERVICE_RESPONSE);
    }

    public ServiceResponse(UUID requesterId, UUID serviceOwnerID, String serviceOwnerAddress, String serviceName) {
        this();
        this.requesterId = requesterId;
        this.serviceOwnerID = serviceOwnerID;
        this.serviceOwnerAddress = serviceOwnerAddress;
        this.serviceName = serviceName;
    }

    public UUID getRequesterId() {
        return requesterId;
    }

    public UUID getServiceOwnerID() {
        return serviceOwnerID;
    }

    public String getServiceOwnerAddress() {
        return serviceOwnerAddress;
    }

    public String getServiceName() {
        return serviceName;
    }
}
