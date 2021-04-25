package ie.ucd.mecframework.messages.service;

import service.core.Message;

import java.util.UUID;

public class StartServiceRequest extends Message {
    private UUID uuid;

    public StartServiceRequest() {
        super(MessageTypes.START_SERVICE_REQUEST);
    }

    public StartServiceRequest(UUID uuid) {
        this();
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    @Override
    public String toString() {
        return "StartServiceRequest{" +
                "uuid=" + uuid +
                '}';
    }
}
