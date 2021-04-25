package ie.ucd.mecframework.messages.service;

import service.core.Message;

import java.util.UUID;

public class StartServiceRequest extends Message {
    private UUID targetUuid;
    private String desiredServiceName;

    public StartServiceRequest() {
        super(MessageTypes.START_SERVICE_REQUEST);
    }

    public StartServiceRequest(UUID uuid, String desiredServiceName) {
        this();
        this.targetUuid = uuid;
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
        return "StartServiceRequest{" +
                "targetUuid=" + targetUuid +
                ", desiredServiceName='" + desiredServiceName + '\'' +
                '}';
    }
}
