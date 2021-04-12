package service.core;

import java.util.UUID;

public class HostRequest extends Message {
    private UUID requestorID;

    public HostRequest() {
        super(Message.MessageTypes.HOST_REQUEST);
    }

    public HostRequest(UUID requestorID) {
        this();
        this.requestorID = requestorID;
    }

    public UUID getRequestorID() {
        return requestorID;
    }

    @Override
    public String toString() {
        return "HostRequest{" +
                "requestorID=" + requestorID +
                '}';
    }
}
