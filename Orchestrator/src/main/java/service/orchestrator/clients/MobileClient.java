package service.orchestrator.clients;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.core.MobileClientInfo;

import java.net.InetSocketAddress;
import java.util.UUID;

import static java.util.Objects.nonNull;

// todo add NotNull annotations to the project (much later)

public class MobileClient {
    private static final Logger logger = LoggerFactory.getLogger(MobileClient.class);

    public UUID uuid;
    /**
     * Always contains the address of the PingServer from the point of view of the Orchestrator.
     */
    public InetSocketAddress pingServer;
    public WebSocket webSocket;

    public MobileClient(UUID uuid, InetSocketAddress pingServer, WebSocket webSocket) {
        this.uuid = uuid;
        this.pingServer = pingServer;
        this.webSocket = webSocket;
    }

    private static boolean isWildcardInetAddress(InetSocketAddress address) {
        return address.getHostString().equals("0.0.0.0");
    }

    public void update(MobileClientInfo mobileClientInfo) {
        // take the new values from mobileClientInfo and add them to the ServiceNode's fields
        if (!uuid.equals(mobileClientInfo.getUuid())) {
            logger.warn("Tried to update ServiceNode {} with NodeInfo {}", uuid, mobileClientInfo.getUuid());
            return;
        }

        setWebSocket(mobileClientInfo.getWebSocket());
        updatePingServer(mobileClientInfo.getPingServer());
    }

    private void updatePingServer(InetSocketAddress update) {
        if (!isWildcardInetAddress(update)) {
            setPingServer(update);
        } else {
            String oldHostName = pingServer.getHostName();
            int newPort = update.getPort();
            setPingServer(new InetSocketAddress(oldHostName, newPort));
        }
    }

    private void setWebSocket(WebSocket webSocket) {
        if (nonNull(webSocket)) {
            this.webSocket = webSocket;
        }
    }

    private void setPingServer(InetSocketAddress pingServer) {
        if (nonNull(pingServer)) {
            this.pingServer = pingServer;
        }
    }

    @Override
    public String toString() {
        return String.format("MobileClient: %s ping=%s", uuid, pingServer);
    }
}
