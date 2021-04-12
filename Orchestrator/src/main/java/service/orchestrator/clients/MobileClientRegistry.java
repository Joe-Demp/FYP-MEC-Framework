package service.orchestrator.clients;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.core.MobileClientInfo;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.isNull;

public class MobileClientRegistry {
    private static final Logger logger = LoggerFactory.getLogger(MobileClientRegistry.class);
    private static final MobileClientRegistry instance = new MobileClientRegistry();
    private Map<UUID, MobileClient> mobileClients = new Hashtable<>();

    private MobileClientRegistry() {
    }

    public static MobileClientRegistry get() {
        return instance;
    }

    public void put(MobileClient mobileClient) {
        mobileClients.put(mobileClient.uuid, mobileClient);
    }

    public MobileClient get(UUID uuid) {
        return isNull(uuid) ? null : mobileClients.get(uuid);
    }

    public Collection<MobileClient> getMobileClients() {
        return mobileClients.values();
    }

    /**
     * todo write
     *
     * @param mobileClientInfo
     */
    public void updateClient(MobileClientInfo mobileClientInfo) {
        getOrCreateMobileClient(mobileClientInfo).update(mobileClientInfo);
    }

    public void removeClientWithWebsocket(WebSocket webSocket) {
        MobileClient toRemove = mobileClientWithWebsocket(webSocket);
        mobileClients.remove(toRemove.uuid);
    }

    // returns a dummy MobileClient if it's not in the registry
    private MobileClient mobileClientWithWebsocket(WebSocket webSocket) {
        return mobileClients.values().stream()
                .filter(client -> client.webSocket.equals(webSocket))
                .findFirst()
                .orElse(new MobileClient(UUID.randomUUID(), null, webSocket));
    }

    private MobileClient getOrCreateMobileClient(MobileClientInfo mobileClientInfo) {
        UUID nodeUuid = mobileClientInfo.getUuid();

        if (mobileClients.containsKey(nodeUuid)) {
            return mobileClients.get(nodeUuid);
        } else {
            MobileClient mobileClient = new MobileClient(
                    nodeUuid, mobileClientInfo.getPingServer(),
                    mobileClientInfo.getWebSocket());
            mobileClients.put(nodeUuid, mobileClient);

            logger.info("Adding to MobileClientRegistry {}", mobileClient.toString());
            return mobileClient;
        }
    }
}
