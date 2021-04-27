package ie.ucd.mecframework.migration;

import ie.ucd.mecframework.service.ServiceController;
import ie.ucd.mecframework.servicenode.ServiceNodeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.transfer.TransferClient;
import service.transfer.TransferServer;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CountDownLatch;

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

    private static TransferClient makeTransferClient(InetSocketAddress address, File file, CountDownLatch latch) {
        URI serviceUri = makeWebSocketUri(address);
        return new TransferClient(serviceUri, file, latch);
    }

    private static URI makeWebSocketUri(InetSocketAddress address) {
        String uriString = String.format("ws://%s:%d", address.getHostString(), address.getPort());
        return URI.create(uriString);
    }

    @Override
    public List<InetSocketAddress> migrateService() {
        controller.stopService();
        launchTransferServer(nodeProperties.getActualTransferServerPortNumber1(), service);
        launchTransferServer(nodeProperties.getActualTransferServerPortNumber2(), state);

        return List.of(
                new InetSocketAddress(nodeProperties.getAdvertisedTransferServerPortNumber1()),
                new InetSocketAddress(nodeProperties.getAdvertisedTransferServerPortNumber2())
        );
    }

    /**
     * This method launches this nodes Transfer Server according to the constants defined in service.core.Constants.
     */
    private void launchTransferServer(int transferServerPortNumber, File fileToTransfer) {
        InetSocketAddress serverAddress = new InetSocketAddress(transferServerPortNumber);
        logger.debug("Launching Transfer Server at {}", serverAddress);
        TransferServer transferServer = new TransferServer(serverAddress, fileToTransfer);
        transferServer.setConnectionLostTimeout(-1);
        transferServer.start();
    }

    @Override
    public void acceptService(List<InetSocketAddress> serverAddress) {
        CountDownLatch transferFinished = new CountDownLatch(2);
        List<TransferClient> transferClients = makeTransferClients(serverAddress, transferFinished);
        doTransfers(transferFinished, transferClients);
    }

    private void doTransfers(CountDownLatch transferFinished, List<TransferClient> transferClients) {
        transferClients.forEach(TransferClient::connect);
        waitForTransferClient(transferFinished);
        transferClients.forEach(TransferClient::close);
    }

    private List<TransferClient> makeTransferClients(
            List<InetSocketAddress> serverAddress, CountDownLatch transferFinished
    ) {
        return List.of(
                makeTransferClient(serverAddress.get(0), service, transferFinished),
                makeTransferClient(serverAddress.get(1), state, transferFinished)
        );
    }

    private void waitForTransferClient(CountDownLatch transferFinished) {
        try {
            transferFinished.await();
        } catch (InterruptedException ie) {
            logger.error("Interrupted exception in waitForCountDownLatch!", ie);
        }
    }
}
