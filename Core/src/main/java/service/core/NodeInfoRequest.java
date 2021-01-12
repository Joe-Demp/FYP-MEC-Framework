package service.core;

import java.util.UUID;

public class NodeInfoRequest extends Message {
    UUID assignedUUID;

    // todo check if this is necessary? May be for serialization
    public NodeInfoRequest(){
        super(MessageTypes.NODE_INFO_REQUEST);
    }

    public NodeInfoRequest(UUID assignedUUID) {
        super(MessageTypes.NODE_INFO_REQUEST);
        this.assignedUUID=assignedUUID;
    }

    public UUID getAssignedUUID() {
        return assignedUUID;
    }
}
