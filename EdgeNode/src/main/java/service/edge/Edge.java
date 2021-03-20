package service.edge;

import org.java_websocket.handshake.ServerHandshake;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import service.core.*;
import service.host.ServiceHost;
import service.node.ServiceNodeMecClient;
import service.transfer.DockerController;
import service.transfer.SecureTransferServer;
import service.transfer.TransferClient;
import service.transfer.TransferServer;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Edge extends ServiceNodeMecClient {
    SystemInfo nodeSystem = new SystemInfo();
    HardwareAbstractionLayer hal = nodeSystem.getHardware();
    DockerController dockerController;
    private boolean trustWorthyNode, secureMode;
    private URI serviceAddress;
    private File service;   // todo remove
    private UUID assignedUUID;
    private Map<Integer, Double> historicalCPUload = new HashMap<>();
    private Map<Integer, Double> historicalRamload = new HashMap<>();

    public Edge(URI serverUri, boolean trustWorthy, URI serviceAddress, boolean secure) {
        super(serverUri);
        dockerController = new DockerController();
        this.serviceAddress = serviceAddress;
        trustWorthyNode = trustWorthy;
        secureMode = secure;
        getSystemLoad();
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        logger.info("Listening to orchestrator at {} on local address {}",
                getRemoteSocketAddress(), getLocalSocketAddress());
    }

    /**
     * When the websocket library receives any messages they are routed to this method
     *
     * @param message   the message received
     */
    @Override
    public void onMessage(String message) {
        Message messageObj = gson.fromJson(message, Message.class);

        logger.info(message);

        //this routes inbound messages based on type and then moves them to other methods
        switch (messageObj.getType()) {
            case Message.MessageTypes.NODE_INFO_REQUEST:
                NodeInfoRequest infoRequest = (NodeInfoRequest) messageObj;
                assignedUUID = infoRequest.getAssignedUUID();
                sendHeartbeatResponse();
                break;
            case Message.MessageTypes.SERVER_HEARTBEAT_REQUEST:
                sendHeartbeatResponse();
                break;
            case Message.MessageTypes.SERVICE_REQUEST:
                // todo this is untested - should be fixed once Cloud and Edge are unified
                ServiceRequest serviceRequest = (ServiceRequest) messageObj;
                InetSocketAddress serverAddress;
                try {
                    serverAddress = launchTransferServer();
                    ServiceResponse serviceResponse = new ServiceResponse(serviceRequest.getTargetNodeUuid(), assignedUUID, serverAddress, serviceRequest.getDesiredServiceName());
                    String jsonStr = gson.toJson(serviceResponse);
                    send(jsonStr);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case Message.MessageTypes.SERVICE_RESPONSE:
                handleServiceResponse((ServiceResponse) messageObj);
                break;
        }
    }

    private void handleServiceResponse(ServiceResponse response) {
        logger.info("Edge received a service response");
        try {
            launchTransferClient(response.getTransferServerAddress());
            MigrationSuccess migrationSuccess = new MigrationSuccess(assignedUUID, response.getSourceNodeUuid(), response.getServiceName());
            sendAsJson(migrationSuccess);
        } catch (URISyntaxException | UnknownHostException e) {
            e.printStackTrace();
        }
    }

    /**
     * Constructs and sends Heartbeat responses when called
     */
    private void sendHeartbeatResponse() {
        NodeInfo nodeInfo = new NodeInfo(assignedUUID, null, null);
        nodeInfo.setServiceHostAddress(serviceAddress);
        nodeInfo.setTrustworthy(trustWorthyNode);
        if (!historicalCPUload.isEmpty()) {
            nodeInfo.setCPUload(historicalCPUload);
        }
        if (!historicalRamload.isEmpty()) {
            nodeInfo.setRamLoad(historicalRamload);
        }
        String jsonStr = gson.toJson(nodeInfo);
        send(jsonStr);
    }

    /**
     * This method launches this nodes Transfer Server using the service address define at node creation
     *
     * @return the InetSocketAddress of the new temp server
     */
    private InetSocketAddress launchTransferServer() throws Exception {
        InetSocketAddress serverAddress = new InetSocketAddress(serviceAddress.getPort());
        setReuseAddr(true);
        if (!secureMode) {
            TransferServer transferServer = new TransferServer(serverAddress, service);
            transferServer.start();
        } else {
            new SecureTransferServer(serverAddress, service);
        }

        return serverAddress;
    }

    /**
     * This method creates and launches the TransferClient for this client,
     * If this node is in secure mode then the TransferClient will also be in secure mode
     * After a successful connection this method will start the launch process for the new service.
     *
     * @param serverAddress The address of the TransferServer that is trying to be connected to
     * @throws URISyntaxException
     * @throws UnknownHostException
     */
    private void launchTransferClient(InetSocketAddress serverAddress) throws URISyntaxException, UnknownHostException {
        URI transferServerURI;
        String uriString = String.format("ws://%s:%d", serverAddress.getHostString(), serverAddress.getPort());

        if (secureMode) {
            logger.warn("Bug here: server address has a leading forward slash.");
            transferServerURI = URI.create("wss://" + serverAddress);
        } else {
            transferServerURI = URI.create(uriString);
        }

        TransferClient transferClient = new TransferClient(transferServerURI, dockerController);
        transferClient.connect();

        // todo use a new thread instead of spinning
        while (transferClient.dockerControllerReady() == null) {
        }
        // todo the method above does not make sure docker was launched. Fix it
        // todo FIXME sometimes blocks here

        logger.info("The transfer client says Docker was launched.");
        DockerController dockerController = transferClient.dockerControllerReady();
        transferClient.close();
        logger.info("Closed the TransferClient and launching the service on Docker");
        launchServiceOnDockerController(dockerController);
    }

    /**
     * This method will launch the host server that will allow users to communicate with the docker instance
     *
     * @param dockerController takes in the dockerController which has the service information
     * @throws UnknownHostException
     */
    private void launchServiceOnDockerController(DockerController dockerController) throws UnknownHostException {
        ServiceHost serviceHost = new ServiceHost(serviceAddress.getPort(), dockerController);

        logger.info("Starting the serviceHost");
        serviceHost.start();
    }

    /**
     * This method polls the system every second and stores pecentage values for CPU and Ram Usage
     * <p>
     * todo implement these methods again in the new Orchestrator
     */
    private void getSystemLoad() {
//        new Timer().schedule(
//                new TimerTask() {
//                    int secondCounter = 0;
//
//                    @Override
//                    public void run() {
//                        secondCounter++;
//                        historicalCPUload.put(secondCounter, hal.getProcessor().getSystemCpuLoadBetweenTicks() * 100);
//                        historicalRamload.put(secondCounter, (double) ((hal.getMemory().getAvailable() / hal.getMemory().getTotal()) * 100));
//                    }
//                }, 0, 1000);
    }

    @Override
    public void onClose(int i, String s, boolean b) {
    }

    @Override
    public void onError(Exception e) {
    }
}
