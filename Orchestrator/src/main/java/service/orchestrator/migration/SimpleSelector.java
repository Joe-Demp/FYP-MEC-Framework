package service.orchestrator.migration;

import service.orchestrator.clients.MobileClient;
import service.orchestrator.nodes.ServiceNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

/**
 * Selector that chooses any old ServiceNode.
 */
public class SimpleSelector implements Selector {
    private static Random random = new Random(2021);

    @Override
    public ServiceNode select(Collection<ServiceNode> nodes, MobileClient mobileClient) {
        if (nodes.isEmpty()) return null;

        int index = random.nextInt(nodes.size());
        return new ArrayList<>(nodes).get(index);
    }
}
