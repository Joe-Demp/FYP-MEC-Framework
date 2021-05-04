package service.orchestrator.migration;

import service.orchestrator.clients.MobileClient;
import service.orchestrator.nodes.ServiceNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A selector that first favours {@code ServiceNodes} that already have a service installed, and then favours the best
 * performing {@code ServiceNode} according to another {@code Selector} provided in the constructor.
 */
public class HighAvailabilitySelector implements Selector {
    private final Selector subSelector;

    public HighAvailabilitySelector(Selector subSelector) {
        this.subSelector = subSelector;
    }

    private static <T> Collection<T> aLessB(Collection<T> A, Collection<T> B) {
        List<T> allNodes = new ArrayList<>(A);
        allNodes.removeAll(B);
        return allNodes;
    }

    @Override
    public ServiceNode select(Collection<ServiceNode> nodes, MobileClient mobileClient) {
        Collection<ServiceNode> nodesWithService = nodesWithServiceInstalled(nodes);
        if (!nodesWithService.isEmpty()) {
            return subSelector.select(nodesWithService, mobileClient);
        } else {
            return subSelector.select(nodesWithoutServiceInstalled(nodes), mobileClient);
        }
    }

    private Collection<ServiceNode> nodesWithServiceInstalled(Collection<ServiceNode> nodes) {
        return nodes.stream()
                .filter(node -> node.serviceInstalled)
                .collect(Collectors.toList());
    }

    private Collection<ServiceNode> nodesWithoutServiceInstalled(Collection<ServiceNode> nodes) {
        Collection<ServiceNode> nodesWithService = nodesWithServiceInstalled(nodes);
        return aLessB(nodes, nodesWithService);
    }
}
