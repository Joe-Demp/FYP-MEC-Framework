package service.orchestrator.migration;

import service.orchestrator.nodes.ServiceNode;

import java.util.Collection;

/**
 * A type for objects that can process Node data and suggest Nodes for migration.
 */
public interface Trigger extends Runnable {
    /**
     * Checks the given {@code Collection} of {@code ServiceNode} objects for Nodes hosting a service
     * that should be moved to another Node.
     */
    void examine(Collection<ServiceNode> nodes);
}
