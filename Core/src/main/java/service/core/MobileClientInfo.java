package service.core;

import org.java_websocket.WebSocket;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.UUID;

public class MobileClientInfo extends Message {
    private UUID uuid;
    private InetSocketAddress pingServer;
    private WebSocket webSocket;

    public MobileClientInfo() {
        super(MessageTypes.MOBILE_CLIENT_INFO);
    }

    @SuppressWarnings("unused")
    public MobileClientInfo(UUID uuid, InetSocketAddress pingServer) {
        this();
        this.uuid = uuid;
        this.pingServer = pingServer;
    }

    public UUID getUuid() {
        return uuid;
    }

    public InetSocketAddress getPingServer() {
        return pingServer;
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
        return "MobileClientInfo{" +
                "uuid=" + uuid +
                ", pingServer=" + pingServer +
                ", webSocket=" + webSocket +
                '}';
    }
}
