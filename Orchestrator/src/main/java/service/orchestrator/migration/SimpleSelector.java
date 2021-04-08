package service.orchestrator.migration;

import service.orchestrator.clients.MobileClient;
import service.orchestrator.nodes.ServiceNode;

import java.util.Collection;

/**
 * Selector that chooses any old ServiceNode.
 */
public class SimpleSelector implements Selector {
    @Override
    public ServiceNode select(Collection<ServiceNode> nodes, MobileClient mobileClient) {
        return nodes.stream()
                .findAny()
                .orElse(null);
    }
}
