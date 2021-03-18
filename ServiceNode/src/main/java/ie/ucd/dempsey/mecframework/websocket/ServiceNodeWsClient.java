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
import service.transfer.DockerController;
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
     * todo fill in
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

    @Override
    public void onMessage(String message) {
        logger.debug("Received: {}", message);

        Message messageObj = gson.fromJson(message, Message.class);
        //this routes inbound messages based on type and then moves them to other methods
        switch (messageObj.getType()) {
            //A request for the nodes status when it initially joins
            case Message.MessageTypes.NODE_INFO_REQUEST:
                NodeInfoRequest infoRequest = (NodeInfoRequest) messageObj;
                uuid = infoRequest.getAssignedUUID();
                sendHeartbeatResponse();
                break;
            //heartbeat request
            case Message.MessageTypes.SERVER_HEARTBEAT_REQUEST:
                sendHeartbeatResponse();
                break;
            //request for the service on the node
            case Message.MessageTypes.SERVICE_REQUEST:
                // transfers service from this node to the target
                handleServiceRequest((ServiceRequest) messageObj);
                break;
            case Message.MessageTypes.SERVICE_RESPONSE:
                // * Service gets transferred to this Node here *

                ServiceResponse response = (ServiceResponse) messageObj;

                // todo figure out whether the CloudNode ever receives a ServiceResponse
                logger.warn("The CloudNode received a ServiceResponse!");

                try {
                    // Inverse of what happens when a ServiceRequest message is received
                    //
                    launchTransferClient(response.getTransferServerAddress());
                    MigrationSuccess migrationSuccess = new MigrationSuccess(uuid, response.getSourceNodeUuid(), response.getServiceName());
                    sendAsJson(migrationSuccess);
                } catch (URISyntaxException | UnknownHostException e) {
                    e.printStackTrace();
                }
                break;
            case Message.MessageTypes.NODE_CLIENT_LATENCY_REQUEST:
                latencyRequestor.registerRequest((NodeClientLatencyRequest) messageObj);
                break;
            default:
                logger.error("Message received with unrecognised type: {}", messageObj.getType());
                break;
        }
    }


    private void sendHeartbeatResponse() {
        throw new UnsupportedOperationException();
    }

    private void handleServiceRequest(ServiceRequest request) {
        throw new UnsupportedOperationException();
    }

    private void launchTransferClient(InetSocketAddress transferServerAddress) throws URISyntaxException, UnknownHostException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {

    }

    @Override
    public void onError(Exception ex) {

    }
}
