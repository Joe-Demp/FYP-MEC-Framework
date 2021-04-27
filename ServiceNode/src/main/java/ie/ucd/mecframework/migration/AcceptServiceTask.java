package ie.ucd.mecframework.migration;

import ie.ucd.mecframework.service.ServiceController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class AcceptServiceTask implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(AcceptServiceTask.class);
    private final MigrationManager manager;
    private final InetSocketAddress transferServer;
    private final ServiceController controller;

    public AcceptServiceTask(MigrationManager manager, InetSocketAddress transferServer, ServiceController controller) {
        this.manager = manager;
        this.transferServer = transferServer;
        this.controller = controller;
    }

    @Override
    public void run() {
        manager.acceptService(transferServer);

        // todo remove logging
        if (controller.serviceExists()) logger.info("Success! ServiceExists!");
        else logger.warn("Problem! TransferClient did not save service in expected location (service does not exist).");

        controller.startService();
    }
}
