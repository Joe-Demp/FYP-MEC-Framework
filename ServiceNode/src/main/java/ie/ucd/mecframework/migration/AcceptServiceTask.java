package ie.ucd.mecframework.migration;

import ie.ucd.mecframework.service.ServiceController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;

public class AcceptServiceTask implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(AcceptServiceTask.class);
    private final MigrationManager manager;
    private final List<InetSocketAddress> transferServers;
    private final ServiceController controller;

    public AcceptServiceTask(MigrationManager manager, List<InetSocketAddress> transferServers,
                             ServiceController controller) {
        this.manager = manager;
        this.transferServers = transferServers;
        this.controller = controller;
    }

    @Override
    public void run() {
        manager.acceptService(transferServers);

        // todo remove logging
        if (controller.serviceExists()) logger.info("Success! ServiceExists!");
        else logger.warn("Problem! TransferClient did not save service in expected location (service does not exist).");

        controller.startService();
    }
}
