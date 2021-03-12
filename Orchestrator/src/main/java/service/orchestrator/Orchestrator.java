package service.orchestrator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.core.*;
import service.orchestrator.clients.MobileClient;
import service.orchestrator.clients.MobileClientRegistry;
import service.orchestrator.exceptions.NoSuchNodeException;
import service.orchestrator.migration.Migrator;
import service.orchestrator.nodes.ServiceNode;
import service.orchestrator.nodes.ServiceNodeRegistry;
import service.orchestrator.properties.OrchestratorProperties;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

// todo remove hard coded values and use a config file instead (or command line args)

public class Orchestrator extends WebSocketServer implements Migrator {
    public static final int PING_SERVER_PORTNUMBER = OrchestratorProperties.get().getClientPingServerPort();
    private static final Logger logger = LoggerFactory.getLogger(Orchestrator.class);
    private static final long HEARTBEAT_REQUEST_PERIOD = 10L * 1000L;
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final ServiceNodeRegistry serviceNodeRegistry = ServiceNodeRegistry.get();
    private static final MobileClientRegistry mobileClientRegistry = MobileClientRegistry.get();

    // todo consider removing this throughout the framework
    private static final String serviceName = "docker.tar";

    private Map<UUID, InetAddress> newWSClientAddresses = new HashMap<>();
    private Gson gson;

    public Orchestrator(int port) {
        super(new InetSocketAddress(port));
        initializeGson();

        // todo extract this to a method
        new Timer().schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        logger.debug("Current connections:");
                        for (ServiceNode node : serviceNodeRegistry.getServiceNodes()) {
                            logger.debug(node.toString());
                        }
                        for (MobileClient mobile : mobileClientRegistry.getMobileClients()) {
                            logger.debug(mobile.toString());
                        }
                        logger.debug("End Current Connections.");

                        logger.debug("Sending HeartbeatRequests");
                        for (ServiceNode node : serviceNodeRegistry.getServiceNodes()) {
                            ServerHeartbeatRequest heartbeat = new ServerHeartbeatRequest(node.uuid);
                            sendAsJson(node.webSocket, heartbeat);
                        }
                        for (MobileClient mobile : mobileClientRegistry.getMobileClients()) {
                            ServerHeartbeatRequest heartbeat = new ServerHeartbeatRequest(mobile.uuid);
                            sendAsJson(mobile.webSocket, heartbeat);
                        }
                    }
                }, HEARTBEAT_REQUEST_PERIOD, HEARTBEAT_REQUEST_PERIOD);
    }

    private static URI mapToUri(InetAddress address) {
        String uriString = String.format("ws://%s:%d", address.getHostAddress(), PING_SERVER_PORTNUMBER);
        logger.debug("Mapping {} to URI.", uriString);
        return URI.create(uriString);
    }

    private static InetAddress getClientAddress(WebSocket webSocket, ClientHandshake handshake) {
        String xForwardedFor = handshake.getFieldValue(X_FORWARDED_FOR);
        if (!xForwardedFor.isEmpty()) {
            return mapStringToAddress(xForwardedFor);
        }
        return webSocket.getRemoteSocketAddress().getAddress();
    }

    private static InetAddress mapStringToAddress(String addressString) {
        InetAddress webSocketAddress = null;
        try {
            webSocketAddress = InetAddress.getByName(addressString);
        } catch (UnknownHostException e) {
            logger.error("Unknown host at {} ; message: {}", addressString, e.getMessage());
        }
        return webSocketAddress;
    }

    private void initializeGson() {
        RuntimeTypeAdapterFactory<Message> adapter = RuntimeTypeAdapterFactory
                .of(Message.class, "type")
                .registerSubtype(NodeInfo.class, Message.MessageTypes.NODE_INFO)
                .registerSubtype(ServiceRequest.class, Message.MessageTypes.SERVICE_REQUEST)
                .registerSubtype(ServiceResponse.class, Message.MessageTypes.SERVICE_RESPONSE)
                .registerSubtype(HostRequest.class, Message.MessageTypes.HOST_REQUEST)
                .registerSubtype(NodeInfoRequest.class, Message.MessageTypes.NODE_INFO_REQUEST)
                .registerSubtype(MigrationSuccess.class, Message.MessageTypes.MIGRATION_SUCCESS)
                .registerSubtype(NodeClientLatencyResponse.class, Message.MessageTypes.NODE_CLIENT_LATENCY_RESPONSE)
                .registerSubtype(MobileClientInfo.class, Message.MessageTypes.MOBILE_CLIENT_INFO);
        gson = new GsonBuilder().setPrettyPrinting().registerTypeAdapterFactory(adapter).create();
    }

    /*
    Note the somewhat complicated 'dance' here.
    WebSocketClients connect to this Server, but we don't know whether they are ServiceNodes or MobileClients

    We also need to associate their IP address with them, either from the webSocket object or via the X-Forwarded-For
    header in the clientHandshake.

    Therefore we keep the client's address in newWSClientAddress until it is removed when a NodeInfo or
    MobileClientInfo message is received.
     */
    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        logger.info("new connection :" + webSocket.getRemoteSocketAddress());
        logger.debug("Http Headers:");
        for (Iterator<String> it = clientHandshake.iterateHttpFields(); it.hasNext(); ) {
            String field = it.next();
            logger.debug("{} : {}", field, clientHandshake.getFieldValue(field));
        }
        logger.debug("END Http Headers.");

        UUID UUIDToReturn = UUID.randomUUID();

        // cache client addresses for safekeeping
        InetAddress newWSClientAddress = getClientAddress(webSocket, clientHandshake);
        newWSClientAddresses.put(UUIDToReturn, newWSClientAddress);
        logger.debug("Keeping UUID and WSocketAddress: {} {}", UUIDToReturn, newWSClientAddress);

        // create a nodeInfoRequest and send it back to the node
        NodeInfoRequest infoRequest = new NodeInfoRequest(UUIDToReturn);
        sendAsJson(webSocket, infoRequest);
    }

    @Override
    public void onMessage(WebSocket webSocket, String message) {
        logger.debug("from {}", webSocket.getRemoteSocketAddress());

        final int MAX_MESSAGE_LEN = 130;
        logger.debug(message.substring(0, Integer.min(message.length(), MAX_MESSAGE_LEN)));

        Message messageObj = gson.fromJson(message, Message.class);

        // todo a good way to cut this down would be to:
        //  map messages to commands (separate switch in a separate class)
        //  execute commands polymorphically
        //this routes inbound messages based on type and then moves them to other methods
        switch (messageObj.getType()) {
            case Message.MessageTypes.NODE_INFO:
                registerServiceNode((NodeInfo) messageObj, webSocket);
                break;
            case Message.MessageTypes.MOBILE_CLIENT_INFO:
                handleMobileClientInfo((MobileClientInfo) messageObj, webSocket);
                break;
            case Message.MessageTypes.SERVICE_RESPONSE:
                handleServiceResponse((ServiceResponse) messageObj);
                break;
            case Message.MessageTypes.MIGRATION_SUCCESS:
                handleMigrationSuccess((MigrationSuccess) messageObj);
                break;
            case Message.MessageTypes.HOST_REQUEST:
                logger.warn("Not calling transferServiceToBestNode. Fix this asap.");
                handleHostRequest((HostRequest) messageObj);
                break;
            case Message.MessageTypes.NODE_CLIENT_LATENCY_RESPONSE:
                // todo get rid of this handler once NodeClientLatencyResponse is deleted
                NodeClientLatencyResponse nclr = (NodeClientLatencyResponse) messageObj;
                logger.error("Received a NodeClientLatencyResponse from {}", nclr.getNodeId());
                break;
            default:
                logger.error("Message received with unrecognised type: {}", messageObj.getType());
                break;
        }
    }

    private void handleHostRequest(HostRequest request) {
        MobileClient requestor = mobileClientRegistry.get(request.getRequestorID());
        ServiceNode bestNode = getBestServiceForClient(requestor);
        HostResponse response = new HostResponse(request.getRequestorID(), bestNode.serviceHostAddress);

        sendAsJson(requestor.webSocket, response);
    }

    // todo should be done by the selector
    private ServiceNode getBestServiceForClient(MobileClient mobileClient) {
        return serviceNodeRegistry.getServiceNodes().stream().findAny().orElse(null);
    }

    // Removes Address from newWSClientAddresses, used by the Orchestrator to track *MobileClients* (not ServiceNodes).
    //  FYI: called every time a NodeInfo is received.
    private void registerServiceNode(NodeInfo nodeInfoMsg, WebSocket nodeWebSocket) {
        nodeInfoMsg.setWebSocket(nodeWebSocket);
        newWSClientAddresses.remove(nodeInfoMsg.getUuid());
        serviceNodeRegistry.updateNode(nodeInfoMsg);

        askNodeToTrackAllLatencies(serviceNodeRegistry.get(nodeInfoMsg.getUuid()));
    }

    private void askNodeToTrackAllLatencies(ServiceNode serviceNode) {
        Collection<MobileClient> mobileClients = mobileClientRegistry.getMobileClients();
        NodeClientLatencyRequest request;

        for (MobileClient client : mobileClients) {
            URI clientPingServer = mapToUri(client.pingServer);
            request = new NodeClientLatencyRequest(serviceNode.uuid, client.uuid, clientPingServer);
            sendAsJson(serviceNode.webSocket, request);
        }
    }

    private void handleMobileClientInfo(MobileClientInfo mobileClientInfo, WebSocket webSocket) {
        boolean isNewClient = newWSClientAddresses.containsKey(mobileClientInfo.getUuid());

        if (isNewClient) {
            registerNewMobileClient(mobileClientInfo, webSocket);
        } else {
            updateExistingMobileClient(mobileClientInfo);
        }
    }

    private void registerNewMobileClient(MobileClientInfo mobileClientInfo, WebSocket webSocket) {
        InetAddress address = newWSClientAddresses.remove(mobileClientInfo.getUuid());
        mobileClientInfo.setPingServer(address);
        mobileClientInfo.setWebSocket(webSocket);
        mobileClientRegistry.updateClient(mobileClientInfo);
    }

    private void updateExistingMobileClient(MobileClientInfo mobileClientInfo) {
        mobileClientRegistry.updateClient(mobileClientInfo);
    }

    // Routes a ServiceResponse from a source ServiceNode to a target ServiceNode.
    // todo make sure this works
    private void handleServiceResponse(ServiceResponse response) {
        UUID targetUuid = response.getTargetNodeUuid();
        WebSocket returnSocket = serviceNodeRegistry.get(targetUuid).webSocket;
        sendAsJson(returnSocket, response);
    }

    // updates the service statuses of the ServiceNodes after migration.
    private void handleMigrationSuccess(MigrationSuccess migrationSuccess) {
        UUID migrationSourceUuid = migrationSuccess.getSourceHostUuid();
        serviceNodeRegistry.get(migrationSourceUuid).serviceName = null;

        UUID migrationTargetUuid = migrationSuccess.getTargetHostUuid();
        serviceNodeRegistry.get(migrationTargetUuid).serviceName = migrationSuccess.getServiceName();
    }

    /**
     * Converts the given message to JSON, and sends that JSON String along the given WebSocket.
     */
    private void sendAsJson(WebSocket ws, Message message) {
        String json = gson.toJson(message);
        logger.debug("Sending: {}", json);
        ws.send(json);
    }

//    /**
//     * This method transfers the requested service to the best node available,
//     * but for testing purposes states that it can't stay on the cloud where it currently is
//     *
//     * @param serviceRequest the requested service
//     * @return the NodeInfo for the node that was deemed best
//     */
//    public NodeInfo transferServiceToEdgeNode(ServiceRequest serviceRequest) {
//        Map<UUID, NodeInfo> connectedNodesWithoutHosts = connectedNodes;
//        NodeInfo worstCurrentOwner = findWorstServiceOwner(serviceRequest);
//        connectedNodesWithoutHosts.remove(worstCurrentOwner.getSystemID());
//        NodeInfo bestNode = findBestNode(connectedNodesWithoutHosts);
//        ServiceRequest request = new ServiceRequest(bestNode.getSystemID(), serviceRequest.getServiceName());
//        Gson gson = new Gson();
//        String jsonStr = gson.toJson(request);
//        worstCurrentOwner.getWebSocket().send(jsonStr);//this tells the current worst owner of a service that its relived of duty and can send away its service
//        System.out.println("Told worst current owner to transfer the file to best Node  --TIME " + System.currentTimeMillis());
//        return bestNode;
//    }

    /**
     * This method transfers the requested service to the best node available,
     * if that node is the current host, no transfer occurs
     * <p>
     * // todo fix this: Trigger should be able to call this? Or Selector..
     *
     * @param serviceRequest the requested service
     * @return the NodeInfo for the node that was deemed best
     */
    public ServiceNode transferServiceToBestNode(ServiceRequest serviceRequest) {
        ServiceNode dormantServiceNode = anyDormantServiceNode();
        ServiceNode worstCurrentOwner = findWorstServiceOwner(serviceRequest);

        if (isNull(dormantServiceNode) || isNull(worstCurrentOwner)) {
            logger.warn("One of the following is null. No transfer made." +
                    "\ndormantServiceNode={}\nworstCurrentOwner={}", dormantServiceNode, worstCurrentOwner);
            return null;
        }

        if (worstCurrentOwner.uuid.equals(dormantServiceNode.uuid)) {
            return dormantServiceNode;
        }

        // tells the worstCurrentOwner to send its service to the dormantServiceNode
        ServiceRequest request = new ServiceRequest(dormantServiceNode.uuid, serviceRequest.getDesiredServiceName());
        sendAsJson(worstCurrentOwner.webSocket, request);

        return dormantServiceNode;
    }

    // Currently returns any ServiceNode running the service.
    public ServiceNode findWorstServiceOwner(ServiceRequest serviceRequest) {
        // todo remove this method when reimplementing the Selector

        // let the worst ServiceOwner be any Node running the service
        List<ServiceNode> nodes = new ArrayList<>(findAllServiceOwners(serviceRequest).values());
        return nodes.size() > 0 ? nodes.get(0) : null;
    }

    /**
     * This method finds the best owner of a service, for a MobileClient.
     *
     * @param serviceRequest
     * @return the nodeInfo of the best owner
     */
    public ServiceNode findBestServiceOwner(ServiceRequest serviceRequest) {
        ServiceNode bestNode = null;
        Map<UUID, ServiceNode> allServiceOwnerAddresses = findAllServiceOwners(serviceRequest);
        if (allServiceOwnerAddresses == null) {
            //noone had this service, inform edgenode
        } else {
            bestNode = anyDormantServiceNode();
        }
        return bestNode;
    }

    /**
     * This method finds every node that is currently hosting a service specified by the given {@code ServiceRequest}.
     */
    public Map<UUID, ServiceNode> findAllServiceOwners(ServiceRequest serviceRequest) {
        Collection<ServiceNode> serviceNodes = serviceNodeRegistry.getServiceNodes();
        String desiredServiceName = serviceRequest.getDesiredServiceName();
        return serviceNodes.stream()
                .filter(node -> nonNull(node.serviceName))
                .filter(node -> node.serviceName.equals(desiredServiceName))
                .collect(Collectors.toMap(node -> node.uuid, Function.identity()));
    }

    // finds any Node that is not hosting a service (is Dormant)
    private ServiceNode anyDormantServiceNode() {
        ServiceNode bestNode = null;
        try {
            bestNode = serviceNodeRegistry.getServiceNodes().stream()
                    .filter(node -> isNull(node.serviceName))
                    .findAny()
                    .orElseThrow(NoSuchNodeException::new);
        } catch (NoSuchNodeException nsne) {
            logger.warn("Could not find an unoccupied node ");
        }
        return bestNode;
    }

    /**
     * Takes in a map of cpuload and the current rolling score for CPU
     *
     * @param entryCPULoad
     * @param rollingCPUScore
     * @return the adjusted new rolling average
     */
    public double calculateRecentCPULoad(Map<Integer, Double> entryCPULoad, double rollingCPUScore) {
        double runningTotal = -1;
        if (entryCPULoad.size() >= 5) {
            for (int i = entryCPULoad.size() - 1; i > entryCPULoad.size() - 5; i--) {
                runningTotal = +entryCPULoad.get(i);
            }
        }

        return -1; // ((1 - rollingAverage) * rollingCPUScore) + (rollingAverage * (runningTotal / 5));
    }

    /**
     * Takes in a map of cpuload and the current rolling score for CPU
     *
     * @param entryRamLoad
     * @param rollingRamScore
     * @return the adjusted new rolling average
     */
    public double calculateRecentRamLoad(Map<Integer, Double> entryRamLoad, double rollingRamScore) {
        double runningTotal = -1;
        if (entryRamLoad.size() >= 5) {
            for (int i = entryRamLoad.size() - 1; i > entryRamLoad.size() - 5; i--) {
                runningTotal = +entryRamLoad.get(i);
            }
        }
        return -1; //((1 - rollingAverage) * rollingRamScore) + (rollingAverage * (runningTotal / 5));
    }

    @Override
    public void migrate(ServiceNode source, ServiceNode target) {
        logger.info("--- In Orchestrator.migrate ---");
        ServiceRequest request = new ServiceRequest(target.uuid, serviceName);
        sendAsJson(source.webSocket, request);
        logger.info("--- End Orchestrator.migrate ---");
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        System.out.println("Error in the Orchestrator:");
        e.printStackTrace();
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        logger.info("Closing a connection to {}", webSocket.getRemoteSocketAddress());
        logger.debug("Reason: {}", s);

        serviceNodeRegistry.removeNodeWithWebsocket(webSocket);
        mobileClientRegistry.removeClientWithWebsocket(webSocket);
    }
}
