package service.orchestrator.migration;

import service.core.MobileClientInfo;
import service.core.NodeInfo;
import service.orchestrator.nodes.ServiceNode;

import java.util.Collection;

// todo revisit

/**
 * A type for objects that choose optimal target nodes for migration.
 */
public interface Selector {
    /**
     * Chooses the most optimal Node
     */
    NodeInfo select(Collection<ServiceNode> nodes, MobileClientInfo mobileClient);

    // todo create an implementation of this
}
