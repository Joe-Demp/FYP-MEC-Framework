package service.orchestrator.clients;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.core.MobileClientInfo;

import java.net.InetAddress;
import java.util.UUID;

import static java.util.Objects.nonNull;

// todo add NotNull annotations to the project (much later)

public class MobileClient {
    private static final Logger logger = LoggerFactory.getLogger(MobileClient.class);

    public UUID uuid;
    public String desiredServiceName;
    public InetAddress pingServer;
    public WebSocket webSocket;

    public MobileClient(UUID uuid, String desiredServiceName, InetAddress pingServer, WebSocket webSocket) {
        this.uuid = uuid;
        this.desiredServiceName = desiredServiceName;
        this.pingServer = pingServer;
        this.webSocket = webSocket;
    }


    public void update(MobileClientInfo mobileClientInfo) {
        // take the new values from mobileClientInfo and add them to the ServiceNode's fields
        if (!uuid.equals(mobileClientInfo.getUuid())) {
            logger.warn("Tried to update ServiceNode {} with NodeInfo {}", uuid, mobileClientInfo.getUuid());
            return;
        }

        setWebSocket(mobileClientInfo.getWebSocket());
        setPingServer(mobileClientInfo.getPingServer());
    }

    private void setWebSocket(WebSocket webSocket) {
        if (nonNull(webSocket)) {
            this.webSocket = webSocket;
        }
    }

    private void setPingServer(InetAddress pingServer) {
        if (nonNull(pingServer)) {
            this.pingServer = pingServer;
        }
    }

    @Override
    public String toString() {
        return String.format("MobileClient: %s %s %s", uuid, desiredServiceName, pingServer);
    }

}
