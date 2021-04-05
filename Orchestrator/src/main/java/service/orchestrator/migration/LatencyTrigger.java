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

import static java.util.Objects.nonNull;

public class LatencyTrigger implements Trigger {
    private static final Logger logger = LoggerFactory.getLogger(LatencyTrigger.class);

    // todo inject this
    private static final SimpleSelector selector = new SimpleSelector();
    private final Migrator migrator;

    // this implementation gets the max latency, or Long.MAX_VALUE if latencies is the empty list
    private static long aggregateLatencies(List<Long> latencies) {
        return latencies.stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(Long.MAX_VALUE);
    }

    public LatencyTrigger(Migrator migrator) {
        this.migrator = migrator;
    }

    @Override
    public void examine(Collection<ServiceNode> nodes) {
        OrchestratorProperties properties = OrchestratorProperties.get();
        logger.debug("{} nodes in examine", nodes.size());

        for (ServiceNode node : nodes) {
            for (Map.Entry<UUID, List<Long>> mcLatencyEntry : node.latencyEntries()) {
                logger.debug("{} has {} latencies", mcLatencyEntry.getKey(), mcLatencyEntry.getValue().size());

                long latencyAggregate = aggregateLatencies(mcLatencyEntry.getValue());
                if (latencyAggregate > properties.getMaxLatency()) {
                    logger.debug("{} has high latency {}", mcLatencyEntry.getKey(), latencyAggregate);
                    handleHighLatencyNode(node);
                } else {
                    logger.debug("{} has low latency {}", mcLatencyEntry.getKey(), latencyAggregate);
                }
            }
        }
    }

    private void handleHighLatencyNode(ServiceNode node) {
        ServiceNode migrationTarget = selector.selectMigrationTarget(node);
        if (nonNull(migrationTarget)) {
            migrator.migrate(node, migrationTarget);
        }
    }

    @Override
    public void run() {
        logger.debug("Running LatencyTrigger");
        examine(ServiceNodeRegistry.get().getHostingAndStableServiceNodes());
    }
}
