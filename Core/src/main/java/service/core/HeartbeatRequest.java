package service.core;

public class HeartbeatRequest extends Message {
    public HeartbeatRequest() {
        super(MessageTypes.HEARTBEAT_REQUEST);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
