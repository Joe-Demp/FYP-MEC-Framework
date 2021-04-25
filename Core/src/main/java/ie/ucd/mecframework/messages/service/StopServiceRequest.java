package ie.ucd.mecframework.messages.service;

import service.core.Message;

import java.util.UUID;

public class StopServiceRequest extends Message {
    private UUID targetUuid;
    private String desiredServiceName;

    public StopServiceRequest() {
        super(MessageTypes.STOP_SERVICE_REQUEST);
    }

    public StopServiceRequest(UUID targetUuid, String desiredServiceName) {
        this();
        this.targetUuid = targetUuid;
        this.desiredServiceName = desiredServiceName;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getDesiredServiceName() {
        return desiredServiceName;
    }

    @Override
    public String toString() {
        return "StopServiceRequest{" +
                "targetUuid=" + targetUuid +
                ", desiredServiceName='" + desiredServiceName + '\'' +
                '}';
    }
}
