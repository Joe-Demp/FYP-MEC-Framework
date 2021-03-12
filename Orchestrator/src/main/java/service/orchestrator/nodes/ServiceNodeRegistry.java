package service.orchestrator.nodes;

import org.java_websocket.WebSocket;
import service.core.NodeInfo;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;

public class ServiceNodeRegistry {
    private static final ServiceNodeRegistry instance = new ServiceNodeRegistry();
    private Map<UUID, ServiceNode> serviceNodes = new Hashtable<>();

    private ServiceNodeRegistry() {
    }

    /**
     * Gets the Singleton instance of {@code ServiceNodeRegistry}.
     */
    public static ServiceNodeRegistry get() {
        return instance;
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

    public void removeNodeWithWebsocket(WebSocket webSocket) {
        ServiceNode toRemove = serviceNodeWithWebsocket(webSocket);
        serviceNodes.remove(toRemove.uuid);
    }

    // returns a dummy ServiceNode if it's not in the registry
    private ServiceNode serviceNodeWithWebsocket(WebSocket webSocket) {
        return serviceNodes.values().stream()
                .filter(node -> node.webSocket.equals(webSocket))
                .findFirst()
                .orElse(new ServiceNode(UUID.randomUUID(), webSocket));
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
