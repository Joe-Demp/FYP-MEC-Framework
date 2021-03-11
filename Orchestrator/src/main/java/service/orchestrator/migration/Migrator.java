package service.orchestrator.migration;

import service.orchestrator.nodes.ServiceNode;

import java.util.Collection;

/**
 * A type for objects that migrate services or artifacts.
 */
public interface Migrator {
    /**
     * Causes the {@code Migrator} to act on a given trigger, represented as a {@code Collection} of {@code NodeInfo}.
     */
    void trigger(Collection<ServiceNode> nodes);
}
