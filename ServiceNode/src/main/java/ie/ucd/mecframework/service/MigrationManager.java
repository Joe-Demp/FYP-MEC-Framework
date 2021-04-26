package ie.ucd.mecframework.service;

import ie.ucd.mecframework.servicenode.ServiceNodeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.transfer.TransferClient;
import service.transfer.TransferServer;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.CountDownLatch;

public class MigrationManager {
    private static final ServiceNodeProperties nodeProperties = ServiceNodeProperties.get();
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private File service;
    private ServiceController controller;

    public MigrationManager(File serviceFile, ServiceController controller) {
        this.service = serviceFile;
        this.controller = controller;
    }

    private static URI mapInetSocketAddressToWebSocketUri(InetSocketAddress address) {
        String uriString = String.format("ws://%s:%d", address.getHostString(), address.getPort());
        return URI.create(uriString);
    }

    /**
     * Stops the running service, launches a {@code TransferServer} and returns the {@code InetSocketAddress} that the
     * {@code TransferClient} on the target node can use to connect to the {@code TransferServer}.
     *
     * @return the address that the {@code TransferClient} can use to connect to the {@code TransferServer}.
     * The {@code TransferServer} port number might not be the same as the advertised port number because of NAT rules.
     */
    public InetSocketAddress migrateService() {
        controller.stopService();

        // todo refactor to use both port numbers: maybe use a Set of available ports.
        launchTransferServer(nodeProperties.getActualTransferServerPortNumber1());
        return new InetSocketAddress(nodeProperties.getAdvertisedTransferServerPortNumber1());
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

    /**
     * Makes this node set up a {@code TransferClient} and waits for the client to finish accepting the migrated service.
     */
    public void acceptService(InetSocketAddress serverAddress) {
        URI serverUri = mapInetSocketAddressToWebSocketUri(serverAddress);
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

    private void waitForTransferClient(CountDownLatch cdl) {
        try {
            cdl.await();
        } catch (InterruptedException ie) {
            logger.error("Interrupted exception in waitForCountDownLatch!", ie);
        }
    }
}
