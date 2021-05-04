package ie.ucd.mecframework.migration;

import ie.ucd.mecframework.migration.transfer.TransferClient;
import ie.ucd.mecframework.migration.transfer.TransferServer;
import ie.ucd.mecframework.service.ServiceController;
import ie.ucd.mecframework.servicenode.ServiceNodeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class StatelessMigrationStrategy implements MigrationStrategy {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ServiceController controller;
    private final ServiceNodeProperties nodeProperties = ServiceNodeProperties.get();
    private final File service;

    public StatelessMigrationStrategy(ServiceController controller, File service) {
        this.controller = controller;
        this.service = service;
    }

    private static URI makeWebSocketUri(InetSocketAddress address) {
        String uriString = String.format("ws://%s:%d", address.getHostString(), address.getPort());
        return URI.create(uriString);
    }

    @Override
    public List<InetSocketAddress> migrateService() {
        controller.stopService();
        launchTransferServer(nodeProperties.getActualTransferServerPortNumber1());
        return List.of(
                new InetSocketAddress(nodeProperties.getAdvertisedTransferServerPortNumber1())
        );
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
    public void acceptService(List<InetSocketAddress> serverAddresses) {
        InetSocketAddress serverAddress = serverAddresses.get(0);
        URI serverUri = makeWebSocketUri(serverAddress);
        CountDownLatch transferFinished = new CountDownLatch(1);
        TransferClient transferClient = new TransferClient(serverUri, service, transferFinished);
        transferClient.setConnectionLostTimeout(-1);
        doTransfer(transferClient, transferFinished);
    }

    private void doTransfer(TransferClient transferClient, CountDownLatch transferFinished) {
        transferClient.connect();
        waitForTransferClient(transferFinished);
        transferClient.close();
    }

    private void waitForTransferClient(CountDownLatch transferFinished) {
        try {
            transferFinished.await();
        } catch (InterruptedException ie) {
            logger.error("Interrupted exception in waitForCountDownLatch!", ie);
        }
    }
}
