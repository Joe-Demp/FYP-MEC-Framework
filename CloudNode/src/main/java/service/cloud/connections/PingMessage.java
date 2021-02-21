package service.cloud.connections;

import java.io.Serializable;

public class PingMessage implements Serializable {
    public static final int LENGTH = 1000;
    public static final byte[] junkMessage = new byte[LENGTH];

    private final byte[] data = junkMessage;

    public byte[] getData() {
        return data;
    }
}
