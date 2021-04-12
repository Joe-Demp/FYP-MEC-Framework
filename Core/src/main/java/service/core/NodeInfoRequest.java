package service.core;

import java.util.UUID;

public class NodeInfoRequest extends Message {
    UUID uuid;

    public NodeInfoRequest() {
        super(MessageTypes.NODE_INFO_REQUEST);
    }

    public NodeInfoRequest(UUID uuid) {
        this();
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    @Override
    public String toString() {
        return "NodeInfoRequest{" +
                "uuid=" + uuid +
                '}';
    }
}
