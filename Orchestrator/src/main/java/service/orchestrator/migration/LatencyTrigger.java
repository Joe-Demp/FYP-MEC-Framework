package service.orchestrator.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.orchestrator.clients.MobileClient;
import service.orchestrator.clients.MobileClientRegistry;
import service.orchestrator.nodes.ServiceNode;
import service.orchestrator.nodes.ServiceNodeRegistry;
import service.orchestrator.properties.OrchestratorProperties;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.nonNull;

// separate Trigger and TriggerStrategy -> Latency, CPU, Memory, Storage, Combined (AND/OR)
public class LatencyTrigger implements Trigger {
    private static final Logger logger = LoggerFactory.getLogger(LatencyTrigger.class);

    private final Selector selector;
    private final Migrator migrator;

    public LatencyTrigger(Selector selector, Migrator migrator) {
        this.selector = selector;
        this.migrator = migrator;
    }

    private static double meanLatency(List<Long> latencies) {
        return latencies.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);
    }

    @Override
    public void examine(Collection<ServiceNode> hostingNodes) {
        OrchestratorProperties properties = OrchestratorProperties.get();
        logger.debug("{} nodes in examine", hostingNodes.size());

        for (ServiceNode node : hostingNodes) {
            logger.debug("examining {}", node.uuid);
            for (Map.Entry<UUID, List<Long>> mcLatencyEntry : node.latencyEntries()) {
                logger.debug("{} has {} latencies", mcLatencyEntry.getKey(), mcLatencyEntry.getValue().size());

                double latencyAggregate = meanLatency(mcLatencyEntry.getValue());
                if (latencyAggregate > properties.getMaxLatency()) {
                    logger.debug("{} has high latency {}", mcLatencyEntry.getKey(), latencyAggregate);
                    handleHighLatencyNode(node, mcLatencyEntry.getKey());
                } else {
                    logger.debug("{} has low latency {}", mcLatencyEntry.getKey(), latencyAggregate);
                }
            }
        }
    }

    // todo remove some of the parameters here
    private void handleHighLatencyNode(ServiceNode highLatency, UUID mobileClientUuid) {
        MobileClient mobile = MobileClientRegistry.get().get(mobileClientUuid);
        Collection<ServiceNode> allServiceNodes = ServiceNodeRegistry.get().getServiceNodes();

        ServiceNode migrationTarget = selector.select(allServiceNodes, mobile);
        if (nonNull(migrationTarget)) {
            migrator.migrate(highLatency, migrationTarget);
        }
    }

    @Override
    public void run() {
        logger.debug("Running LatencyTrigger");
        examine(ServiceNodeRegistry.get().getHostingAndStableServiceNodes());
    }
}
