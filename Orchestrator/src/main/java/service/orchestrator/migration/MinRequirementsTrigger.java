package service.orchestrator.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.orchestrator.nodes.ServiceNode;
import service.orchestrator.nodes.ServiceNodeRegistry;
import service.orchestrator.properties.OrchestratorProperties;

import java.io.IOException;
import java.util.*;
import java.util.stream.DoubleStream;

public class MinRequirementsTrigger implements Trigger {
    private static final Logger logger = LoggerFactory.getLogger(MinRequirementsTrigger.class);

    private Migrator migrator;

    public MinRequirementsTrigger(Migrator migrator) {
        this.migrator = migrator;
    }

    @Override
    public void examine(Collection<ServiceNode> nodes) {
        OrchestratorProperties properties = getOrchestratorProperties();
        List<ServiceNode> failingNodes = new LinkedList<>();

        // todo nodes

        for (ServiceNode node : nodes) {
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
        migrator.trigger(failingNodes);
    }

    private OrchestratorProperties getOrchestratorProperties() {
        OrchestratorProperties properties;
        try {
            properties = OrchestratorProperties.get();
        } catch (IOException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
            throw new MissingResourceException(
                    "No Orchestrator properties file found",
                    OrchestratorProperties.class.getSimpleName(),
                    "orchestrator.properties"
            );
        }
        return properties;
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
        examine(ServiceNodeRegistry.get().getServiceNodes());
    }
}
