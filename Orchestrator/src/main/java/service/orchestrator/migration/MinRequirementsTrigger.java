package service.orchestrator.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.orchestrator.nodes.ServiceNode;
import service.orchestrator.nodes.ServiceNodeRegistry;
import service.orchestrator.properties.OrchestratorProperties;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.DoubleStream;

public class MinRequirementsTrigger implements Trigger {
    private static final Logger logger = LoggerFactory.getLogger(MinRequirementsTrigger.class);

    private Migrator migrator;

    public MinRequirementsTrigger(Migrator migrator) {
        this.migrator = migrator;
    }

    @Override
    public void examine(Collection<ServiceNode> hostingNodes) {
        OrchestratorProperties properties = OrchestratorProperties.get();
        List<ServiceNode> failingNodes = new LinkedList<>();

        // todo nodes

        for (ServiceNode node : hostingNodes) {
            double meanCpu = node.getMeanCPU();
            double meanRam = node.getMeanRam();
            double meanStorage = node.getMeanStorage();

            // todo have a check for latencies here

            if (
                    meanCpu > properties.getMaxCpu()
                            || meanRam > properties.getMaxMemory()
                            || meanStorage > properties.getMinStorage()
            ) {
                logger.info("Flagging NodeInfo at {} to be decommissioned.",
                        node.webSocket.getRemoteSocketAddress());
                failingNodes.add(node);
            }
        }
        logger.error("MinRequirementsTrigger.examine not calling migrator.migrate(...)");
//        migrator.trigger(failingNodes);
    }

    /**
     * Gets the mean of the given {@code Collection}, or Double.MAX_VALUE if the Collection is empty.
     */
    private double getMean(Collection<Double> numbers) {
        OptionalDouble optionalSum = numbers.stream().flatMapToDouble(DoubleStream::of).average();
        return optionalSum.orElse(Double.MAX_VALUE);
    }

    @Override
    public void run() {
        examine(ServiceNodeRegistry.get().getHostingAndStableServiceNodes());
    }
}
