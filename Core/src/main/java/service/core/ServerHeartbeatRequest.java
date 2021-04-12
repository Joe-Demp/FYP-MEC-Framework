package service.core;

public class ServerHeartbeatRequest extends Message {
    public ServerHeartbeatRequest() {
        super(MessageTypes.SERVER_HEARTBEAT_REQUEST);
    }

    @Override
    public String toString() {
        return String.format("type=%s", getType());
    }
}
