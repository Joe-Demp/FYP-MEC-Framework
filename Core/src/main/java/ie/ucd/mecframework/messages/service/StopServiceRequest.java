package ie.ucd.mecframework.messages.service;

import service.core.Message;

import java.util.UUID;

public class StopServiceRequest extends Message {
    private UUID uuid;

    public StopServiceRequest() {
        super(MessageTypes.STOP_SERVICE_REQUEST);
    }

    public StopServiceRequest(UUID uuid) {
        this();
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    @Override
    public String toString() {
        return "StopServiceRequest{" +
                "uuid=" + uuid +
                '}';
    }
}
