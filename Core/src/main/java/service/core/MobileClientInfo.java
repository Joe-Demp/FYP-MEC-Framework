package service.core;

import org.java_websocket.WebSocket;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.UUID;

public class MobileClientInfo extends Message {
    private UUID uuid;
    private String desiredServiceName;
    private InetSocketAddress pingServer;
    private WebSocket webSocket;

    public MobileClientInfo() {
        super(MessageTypes.MOBILE_CLIENT_INFO);
    }

    public MobileClientInfo(UUID uuid, String desiredServiceName, InetSocketAddress pingServer) {
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

    public InetSocketAddress getPingServer() {
        return pingServer;
    }

    public void setPingServer(InetSocketAddress pingServer) {
        this.pingServer = pingServer;
    }

    public void setPingServerAddress(InetAddress address) {
        this.pingServer = new InetSocketAddress(address, Constants.MOBILE_PING_SERVER_PORT);
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
