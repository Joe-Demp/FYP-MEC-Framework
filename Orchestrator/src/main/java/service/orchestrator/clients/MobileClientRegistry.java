package service.orchestrator.clients;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.core.MobileClientInfo;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MobileClientRegistry {
    private static final Logger logger = LoggerFactory.getLogger(MobileClientRegistry.class);
    private static final MobileClientRegistry instance = new MobileClientRegistry();
    private Map<UUID, MobileClient> mobileClients = new HashMap<>();

    private MobileClientRegistry() {
    }

    public static MobileClientRegistry get() {
        return instance;
    }

    public void put(MobileClient mobileClient) {
        mobileClients.put(mobileClient.uuid, mobileClient);
    }

    public MobileClient get(UUID uuid) {
        return mobileClients.get(uuid);
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

    private MobileClient getOrCreateMobileClient(MobileClientInfo mobileClientInfo) {
        UUID nodeUuid = mobileClientInfo.getUuid();

        if (mobileClients.containsKey(nodeUuid)) {
            return mobileClients.get(nodeUuid);
        } else {
            MobileClient mobileClient = new MobileClient(
                    nodeUuid, mobileClientInfo.getDesiredServiceName(), mobileClientInfo.getPingServer(),
                    mobileClientInfo.getWebSocket());
            mobileClients.put(nodeUuid, mobileClient);

            logger.info("Adding to MobileClientRegistry {}", mobileClient.toString());
            return mobileClient;
        }
    }
}
