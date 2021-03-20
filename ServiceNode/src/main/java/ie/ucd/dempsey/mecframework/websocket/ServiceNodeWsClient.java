package ie.ucd.dempsey.mecframework.websocket;

import com.google.gson.Gson;
import ie.ucd.dempsey.mecframework.metrics.latency.LatencyRequestMonitor;
import ie.ucd.dempsey.mecframework.metrics.latency.LatencyRequestor;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;
import service.core.*;
import service.host.ServiceHost;
import service.transfer.DockerController;
import service.transfer.TransferClient;
import service.transfer.TransferServer;
import service.util.Gsons;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/*
    todo extract the following to separate classes
    * SystemInfo, HardwareAbstractionLayer, OperatingSystem
    * DockerController, File etc. (service state should not change this class)
    * ServiceNode (probably) to store all of the info that the WebSocketClient does not need
 */
public class ServiceNodeWsClient extends WebSocketClient {
    private final Logger logger = LoggerFactory.getLogger(ServiceNodeWsClient.class);

    private final Gson gson = Gsons.serviceNodeGson();
    private final LatencyRequestMonitor latencyRequestMonitor;
    private final LatencyRequestor latencyRequestor;
    private final SystemInfo nodeSystem = new SystemInfo();
    private final HardwareAbstractionLayer hal = nodeSystem.getHardware();
    private final OperatingSystem os = nodeSystem.getOperatingSystem();
    private final DockerController dockerController = new DockerController();
    private final String label;
    private File service;
    private UUID uuid;
    /**
     * Address of the current running service/application
     * (In a Docker Container for now).
     */
    private URI serviceAddress;
    private Map<Integer, Double> historicalCPUload = new HashMap<>();
    private Map<Integer, Double> historicalRamload = new HashMap<>();
    private Map<Integer, Long> unusedStorage = new HashMap<>();

    public ServiceNodeWsClient(URI serverUri, File serviceFile, URI serviceAddress,
                               LatencyRequestMonitor latencyRequestMonitor, LatencyRequestor latencyRequestor,
                               String label) {
        super(serverUri);
        this.service = serviceFile;
        this.serviceAddress = serviceAddress;
        this.latencyRequestMonitor = latencyRequestMonitor;
        this.latencyRequestor = latencyRequestor;
        this.label = label;
    }

    /**
     * Converts the given message to JSON, and sends that JSON String along the given WebSocket.
     */
    public final void sendAsJson(Message message) {
        String json = gson.toJson(message);
        logger.debug("Sending: {}", json);
        send(json);
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        logger.info("Listening to orchestrator at {} on local address {}",
                getRemoteSocketAddress(), getLocalSocketAddress());
    }

    private static URI mapInetSocketAddressToWebSocketUri(InetSocketAddress address) {
        String uriString = String.format("ws://%s:%d", address.getHostString(), address.getPort());
        return URI.create(uriString);
    }

    @Override
    public void onMessage(String message) {
        logger.debug("Received: {}", message);

        Message messageObj = gson.fromJson(message, Message.class);
        switch (messageObj.getType()) {
            case Message.MessageTypes.NODE_INFO_REQUEST:
                handleNodeInfoRequest((NodeInfoRequest) messageObj);
                break;
            case Message.MessageTypes.SERVER_HEARTBEAT_REQUEST:
                sendHeartbeatResponse();
                break;
            case Message.MessageTypes.SERVICE_REQUEST:
                handleServiceRequest((ServiceRequest) messageObj);
                break;
            case Message.MessageTypes.SERVICE_RESPONSE:
                handleServiceResponse((ServiceResponse) messageObj);
                break;
            case Message.MessageTypes.NODE_CLIENT_LATENCY_REQUEST:
                latencyRequestor.registerRequest((NodeClientLatencyRequest) messageObj);
                break;
            default:
                logger.error("Message received with unrecognised type: {}", messageObj.getType());
                break;
        }
    }

    /**
     * Processes NodeInfoRequests, sent to the Service Node by the Orchestrator when a connection is opened.
     *
     * <p>Takes the assigned {@code UUID} and sends a heartbeat response.</p>
     */
    private void handleNodeInfoRequest(NodeInfoRequest request) {
        uuid = request.getAssignedUUID();
        sendHeartbeatResponse();
    }

    private void sendHeartbeatResponse() {
        NodeInfo nodeInfo = new NodeInfo(uuid, service.getName(), serviceAddress);

        // adding performance data
        // todo replace these with recent values only, not the entire map
        logger.warn("No updates made to historicalCPUload or historicalRamload or ...");
        nodeInfo.setCPUload(historicalCPUload);
        nodeInfo.setRamLoad(historicalRamload);
        nodeInfo.setUnusedStorage(unusedStorage);
        nodeInfo.setLatencies(latencyRequestMonitor.takeLatencySnapshot());
        // END adding performance data

        sendAsJson(nodeInfo);
    }

    /**
     * Starts the process wherein this Service Node will send its application to the target Service Node.
     */
    private void handleServiceRequest(ServiceRequest request) {
        logger.info("{} received a ServiceRequest!", label);
        InetSocketAddress transferServerAddress = launchTransferServer();
        sendServiceResponse(request, transferServerAddress);
    }

    /**
     * Starts the process wherein this Service Node will receive an application from the source Service Node.
     */
    private void handleServiceResponse(ServiceResponse response) {
        logger.info("{} received a ServiceResponse!", label);

        try {
            launchTransferClient(response.getTransferServerAddress());
            MigrationSuccess migrationSuccess = new MigrationSuccess(uuid, response.getSourceNodeUuid(), response.getServiceName());
            sendAsJson(migrationSuccess);
        } catch (URISyntaxException | UnknownHostException ex) {
            logger.error("Problem launching the TransferClient", ex);
        }
    }

    /**
     * This method launches this nodes Transfer Server using the service address define at node creation.
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

    private void launchTransferClient(InetSocketAddress serverAddress) throws URISyntaxException, UnknownHostException {
        URI serverUri = mapInetSocketAddressToWebSocketUri(serverAddress);

        TransferClient transferClient = new TransferClient(serverUri, dockerController);
        transferClient.connect();

        /* todo use a new thread instead of spinning
            TransferAndStartService implements Runnable should:
            * Start the TransferClient, await its completion.
            * Start the DockerController
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

    private void sendServiceResponse(ServiceRequest request, InetSocketAddress transferServerAddress) {
        ServiceResponse response = new ServiceResponse(
                request.getTargetNodeUuid(), uuid, transferServerAddress, request.getDesiredServiceName());
        sendAsJson(response);
    }

    /**
     * This method will launch the host server that will allow users to communicate with the docker instance
     * <p>
     * todo delete
     */
    private void launchServiceOnDockerController() throws UnknownHostException {
        ServiceHost serviceHost = new ServiceHost(serviceAddress.getPort(), dockerController);

        logger.info("Starting the serviceHost");
        serviceHost.start();
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        logger.warn("Closing {}", label);
    }

    @Override
    public void onError(Exception ex) {
        logger.error("Error in " + label, ex);
    }
}
