package service.orchestrator;

import com.google.gson.Gson;
import ie.ucd.mecframework.messages.service.StartServiceRequest;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.core.*;
import service.orchestrator.clients.MobileClient;
import service.orchestrator.clients.MobileClientRegistry;
import service.orchestrator.migration.Migrator;
import service.orchestrator.migration.Selector;
import service.orchestrator.nodes.ServiceNode;
import service.orchestrator.nodes.ServiceNodeRegistry;
import service.orchestrator.properties.OrchestratorProperties;
import service.util.Gsons;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * The Orchestrator's WebSocketServer.
 *
 * <br>This server sends a heartbeat to all clients every few seconds, as specified in orchestrator.properties.
 */
public class Orchestrator extends WebSocketServer implements Migrator {
    private static final Logger logger = LoggerFactory.getLogger(Orchestrator.class);
    private static final long HEARTBEAT_REQUEST_PERIOD = OrchestratorProperties.get().getHeartbeatPeriod();
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final ServiceNodeRegistry serviceNodeRegistry = ServiceNodeRegistry.get();
    private static final MobileClientRegistry mobileClientRegistry = MobileClientRegistry.get();
    private static final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
    private static final ServiceNode NULL_SERVICE_NODE =
            new ServiceNode(UUID.fromString("00000000-0000-0000-0000-000000000000"), null);

    // todo remove this from the framework
    private static final String serviceName = "stream";

    private final Selector selector;
    private Map<UUID, InetAddress> newWSClientAddresses = new Hashtable<>();
    private Gson gson;

    public Orchestrator(int port, Selector selector) {
        super(new InetSocketAddress(port));
        this.selector = selector;
        gson = Gsons.orchestratorGson();
        heartbeatScheduler.scheduleAtFixedRate(
                this::broadcastHeartbeats, HEARTBEAT_REQUEST_PERIOD, HEARTBEAT_REQUEST_PERIOD, TimeUnit.SECONDS);
    }

    private static URI mapToUri(InetSocketAddress address) {
        String uriString = String.format("ws://%s:%d", address.getHostString(), address.getPort());
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
        logger.info("X-Forwarded-For : " + clientHandshake.getFieldValue(X_FORWARDED_FOR));
        UUID newClientUuid = UUID.randomUUID();

        // cache client addresses for safekeeping
        InetAddress newWSClientAddress = getClientAddress(webSocket, clientHandshake);
        newWSClientAddresses.put(newClientUuid, newWSClientAddress);
        logger.debug("Client {} has address {}", newClientUuid, newWSClientAddress);
        sendAsJson(webSocket, new NodeInfoRequest(newClientUuid));
    }

    @Override
    public void onMessage(WebSocket webSocket, String message) {
        Message messageObj = gson.fromJson(message, Message.class);
        logger.debug("Received: {}", messageObj);
        switch (messageObj.getType()) {
            case Message.MessageTypes.NODE_INFO:
                registerServiceNode((NodeInfo) messageObj, webSocket);
                break;
            case Message.MessageTypes.MOBILE_CLIENT_INFO:
                registerMobileClient((MobileClientInfo) messageObj, webSocket);
                break;
            case Message.MessageTypes.SERVICE_RESPONSE:
                handleServiceResponse((ServiceResponse) messageObj);
                break;
            case Message.MessageTypes.MIGRATION_SUCCESS:
                handleMigrationSuccess((MigrationSuccess) messageObj);
                break;
            case Message.MessageTypes.HOST_REQUEST:
                handleHostRequest((HostRequest) messageObj);
                break;
            case Message.MessageTypes.START_SERVICE_RESPONSE:
                System.out.println(messageObj);
                break;
            default:
                logger.error("Message received with unrecognised type: {}", messageObj.getType());
                break;
        }
    }

    // todo take the port number of the node's serviceAddress and the IP address of the node itself
    private void handleHostRequest(HostRequest request) {
        MobileClient requestor = mobileClientRegistry.get(request.getRequestorID());
        if (isNull(requestor)) {
            logger.info("Received a HostRequest from an unregistered MobileClient. Ignoring.");
            return;
        }

        Collection<ServiceNode> viableServiceNodes = ServiceNodeRegistry.get().getHostingAndStableServiceNodes();
        ServiceNode bestNode = selector.select(viableServiceNodes, requestor);
        if (nonNull(bestNode)) {
            URI serviceAddress = bestNode.getServiceAddressUri();
            logger.debug("service address of best available node: {}", serviceAddress);
            HostResponse response = new HostResponse(request.getRequestorID(), serviceAddress);
            sendAsJson(requestor.webSocket, response);
        } else {
            logger.info("Couldn't find service for client. Ignoring HostRequest.");
        }
    }

    private void registerServiceNode(NodeInfo nodeInfo, WebSocket nodeWebSocket) {
        UUID serviceNodeUuid = nodeInfo.getUuid();
        nodeInfo.setWebSocket(nodeWebSocket);

        boolean isNewServiceNode = newWSClientAddresses.containsKey(serviceNodeUuid);
        if (isNewServiceNode) {
            InetAddress globalIpAddress = newWSClientAddresses.remove(serviceNodeUuid);
            nodeInfo.setGlobalIpAddress(globalIpAddress);
        }

        serviceNodeRegistry.updateNode(nodeInfo);
        ServiceNode updatedNode = serviceNodeRegistry.get(serviceNodeUuid);
        askNodeToTrackAllLatencies(updatedNode);
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

    private void registerMobileClient(MobileClientInfo mobileClientInfo, WebSocket webSocket) {
        boolean isNewClient = newWSClientAddresses.containsKey(mobileClientInfo.getUuid());

        if (isNewClient) {
            registerNewMobileClient(mobileClientInfo, webSocket);
        } else {
            updateExistingMobileClient(mobileClientInfo);
        }
    }

    private void registerNewMobileClient(MobileClientInfo mobileClientInfo, WebSocket webSocket) {
        InetAddress address = newWSClientAddresses.remove(mobileClientInfo.getUuid());
        mobileClientInfo.setPingServerAddress(address);
        mobileClientInfo.setWebSocket(webSocket);
        mobileClientRegistry.updateClient(mobileClientInfo);
    }

    private void updateExistingMobileClient(MobileClientInfo mobileClientInfo) {
        mobileClientRegistry.updateClient(mobileClientInfo);
    }

    private static void fixTransferServerAddress(ServiceResponse response, ServiceNode serverNode) {
        response.setTransferServerAddress(
                new InetSocketAddress(serverNode.globalIpAddress, response.getTransferServerAddress().getPort())
        );
    }

    // Routes a ServiceResponse from a source ServiceNode to a target ServiceNode.
    // has to add the globalIp to the transfer server address.
    private void handleServiceResponse(ServiceResponse response) {
        fixTransferServerAddress(response, serviceNodeRegistry.get(response.getSourceUuid()));
        UUID targetUuid = response.getTargetUuid();
        WebSocket returnSocket = serviceNodeRegistry.get(targetUuid).webSocket;
        sendAsJson(returnSocket, response);
    }

    // updates the service statuses of the ServiceNodes after migration.
    private void handleMigrationSuccess(MigrationSuccess migrationSuccess) {
        UUID sourceUuid = migrationSuccess.getSourceHostUuid();
        UUID targetUuid = migrationSuccess.getTargetHostUuid();
        serviceNodeRegistry.recordMigration(sourceUuid, targetUuid);
        broadcast(migrationSuccess);
    }

    private void broadcast(MigrationSuccess migrationSuccess) {
        logger.debug("Broadcasting {}", migrationSuccess);
        String json = gson.toJson(migrationSuccess);
        broadcast(json);
    }

    /**
     * Converts the given message to JSON, and sends that JSON String along the given WebSocket.
     */
    private void sendAsJson(WebSocket ws, Message message) {
        logger.debug("Sending: {}", message);
        String json = gson.toJson(message);
        ws.send(json);
    }

    private void broadcastHeartbeats() {
        ServerHeartbeatRequest request = new ServerHeartbeatRequest();
        String json = gson.toJson(request);
        broadcast(json);
    }

    @Override
    public void migrate(ServiceNode source, ServiceNode target) {
        if (source.equals(target)) logger.info("Refusing to migrate: source==target.");
        else if (target.serviceInstalled) {
            logger.info("Service installed: starting service on {}", target);
            StartServiceRequest request = new StartServiceRequest(target.uuid);
            sendAsJson(target.webSocket, request);
            broadcastMigrationAlert(NULL_SERVICE_NODE, target);
        } else {
            logger.info("Migrating {} -> {}", source, target);
            ServiceRequest request = new ServiceRequest(target.uuid, serviceName);
            serviceNodeRegistry.setToMigrating(source, target);
            sendAsJson(source.webSocket, request);
            broadcastMigrationAlert(source, target);
        }
    }

    private void broadcastMigrationAlert(ServiceNode source, ServiceNode target) {
        MigrationAlert alert = new MigrationAlert(source.globalIpAddress, target.globalIpAddress);
        logger.debug("Sending: {}", alert);
        String json = gson.toJson(alert);
        broadcast(json);
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
