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
     *
     * <p>
     * Take care to only pass a Collection of nodes that are eligible to become a migration <b>source</b>,
     * i.e. only those returned by method {@code ServiceNodeRegistry.getHostingAndStableServiceNodes()}.
     * </p>
     */
    void examine(Collection<ServiceNode> hostingNodes);
}
