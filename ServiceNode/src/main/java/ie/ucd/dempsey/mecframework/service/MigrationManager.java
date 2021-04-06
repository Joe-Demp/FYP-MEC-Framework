package ie.ucd.dempsey.mecframework.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.core.Constants;
import service.transfer.TransferClient;
import service.transfer.TransferServer;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URI;

public class MigrationManager {
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

    public InetSocketAddress migrateService() {
        controller.stopService();
        return launchTransferServer();

        // todo deletion done by the TransferServer, ideally this should be done here.
    }

    /**
     * This method launches this nodes Transfer Server using the service address defined at node creation.
     *
     * @return the address of the newly launched transfer server.
     */
    private InetSocketAddress launchTransferServer() {
        InetSocketAddress serverAddress = new InetSocketAddress(Constants.TRANSFER_SERVER_PORT);
        logger.debug("Launching Transfer Server at {}", serverAddress);
        TransferServer transferServer = new TransferServer(serverAddress, service);
        transferServer.start();
        return serverAddress;
    }

    /**
     * Makes this node set up a {@code TransferClient} and waits for the client to finish accepting the migrated service.
     */
    public void acceptService(InetSocketAddress serverAddress) {
        // launch a TransferClient and wait for it to finish
        URI serverUri = mapInetSocketAddressToWebSocketUri(serverAddress);
        TransferClient transferClient = new TransferClient(serverUri, service);
        transferClient.run();
    }
}
