package service.orchestrator.migration;

import service.orchestrator.clients.MobileClient;
import service.orchestrator.nodes.ServiceNode;

import java.util.Collection;

import static java.util.Comparator.naturalOrder;
import static java.util.Objects.nonNull;

public class CpuSelector implements Selector {
    @Override
    public ServiceNode select(Collection<ServiceNode> nodes, MobileClient mobileClient) {
        NodeCpuPair nodeCpuPair = nodes.stream()
                .map(NodeCpuPair::new)
                .min(naturalOrder())
                .orElse(null);
        return nonNull(nodeCpuPair) ? nodeCpuPair.node : null;
    }

    private static class NodeCpuPair implements Comparable<NodeCpuPair> {
        private final double cpu;
        final ServiceNode node;

        NodeCpuPair(ServiceNode node) {
            this.node = node;
            this.cpu = node.getCpuScore();
        }

        @Override
        public int compareTo(NodeCpuPair other) {
            return Double.compare(cpu, other.cpu);
        }
    }
}
