package service.orchestrator.migration;

import service.orchestrator.nodes.ServiceNode;

// todo so far the Migrator also tends to start the service on the target node,
//  ideally these operations would be decoupled

/**
 * A type for objects that migrate services or artifacts.
 */
public interface Migrator {
    /**
     * Starts the migration process between two {@code ServiceNode}s.
     *
     * @param source the {@code ServiceNode} that the application will migrate from.
     * @param target the {@code ServiceNode} that the application will migrate to.
     */
    void migrate(ServiceNode source, ServiceNode target);
}
