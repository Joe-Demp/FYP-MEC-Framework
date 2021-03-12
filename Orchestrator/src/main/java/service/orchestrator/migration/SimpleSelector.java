package service.orchestrator.migration;

import service.orchestrator.nodes.ServiceNode;
import service.orchestrator.nodes.ServiceNodeRegistry;

/**
 * Selector that chooses any old ServiceNode.
 */
public class SimpleSelector {
    public ServiceNode selectMigrationTarget(ServiceNode source) {
        return ServiceNodeRegistry.get().getServiceNodes().stream()
                .filter(node -> !node.equals(source))
                .findAny()
                .orElse(null);
    }
}
