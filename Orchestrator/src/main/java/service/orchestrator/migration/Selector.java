package service.orchestrator.migration;

import service.core.MobileClientInfo;
import service.core.NodeInfo;

import java.util.Collection;

// todo revisit
/**
 * A type for objects that choose optimal target nodes for migration.
 */
public interface Selector {
    /**
     * Chooses the most optimal Node
     */
    NodeInfo select(Collection<NodeInfo> nodes, MobileClientInfo mobileClient);
}
