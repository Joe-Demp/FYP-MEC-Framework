package service.core;

import java.util.UUID;

public class MigrationSuccess extends Message {
    UUID targetHostUuid;
    UUID sourceHostUuid;
    String serviceName;

    public MigrationSuccess() {
        super(MessageTypes.MIGRATION_SUCCESS);
    }

    public MigrationSuccess(UUID targetHostUuid, UUID sourceHostUuid, String serviceName) {
        this();
        this.targetHostUuid = targetHostUuid;
        this.serviceName = serviceName;
        this.sourceHostUuid = sourceHostUuid;
    }

    public UUID getTargetHostUuid() {
        return targetHostUuid;
    }

    public String getServiceName() {
        return serviceName;
    }

    public UUID getSourceHostUuid() {
        return sourceHostUuid;
    }

    @Override
    public String toString() {
        return "MigrationSuccess{" +
                "targetHostUuid=" + targetHostUuid +
                ", sourceHostUuid=" + sourceHostUuid +
                ", serviceName='" + serviceName + '\'' +
                '}';
    }
}


