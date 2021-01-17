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
import service.orchestrator.exceptions.NoSuchNodeException;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.*;

// todo remove hard coded values and use a config file instead (or command line args)

public class Orchestrator extends WebSocketServer {
    private static final Logger logger = LoggerFactory.getLogger(Orchestrator.class);
    private static final long HEARTBEAT_REQUEST_PERIOD = 20L * 1000L;

    int rollingAverage;

    // todo expand this into 2 maps: 1 for ApplicationNodes, another for Clients
    private Map<UUID, NodeInfo> connectedNodes = new HashMap<>();
    private Gson gson;

    public Orchestrator(int port, int rollingAverage) {
        super(new InetSocketAddress(port));
        this.rollingAverage = rollingAverage / 100;
        initializeGson();

        // todo check if this rolling average thing works, shouldn't an int divided by a bigger int = 0 ?
        logger.debug("rollingAverage={}", rollingAverage);

        // todo extract this to a method
        new Timer().schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        logger.debug("Current connections:");
                        for (NodeInfo node : connectedNodes.values()) {
                            logger.debug(node.toString());
                        }
                        logger.debug("End Current Connections.");

                        logger.debug("Sending HeartbeatRequests");
                        for (NodeInfo node : connectedNodes.values()) {
                            ServerHeartbeatRequest heartbeat = new ServerHeartbeatRequest(node.getSystemID());
                            sendAsJson(node.getWebSocket(), heartbeat);
                        }
                    }
                }, HEARTBEAT_REQUEST_PERIOD, HEARTBEAT_REQUEST_PERIOD);
    }

    private void initializeGson() {
        RuntimeTypeAdapterFactory<Message> adapter = RuntimeTypeAdapterFactory
                .of(Message.class, "type")
                .registerSubtype(NodeInfo.class, Message.MessageTypes.NODE_INFO)
                .registerSubtype(ServiceRequest.class, Message.MessageTypes.SERVICE_REQUEST)
                .registerSubtype(ServiceResponse.class, Message.MessageTypes.SERVICE_RESPONSE)
                .registerSubtype(HostRequest.class, Message.MessageTypes.HOST_REQUEST)
                .registerSubtype(NodeInfoRequest.class, Message.MessageTypes.NODE_INFO_REQUEST)
                .registerSubtype(MigrationSuccess.class, Message.MessageTypes.MIGRATION_SUCCESS);
        gson = new GsonBuilder().setPrettyPrinting().registerTypeAdapterFactory(adapter).create();
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        logger.info("new connection :" + webSocket.getRemoteSocketAddress());
        logger.debug("Resource descriptor: " + clientHandshake.getResourceDescriptor());
        logger.debug("Http Headers:");
        for (Iterator<String> it = clientHandshake.iterateHttpFields(); it.hasNext(); ) {
            String field = it.next();
            logger.debug("{} : {}", field, clientHandshake.getFieldValue(field));
        }
        logger.debug("END Http Headers.");

        UUID UUIDToReturn = UUID.randomUUID();

        // todo clean this if it's not needed,
        //  the NodeInfo should be added to the map in onMessage anyway.
        //  Better to keep the "All NodeInfos in connectedNodes are non null" invariant
        // connectedNodes.put(UUIDToReturn, null);//no node information yet to add

        //create a nodeInfoRequest and send it back to the node
        NodeInfoRequest infoRequest = new NodeInfoRequest(UUIDToReturn);
        sendAsJson(webSocket, infoRequest);
    }

    @Override
    public void onMessage(WebSocket webSocket, String message) {
        logger.debug(message);
        logger.debug("from {}", webSocket.getRemoteSocketAddress());    // todo replace this call with something that
        //  gets the actual (global) ip address

        Message messageObj = gson.fromJson(message, Message.class);

        //this routes inbound messages based on type and then moves them to other methods
        switch (messageObj.getType()) {
            case Message.MessageTypes.NODE_INFO:
                NodeInfo nodeInfo = (NodeInfo) messageObj;
                nodeInfo.setWebSocket(webSocket);
                connectedNodes.put(nodeInfo.getSystemID(), nodeInfo);
                if (nodeInfo.getServiceName() != null && nodeInfo.getServiceName().equals("MobileUser")) {
                    connectedNodes.remove(nodeInfo.getSystemID());
                }
                break;
            case Message.MessageTypes.SERVICE_REQUEST:
                ServiceRequest serviceRequest = (ServiceRequest) messageObj;
                String jsonStr = gson.toJson(serviceRequest);
                WebSocket a = findBestServiceOwner(serviceRequest).getWebSocket();
                a.send(jsonStr);
                break;
            case Message.MessageTypes.SERVICE_RESPONSE:
                // Note: returnSocket seems to be for the mobile client that requested a service host
                ServiceResponse response = (ServiceResponse) messageObj;
                WebSocket returnSocket = connectedNodes.get(response.getRequstorID()).getWebSocket();
                jsonStr = gson.toJson(response);
                returnSocket.send(jsonStr);
                break;
            case Message.MessageTypes.MIGRATION_SUCCESS:
                MigrationSuccess successMessage = (MigrationSuccess) messageObj;
                connectedNodes.get(successMessage.getHostId()).setServiceName(successMessage.getServiceName());
                connectedNodes.get(successMessage.getOldHostId()).setServiceName("noService");
                break;
            case Message.MessageTypes.HOST_REQUEST:
                HostRequest hostRequest = (HostRequest) messageObj;
                ServiceRequest requestFromUser = new ServiceRequest(hostRequest.getRequestorID(), hostRequest.getRequestedServiceName());
                NodeInfo returnedNode = transferServiceToBestNode(requestFromUser);
                URI returnURI = returnedNode.getServiceHostAddress();
                HostResponse responseForClient = new HostResponse(hostRequest.getRequestorID(), returnURI);
                sendAsJson(webSocket, responseForClient);
                break;
        }
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
     *
     * @param serviceRequest the requested service
     * @return the NodeInfo for the node that was deemed best
     */
    public NodeInfo transferServiceToBestNode(ServiceRequest serviceRequest) {
        NodeInfo bestNode = findBestNode(connectedNodes);
        NodeInfo worstCurrentOwner = findWorstServiceOwner(serviceRequest);

        // DEBUG Remove me
        System.out.println("\n***********************");
        System.out.println("Debug in Orchestrator#transferServiceToBestNode");
        System.out.println("***********************");
        System.out.println(serviceRequest);
        System.out.println("bestNode: " + bestNode);
        System.out.println("worstCurrentOwner: " + worstCurrentOwner);
        System.out.println("***********************\n");
        // END DEBUG

        if (worstCurrentOwner.getSystemID().equals(bestNode.getSystemID())) {
            return bestNode;
        }
        ServiceRequest request = new ServiceRequest(bestNode.getSystemID(), serviceRequest.getServiceName());
        String jsonStr = gson.toJson(request);

        System.out.println("Sending a service request to worstCurrent owner");
        worstCurrentOwner.getWebSocket().send(jsonStr);//this tells the current worst owner of a service that its relived of duty and can send away its service

        return bestNode;
    }

    /**
     * This method finds the owner of the Service that scores lowest by the evaluation metric
     *
     * @param serviceRequest
     * @return the nodeInfo for the worst owner
     */
    public NodeInfo findWorstServiceOwner(ServiceRequest serviceRequest) {
        NodeInfo worstNode = null;
        Map<UUID, NodeInfo> allServiceOwnerAddresses = findAllServiceOwners(serviceRequest);
        if (allServiceOwnerAddresses == null) {
            //noone had this service, inform edgenode
        } else {
            worstNode = findBestNode(allServiceOwnerAddresses);
        }
        return worstNode;
    }

    /**
     * This method finds the best owner of a service
     *
     * @param serviceRequest
     * @return the nodeInfo of the best owner
     */
    public NodeInfo findBestServiceOwner(ServiceRequest serviceRequest) {
        NodeInfo bestNode = null;
        Map<UUID, NodeInfo> allServiceOwnerAddresses = findAllServiceOwners(serviceRequest);
        if (allServiceOwnerAddresses == null) {
            //noone had this service, inform edgenode
        } else {
            bestNode = findBestNode(allServiceOwnerAddresses);
        }
        return bestNode;
    }

    /**
     * This method finds every node that currently is hosting a given service
     *
     * @param serviceRequest
     * @return Map of all owners of a service
     */
    public Map<UUID, NodeInfo> findAllServiceOwners(ServiceRequest serviceRequest) {
        Map<UUID, NodeInfo> toReturn = new HashMap<>();
        for (Map.Entry<UUID, NodeInfo> entry : connectedNodes.entrySet()) {
            if (entry.getValue().getServiceName() != null && entry.getValue().getServiceName().equals(serviceRequest.getServiceName())) {
                toReturn.put(entry.getKey(), entry.getValue());
            }
        }
        return toReturn;
    }

    /**
     * This method takes in a list of nodes and finds the best node on that service, ths acts as the evaluation method for the orchestrator
     *
     * @param Nodes
     * @return the best node's NodeInfo
     */
    private NodeInfo findBestNode(Map<UUID, NodeInfo> Nodes) {
        double bestNodeScore = 200;
        NodeInfo bestNode = null;
        for (Map.Entry<UUID, NodeInfo> entry : Nodes.entrySet()) {
            double currentNodeCPUScore = calculateRecentCPULoad(entry.getValue().getCPUload(), entry.getValue().getRollingCPUScore());
            double currentNodeRamScore = calculateRecentRamLoad(entry.getValue().getRamLoad(), entry.getValue().getRollingRamScore());
            if (currentNodeCPUScore + currentNodeRamScore < bestNodeScore && entry.getValue().isTrustyworthy()) {
                bestNodeScore = currentNodeCPUScore;
                bestNode = entry.getValue();
            }
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

        return ((1 - rollingAverage) * rollingCPUScore) + (rollingAverage * (runningTotal / 5));
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
        return ((1 - rollingAverage) * rollingRamScore) + (rollingAverage * (runningTotal / 5));
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

        // remove the node that owns the connection
        try {
            UUID toRemove = connectedNodes.entrySet().stream()
                    .filter(e -> e.getValue().getWebSocket().equals(webSocket))
                    .findAny()
                    .orElseThrow(NoSuchNodeException::new)
                    .getKey();

            logger.debug("Removing Node {} from connectedNodes.", toRemove);
            connectedNodes.remove(toRemove);
        } catch (NoSuchNodeException e) {
            logger.error("No Node with address {} found in connectedNodes", webSocket.getRemoteSocketAddress());
            e.printStackTrace();
        }
    }
}
