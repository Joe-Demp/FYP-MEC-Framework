package service.orchestrator.migration;

import service.orchestrator.clients.MobileClient;
import service.orchestrator.nodes.ServiceNode;

import java.util.Collection;

/**
 * A type for objects that choose optimal target nodes for migration.
 */
public interface Selector {
    /**
     * Chooses the most optimal Node
     *
     * @return a {@code ServiceNode} that is a suitable target for migration,
     * or null if there is no such {@code ServiceNode}
     */
    ServiceNode select(Collection<ServiceNode> nodes, MobileClient mobileClient);
}
