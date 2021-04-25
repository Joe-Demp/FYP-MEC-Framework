package ie.ucd.mecframework.messages.service;

import service.core.Message;

import java.util.UUID;

public class StopServiceResponse extends Message {
    private UUID uuid;
    private boolean serviceStopped;

    public StopServiceResponse() {
        super(MessageTypes.STOP_SERVICE_RESPONSE);
    }

    public StopServiceResponse(UUID uuid, boolean serviceStopped) {
        this();
        this.uuid = uuid;
        this.serviceStopped = serviceStopped;
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isServiceStopped() {
        return serviceStopped;
    }
}
