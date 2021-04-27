package ie.ucd.mecframework.migration;

import ie.ucd.mecframework.service.ServiceController;
import ie.ucd.mecframework.servicenode.ServiceNodeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.transfer.TransferServer;

import java.io.File;
import java.net.InetSocketAddress;

public class StatefulMigrationStrategy implements MigrationStrategy {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ServiceController controller;
    private final ServiceNodeProperties nodeProperties = ServiceNodeProperties.get();
    private final File service;
    private final File state;

    public StatefulMigrationStrategy(ServiceController controller, File service, File state) {
        this.controller = controller;
        this.service = service;
        this.state = state;
    }

    @Override
    public InetSocketAddress migrateService() {
        controller.stopService();
        launchTransferServer(nodeProperties.getActualTransferServerPortNumber1());

        throw new UnsupportedOperationException();
    }

    /**
     * This method launches this nodes Transfer Server according to the constants defined in service.core.Constants.
     */
    private void launchTransferServer(int transferServerPortNumber) {
        InetSocketAddress serverAddress = new InetSocketAddress(transferServerPortNumber);
        logger.debug("Launching Transfer Server at {}", serverAddress);
        TransferServer transferServer = new TransferServer(serverAddress, service);
        transferServer.setConnectionLostTimeout(-1);
        transferServer.start();
    }

    @Override
    public void acceptService(InetSocketAddress serverAddress) {
        throw new UnsupportedOperationException();
    }
}
