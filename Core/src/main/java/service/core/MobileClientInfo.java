package service.core;

import org.java_websocket.WebSocket;

import java.net.InetAddress;
import java.util.UUID;

public class MobileClientInfo extends Message {
    private UUID uuid;
    private String desiredServiceName;
    private InetAddress pingServer;
    private WebSocket webSocket;


    public MobileClientInfo() {
        super(MessageTypes.MOBILE_CLIENT_INFO);
    }

    public MobileClientInfo(UUID uuid, String desiredServiceName, InetAddress pingServer) {
        this();
        this.uuid = uuid;
        this.desiredServiceName = desiredServiceName;
        this.pingServer = pingServer;
    }

    public MobileClientInfo(UUID uuid, String desiredServiceName) {
        this(uuid, desiredServiceName, null);
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getDesiredServiceName() {
        return desiredServiceName;
    }

    public void setDesiredServiceName(String desiredServiceName) {
        this.desiredServiceName = desiredServiceName;
    }

    public InetAddress getPingServer() {
        return pingServer;
    }

    public void setPingServer(InetAddress pingServer) {
        this.pingServer = pingServer;
    }

    public WebSocket getWebSocket() {
        return webSocket;
    }

    public void setWebSocket(WebSocket webSocket) {
        this.webSocket = webSocket;
    }

    @Override
    public String toString() {
        return String.format("%s: %s %s %s", getType(), getUuid(), getDesiredServiceName(), getPingServer());
    }
}
