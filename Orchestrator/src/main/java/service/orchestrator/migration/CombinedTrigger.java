package service.orchestrator.migration;

import service.orchestrator.clients.MobileClient;
import service.orchestrator.clients.MobileClientRegistry;
import service.orchestrator.nodes.ServiceNode;
import service.orchestrator.nodes.ServiceNodeRegistry;

import java.util.*;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class CombinedTrigger implements Trigger {
    private final Selector selector;
    private final Migrator migrator;
    private final DeferredMigrator deferredMigrator;
    private final List<Trigger> triggers;

    public CombinedTrigger(Selector selector, Migrator migrator, DeferredMigrator deferredMigrator,
                           Trigger... triggers) {
        this.selector = selector;
        this.migrator = migrator;
        this.deferredMigrator = deferredMigrator;
        this.triggers = Arrays.asList(triggers);
    }

    @Override
    public void examine(Collection<ServiceNode> hostingNodes) {
        for (Trigger trigger : triggers) {
            trigger.examine(hostingNodes);
        }

        List<ServiceNode> triggerableNodes = deferredMigrator.getTriggerableServiceNodes();
        if (!triggerableNodes.isEmpty()) {
            findTargetServiceNode(triggerableNodes.get(0));
        }
    }

    private void findTargetServiceNode(ServiceNode currentServiceNode) {
        MobileClient mobileClient = getStrugglingMobileClient(currentServiceNode);
        Collection<ServiceNode> nonHostingNodes = ServiceNodeRegistry.get().getNonHostingNodes();

        ServiceNode migrationTarget = selector.select(nonHostingNodes, mobileClient);
        if (nonNull(migrationTarget)) {
            migrator.migrate(currentServiceNode, migrationTarget);
        }
    }

    private MobileClient getStrugglingMobileClient(ServiceNode serviceNode) {
        UUID clientUuid = serviceNode.latencyEntries().stream()
                .map(Map.Entry::getKey)
                .findAny()
                .orElse(null);
        return isNull(clientUuid) ? null : MobileClientRegistry.get().get(clientUuid);
    }

    @Override
    public void run() {
        examine(ServiceNodeRegistry.get().getHostingAndStableServiceNodes());
    }
}
