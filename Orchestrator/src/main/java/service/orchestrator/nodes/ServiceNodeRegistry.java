package service.orchestrator.nodes;

import org.java_websocket.WebSocket;
import service.core.NodeInfo;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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

    private static void swapServiceNames(ServiceNode source, ServiceNode target) {
        target.serviceName = source.serviceName;
        source.serviceName = null;
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

    private static void setToStable(ServiceNode... nodes) {
        for (ServiceNode node : nodes) node.setState(ServiceNode.State.STABLE);
    }

    public Collection<ServiceNode> getHostingAndStableServiceNodes() {
        return serviceNodes.values().stream()
                .filter(ServiceNode::isHosting)
                .filter(node -> (node.getState() == ServiceNode.State.STABLE))
                .collect(Collectors.toList())
                ;
    }

    public void recordMigration(UUID source, UUID target) {
        ServiceNode sourceNode = get(source);
        ServiceNode targetNode = get(target);


        swapServiceNames(sourceNode, targetNode);
        setToStable(sourceNode, targetNode);
    }

    public void setToMigrating(ServiceNode... nodes) {
        for (ServiceNode node : nodes) node.setState(ServiceNode.State.MIGRATING);
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
