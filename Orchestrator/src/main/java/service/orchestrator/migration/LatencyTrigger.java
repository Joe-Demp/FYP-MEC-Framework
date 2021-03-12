package service.orchestrator.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.orchestrator.nodes.ServiceNode;
import service.orchestrator.nodes.ServiceNodeRegistry;
import service.orchestrator.properties.OrchestratorProperties;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.LongStream;

public class LatencyTrigger implements Trigger {
    private static final Logger logger = LoggerFactory.getLogger(LatencyTrigger.class);

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
            for (Map.Entry<UUID, List<Long>> mcLatencyEntry : node.getLatencies().entrySet()) {
                long latencyAggregate = aggregateLatencies(mcLatencyEntry.getValue());
                if (latencyAggregate > properties.getMaxLatency()) {
                    // do something about it
                    logger.debug("{} has high latency {}", mcLatencyEntry.getKey(), latencyAggregate);
                } else {
                    // todo remove this unnecessary branch
                    logger.debug("{} has low latency {}", mcLatencyEntry.getKey(), latencyAggregate);
                }
            }
        }
    }

    @Override
    public void run() {
        examine(ServiceNodeRegistry.get().getServiceNodes());
    }
}
