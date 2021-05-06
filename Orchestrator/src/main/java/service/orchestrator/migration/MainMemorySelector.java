package service.orchestrator.migration;

import service.orchestrator.clients.MobileClient;
import service.orchestrator.nodes.ServiceNode;
import service.orchestrator.properties.OrchestratorProperties;

import java.util.Collection;

import static java.util.Comparator.naturalOrder;
import static java.util.Objects.nonNull;

public class MainMemorySelector implements Selector {
    @Override
    public ServiceNode select(Collection<ServiceNode> nodes, MobileClient mobileClient) {
        OrchestratorProperties properties = OrchestratorProperties.get();
        NodeMemory nodeCpuPair = nodes.stream()
                .map(NodeMemory::new)
                .filter(mem -> mem.memoryAmount > properties.getMinMemoryGibibytes())
                .min(naturalOrder())
                .orElse(null);
        return nonNull(nodeCpuPair) ? nodeCpuPair.node : null;
    }

    private static class NodeMemory implements Comparable<NodeMemory> {
        final ServiceNode node;
        private final double memoryUtilization;
        private final double memoryAmount;

        NodeMemory(ServiceNode node) {
            this.node = node;
            this.memoryUtilization = node.getMainMemoryScore();
            this.memoryAmount = node.getMainMemoryInGibibytes();
        }

        @Override
        public int compareTo(NodeMemory other) {
            return Double.compare(memoryUtilization, other.memoryUtilization);
        }
    }
}
