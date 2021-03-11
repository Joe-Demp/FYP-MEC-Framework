package service.orchestrator.nodes;

import service.core.NodeInfo;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ServiceNodeRegistry {
    private static final ServiceNodeRegistry instance = new ServiceNodeRegistry();
    private Map<UUID, ServiceNode> serviceNodes = new HashMap<>();

    private ServiceNodeRegistry() {
    }

    /**
     * Gets the Singleton instance of {@code ServiceNodeRegistry}.
     */
    public static ServiceNodeRegistry get() {
        return instance;
    }

    public void put(ServiceNode serviceNode) {
        serviceNodes.put(serviceNode.uuid, serviceNode);
    }

    public ServiceNode get(UUID uuid) {
        return serviceNodes.get(uuid);
    }

    public Collection<ServiceNode> getServiceNodes() {
        return serviceNodes.values();
    }

    /**
     * todo write
     *
     * @param nodeInfo
     */
    public void updateNode(NodeInfo nodeInfo) {
        getOrCreateServiceNode(nodeInfo).update(nodeInfo);
    }

    private ServiceNode getOrCreateServiceNode(NodeInfo nodeInfo) {
        UUID nodeUuid = nodeInfo.getUuid();

        if (serviceNodes.containsKey(nodeUuid)) {
            return serviceNodes.get(nodeUuid);
        } else {
            ServiceNode serviceNode = new ServiceNode(nodeUuid, nodeInfo.getWebSocket());
            serviceNodes.put(nodeUuid, serviceNode);
            return serviceNode;
        }
    }
}
