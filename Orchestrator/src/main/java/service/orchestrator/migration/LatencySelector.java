package service.orchestrator.migration;

import service.orchestrator.clients.MobileClient;
import service.orchestrator.nodes.ServiceNode;

import java.util.Collection;
import java.util.UUID;

import static java.util.Comparator.naturalOrder;
import static java.util.Objects.nonNull;

public class LatencySelector implements Selector {
    @Override
    public ServiceNode select(Collection<ServiceNode> nodes, MobileClient mobileClient) {
        NodeLatencyPair nodeLatencyPair = nodes.stream()
                .map(node -> new NodeLatencyPair(node, mobileClient.uuid))
                .min(naturalOrder())
                .orElse(null);
        return nonNull(nodeLatencyPair) ? nodeLatencyPair.node : null;
    }

    private static class NodeLatencyPair implements Comparable<NodeLatencyPair> {
        private final double latency;
        ServiceNode node;

        NodeLatencyPair(ServiceNode node, UUID clientUuid) {
            this.node = node;
            this.latency = node.getMeanLatency(clientUuid);
        }

        @Override
        public int compareTo(NodeLatencyPair nodeLatencyPair) {
            return Double.compare(latency, nodeLatencyPair.latency);
        }
    }
}
