package service.orchestrator.migration;

import service.orchestrator.nodes.ServiceNode;
import service.orchestrator.nodes.ServiceNodeRegistry;
import service.orchestrator.properties.OrchestratorProperties;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.LongStream;

public class LatencyTrigger implements Trigger {
    // this implementation gets the max latency, or Long.MAX_VALUE if latencies is the empty list
    private static long aggregateLatencies(List<Long> latencies) {
        return latencies.stream()
                .flatMapToLong(LongStream::of)
                .max()
                .orElse(Long.MAX_VALUE);
    }

    @Override
    public void examine(Collection<ServiceNode> nodes) {
        OrchestratorProperties properties = OrchestratorProperties.get();

        for (ServiceNode node : nodes) {
            for (Map.Entry<UUID, List<Long>> mcLatencyEntry : node.mobileClientLatencies.entrySet()) {
                long latencyAggregate = aggregateLatencies(mcLatencyEntry.getValue());
                if (latencyAggregate > properties.getMaxLatency()) {
                    // do something about it
                }
            }
        }
    }

    @Override
    public void run() {
        examine(ServiceNodeRegistry.get().getServiceNodes());
    }
}
