package ie.ucd.mecframework.messages.service;

import service.core.Message;

import java.util.UUID;

public class StartServiceResponse extends Message {
    private UUID targetUuid;
    private String desiredServiceName;
    private boolean serviceStarted;

    public StartServiceResponse() {
        super(MessageTypes.START_SERVICE_RESPONSE);
    }

    public StartServiceResponse(UUID uuid, String desiredServiceName, boolean serviceStarted) {
        this();
        this.targetUuid = uuid;
        this.desiredServiceName = desiredServiceName;
        this.serviceStarted = serviceStarted;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getDesiredServiceName() {
        return desiredServiceName;
    }

    public boolean isServiceStarted() {
        return serviceStarted;
    }

    @Override
    public String toString() {
        return "StartServiceResponse{" +
                "targetUuid=" + targetUuid +
                ", desiredServiceName='" + desiredServiceName + '\'' +
                ", serviceStarted=" + serviceStarted +
                '}';
    }
}
