package service.cloud;

import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import service.cloud.connections.LatencyRequestMonitor;
import service.core.*;
import service.host.ServiceHost;
import service.node.AbstractServiceNode;
import service.transfer.DockerController;
import service.transfer.SecureTransferServer;
import service.transfer.TransferClient;
import service.transfer.TransferServer;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.*;

// todo fix all access modifiers
public class Cloud extends AbstractServiceNode {
    private static final Logger logger = LoggerFactory.getLogger(Cloud.class);
    private final LatencyRequestMonitor latencyRequestMonitor = new LatencyRequestMonitor(this);
    SystemInfo nodeSystem = new SystemInfo();
    HardwareAbstractionLayer hal = nodeSystem.getHardware();
    OperatingSystem os = nodeSystem.getOperatingSystem();
    DockerController dockerController;
    boolean secureMode;
    private File service;
    private UUID assignedUUID;
    private URI serviceAddress;
    private Map<Integer, Double> historicalCPUload = new HashMap<>();
    private Map<Integer, Double> historicalRamload = new HashMap<>();
    private Map<Integer, Long> unusedStorage = new HashMap<>();
    private long[] ticks;

    public Cloud(URI serverUri, File service, URI serviceAddress, Boolean secureMode) {
        super(serverUri);
        this.service = service;
        dockerController = new DockerController();
        this.serviceAddress = serviceAddress;
        this.secureMode = secureMode;
        getSystemLoad();
        startLatencyRequestMonitor();
    }

    private void startLatencyRequestMonitor() {
        logger.debug("Starting the LatencyRequestMonitor");
        new Thread(latencyRequestMonitor, "LatencyRequestMonitorThread").start();
    }

    /**
     * When the websocket library receives any messages they are routed to this method
     *
     * @param message the message received
     */
    @Override
    public void onMessage(String message) {
        logger.debug("from {}", getRemoteSocketAddress());
        logger.debug(message);

        Message messageObj = gson.fromJson(message, Message.class);

        //this routes inbound messages based on type and then moves them to other methods
        switch (messageObj.getType()) {
            //A request for the nodes status when it initially joins
            case Message.MessageTypes.NODE_INFO_REQUEST:
                NodeInfoRequest infoRequest = (NodeInfoRequest) messageObj;
                assignedUUID = infoRequest.getAssignedUUID();
                sendHeartbeatResponse();
                break;
            //heartbeat request
            case Message.MessageTypes.SERVER_HEARTBEAT_REQUEST:
                sendHeartbeatResponse();
                break;
            //request for the service on the node
            case Message.MessageTypes.SERVICE_REQUEST:
                // * Service gets transferred from this Node here *

                ServiceRequest serviceRequest = (ServiceRequest) messageObj;

                UUID clientRequesterId = serviceRequest.getRequesterId();
                String serviceOwnerAddress = serviceAddress.getHost() + ":" + serviceAddress.getPort();
                String serviceName = serviceRequest.getServiceName();

                try {
                    launchTransferServer();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // ServiceResponse allows the Orchestrator to respond to the client with information about the
                //  migration
                //
                ServiceResponse serviceResponse =
                        new ServiceResponse(clientRequesterId, assignedUUID, serviceOwnerAddress, serviceName);
                sendAsJson(serviceResponse);
                break;
            case Message.MessageTypes.SERVICE_RESPONSE:
                // * Service gets transferred to this Node here *

                ServiceResponse response = (ServiceResponse) messageObj;

                // todo figure out whether the CloudNode ever receives a ServiceResponse
                logger.warn("The CloudNode received a ServiceResponse!");

                try {
                    // Inverse of what happens when a ServiceRequest message is received
                    //
                    launchTransferClient(response.getServiceOwnerAddress());
                    MigrationSuccess migrationSuccess = new MigrationSuccess(assignedUUID, response.getServiceOwnerID(), response.getServiceName());
                    sendAsJson(migrationSuccess);
                } catch (URISyntaxException | UnknownHostException e) {
                    e.printStackTrace();
                }
                break;
            case Message.MessageTypes.NODE_CLIENT_LATENCY_REQUEST:
                NodeClientLatencyRequest nclRequest = (NodeClientLatencyRequest) messageObj;

                // todo here: requests should have clouds continually checking MobileClients
                //  i.e. when a request is received, it should be sent to the 'LatencyChecker'
                //      which should startLatencyRequests every so often
                latencyRequestMonitor.startLatencyRequest(nclRequest);
                break;
            default:
                logger.error("Message received with unrecognised type: {}", messageObj.getType());
                break;
        }
    }

    /**
     * Converts the given message to JSON, and sends that JSON String along the given WebSocket.
     */
    public void sendAsJson(Message message) {
        String json = gson.toJson(message);
        logger.debug("Sending: {}", json);
        send(json);
    }

    /**
     * Constructs and sends Heartbeat responses when called
     */
    public void sendHeartbeatResponse() {
        NodeInfo nodeInfo = new NodeInfo(assignedUUID, null, service.getName());
        nodeInfo.setServiceHostAddress(serviceAddress);

        // adding performance data
        if (!historicalCPUload.isEmpty()) {
            nodeInfo.setCPUload(historicalCPUload);
        }
        if (!historicalRamload.isEmpty()) {
            nodeInfo.setRamLoad(historicalRamload);
        }
        if (!unusedStorage.isEmpty()) {
            nodeInfo.setUnusedStorage(unusedStorage);
        }
        // END adding performance data

        sendAsJson(nodeInfo);
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
    private void launchTransferClient(String serverAddress) throws URISyntaxException, UnknownHostException {
        URI transferServerURI;
        if (secureMode) {
            transferServerURI = new URI("wss://" + serverAddress);
        } else {
            transferServerURI = new URI("ws://" + serverAddress);
        }

        TransferClient transferClient = new TransferClient(transferServerURI, dockerController);
        transferClient.connect();

        logger.info("Checking if the dockerController is ready at {}", Instant.now());
        while (transferClient.dockerControllerReady() == null) {
        }
        logger.info("dockerController ready at {}", Instant.now());
        transferClient.close();

        // todo this is the only place where the service gets launched.
        //  Ideally the CloudNode would start the service before it has to be migrated
        launchServiceOnDockerController(dockerController);
    }

    /**
     * This method will launch the host server that will allow users to communicate with the docker instance
     *
     * @param dockerController takes in the dockerController which has the service information
     * @throws UnknownHostException
     */
    private void launchServiceOnDockerController(DockerController dockerController) throws UnknownHostException {
        String[] array = serviceAddress.toString().split(":");
        ServiceHost serviceHost = new ServiceHost(Integer.parseInt(array[5]), dockerController);

        logger.info("Asking the serviceHost to run. This thread should block here?");
        serviceHost.run();
    }

    /**
     * This method polls the system every second and stores pecentage values for CPU and Ram Usage
     * <p>
     * todo extract this out to a separate class
     */
    private void getSystemLoad() {
        ticks = hal.getProcessor().getSystemCpuLoadTicks();

        new Timer().schedule(
                new TimerTask() {
                    int secondCounter = 0;

                    @Override
                    public void run() {
                        secondCounter++;
                        historicalCPUload.put(secondCounter, hal.getProcessor().getSystemCpuLoadBetweenTicks(ticks) * 100);

                        double totalMemory = hal.getMemory().getTotal();
                        double availableMemory = hal.getMemory().getAvailable();
                        double fractionMemoryUsed = 1.0 - (availableMemory / totalMemory);
                        historicalRamload.put(secondCounter, fractionMemoryUsed);

                        long usableSpace = os.getFileSystem().getFileStores().stream()
                                .mapToLong(OSFileStore::getUsableSpace)
                                .sum();
                        unusedStorage.put(secondCounter, usableSpace);
                        ticks = hal.getProcessor().getSystemCpuLoadTicks();
                    }
                }, 0, 1000);
    }

    /**
     * This method launches this nodes Transfer Server using the service address define at node creation
     */
    private void launchTransferServer() throws Exception {
        InetSocketAddress serverAddress = new InetSocketAddress(serviceAddress.getPort());
        setReuseAddr(true);

        logger.debug("serverAddress={}", serverAddress);
        if (!secureMode) {
            TransferServer transferServer = new TransferServer(serverAddress, service);
            transferServer.start();
        } else {
            new SecureTransferServer(serverAddress, service);
        }

    }

    @Override
    public void onClose(int i, String s, boolean b) {
    }

    @Override
    public void onError(Exception e) {
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        System.out.println("connected to orchestrator");
        System.out.println(Cloud.this.getLocalSocketAddress());//this is the local address in theory
    }
}
