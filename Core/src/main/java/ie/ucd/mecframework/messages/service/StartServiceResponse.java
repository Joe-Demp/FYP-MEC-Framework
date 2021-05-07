package ie.ucd.mecframework.messages.service;

import service.core.Message;

import java.util.UUID;

public class StartServiceResponse extends Message {
    private UUID uuid;
    private boolean serviceStarted;

    public StartServiceResponse() {
        super(MessageTypes.START_SERVICE_RESPONSE);
    }

    public StartServiceResponse(UUID uuid, boolean serviceStarted) {
        this();
        this.uuid = uuid;
        this.serviceStarted = serviceStarted;
    }

    @Override
    public String toString() {
        return "StartServiceResponse{" +
                "targetUuid=" + uuid +
                ", serviceStarted=" + serviceStarted +
                '}';
    }
}
