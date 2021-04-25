package ie.ucd.mecframework.messages.service;

import service.core.Message;

import java.util.UUID;

public class StopServiceResponse extends Message {
    private UUID targetUuid;
    private String desiredServiceName;
    private boolean serviceStopped;

    public StopServiceResponse() {
        super(MessageTypes.STOP_SERVICE_RESPONSE);
    }

    public StopServiceResponse(UUID targetUuid, String desiredServiceName, boolean serviceStopped) {
        this();
        this.targetUuid = targetUuid;
        this.desiredServiceName = desiredServiceName;
        this.serviceStopped = serviceStopped;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getDesiredServiceName() {
        return desiredServiceName;
    }

    public boolean isServiceStopped() {
        return serviceStopped;
    }

    @Override
    public String toString() {
        return "StopServiceResponse{" +
                "targetUuid=" + targetUuid +
                ", desiredServiceName='" + desiredServiceName + '\'' +
                ", serviceStopped=" + serviceStopped +
                '}';
    }
}
