package ie.ucd.dempsey.mecframework.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.core.Constants;
import service.host.ServiceHost;
import service.transfer.DockerController;
import service.transfer.TransferClient;
import service.transfer.TransferServer;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

public class MigrationManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    // todo remove this: should be A ServiceController owned by the ServiceNode
    DockerController dockerController;
    // todo see if the Manager needs this
    private URI serviceAddress;
    private File service;

    public MigrationManager(File serviceFile) {
        this.service = serviceFile;
    }

    private static URI mapInetSocketAddressToWebSocketUri(InetSocketAddress address) {
        String uriString = String.format("ws://%s:%d", address.getHostString(), address.getPort());
        return URI.create(uriString);
    }

    /**
     * This method launches this nodes Transfer Server using the service address define at node creation.
     *
     * @return the address of the newly launched transfer server.
     */
    public InetSocketAddress launchTransferServer() {
        InetSocketAddress serverAddress = new InetSocketAddress(Constants.TRANSFER_SERVER_PORT);
        logger.debug("Launching Transfer Server at {}", serverAddress);
        TransferServer transferServer = new TransferServer(serverAddress, service);
        transferServer.start();
        return serverAddress;
    }

    public void launchTransferClient(InetSocketAddress serverAddress) throws URISyntaxException, UnknownHostException {
        URI serverUri = mapInetSocketAddressToWebSocketUri(serverAddress);

        TransferClient transferClient = new TransferClient(serverUri, dockerController);
        transferClient.connect();

        /* todo use a new thread instead of spinning
            TransferAndStartService implements Runnable should:
            * Start the TransferClient, await its completion.
            * Alert the ServiceNode
            * exit
         */

        while (transferClient.dockerControllerReady() == null) {
        }
        // todo the method above does not make sure docker was launched. Fix it
        // todo FIXME sometimes blocks here

        logger.info("The transfer client says Docker was launched.");
        DockerController dockerController = transferClient.dockerControllerReady();
        transferClient.close();
        logger.info("Closed the TransferClient and launching the service on Docker");
        launchServiceOnDockerController();
    }

    /**
     * This method will launch the host server that will allow users to communicate with the docker instance
     * <p>
     * todo delete
     */
    private void launchServiceOnDockerController() throws UnknownHostException {
        // should tell the ServiceNode to ask the ServiceController to do this

        ServiceHost serviceHost = new ServiceHost(serviceAddress.getPort(), dockerController);

        logger.info("Starting the serviceHost");
        serviceHost.start();
    }
}
