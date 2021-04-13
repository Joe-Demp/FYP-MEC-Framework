package service.core;

import java.net.InetAddress;

public class MigrationAlert extends Message {
    private InetAddress sourceIp;
    private InetAddress targetIp;

    private MigrationAlert() {
        super(MessageTypes.MIGRATION_ALERT);
    }

    public MigrationAlert(InetAddress sourceIp, InetAddress targetIp) {
        this();
        this.sourceIp = sourceIp;
        this.targetIp = targetIp;
    }

    @Override
    public String toString() {
        return "MigrationAlert{" +
                "sourceIp=" + sourceIp +
                ", targetIp=" + targetIp +
                '}';
    }
}
