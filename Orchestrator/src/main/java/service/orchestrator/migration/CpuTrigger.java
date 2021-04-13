package service.orchestrator.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.orchestrator.nodes.ServiceNode;
import service.orchestrator.nodes.ServiceNodeRegistry;
import service.orchestrator.properties.OrchestratorProperties;

import java.util.Collection;

import static java.util.Objects.nonNull;

public class CpuTrigger implements Trigger {
    private static final Logger logger = LoggerFactory.getLogger(CpuTrigger.class);

    private final Selector selector;
    private final Migrator migrator;

    public CpuTrigger(Selector selector, Migrator migrator) {
        this.selector = selector;
        this.migrator = migrator;
    }

    @Override
    public void examine(Collection<ServiceNode> hostingNodes) {
        OrchestratorProperties properties = OrchestratorProperties.get();
        logger.debug("{} nodes in examine", hostingNodes.size());

        for (ServiceNode node : hostingNodes) {
            logger.debug("examining {}", node.uuid);
            double cpuScore = node.getCpuScore();

            if (cpuScore > properties.getMaxCpu()) {
                logger.debug("{} has high cpu {}", node.uuid, cpuScore);
                triggerMigration(node);
            } else {
                logger.debug("{} has low cpu {}", node.uuid, cpuScore);
            }
        }
    }

    private void triggerMigration(ServiceNode currentServiceNode) {
        Collection<ServiceNode> allServiceNodes = ServiceNodeRegistry.get().getServiceNodes();
        ServiceNode migrationTarget = selector.select(allServiceNodes, null);
        if (nonNull(migrationTarget)) {
            migrator.migrate(currentServiceNode, migrationTarget);
        }
    }

    @Override
    public void run() {
        examine(ServiceNodeRegistry.get().getHostingAndStableServiceNodes());
    }
}
