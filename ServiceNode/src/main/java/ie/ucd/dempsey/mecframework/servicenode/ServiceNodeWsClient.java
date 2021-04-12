package ie.ucd.dempsey.mecframework.servicenode;

import com.google.gson.Gson;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.core.*;
import service.util.Gsons;

import java.net.URI;

public class ServiceNodeWsClient extends WebSocketClient {
    private final Logger logger = LoggerFactory.getLogger(ServiceNodeWsClient.class);

    private final Gson gson = Gsons.serviceNodeGson();
    private final ServiceNode serviceNode;

    public ServiceNodeWsClient(URI serverUri, ServiceNode serviceNode) {
        super(serverUri);
        this.serviceNode = serviceNode;
    }

    /**
     * Converts the given message to JSON, and sends that JSON String along the given WebSocket.
     */
    public final void sendAsJson(Message message) {
        logger.debug("Sending: {}", message);
        String json = gson.toJson(message);
        send(json);
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        logger.info("Listening to orchestrator at {} on local address {}",
                getRemoteSocketAddress(), getLocalSocketAddress());
    }

    @Override
    public void onMessage(String message) {
        Message messageObj = gson.fromJson(message, Message.class);
        logger.debug("Received: {}", messageObj);
        switch (messageObj.getType()) {
            case Message.MessageTypes.NODE_INFO_REQUEST:
                serviceNode.handleNodeInfoRequest((NodeInfoRequest) messageObj);
                break;
            case Message.MessageTypes.SERVER_HEARTBEAT_REQUEST:
                serviceNode.sendHeartbeatResponse();
                break;
            case Message.MessageTypes.SERVICE_REQUEST:
                serviceNode.handleServiceRequest((ServiceRequest) messageObj);
                break;
            case Message.MessageTypes.SERVICE_RESPONSE:
                serviceNode.handleServiceResponse((ServiceResponse) messageObj);
                break;
            case Message.MessageTypes.NODE_CLIENT_LATENCY_REQUEST:
                serviceNode.handleLatencyRequest((NodeClientLatencyRequest) messageObj);
                break;
            case Message.MessageTypes.MIGRATION_SUCCESS:
                break;
            default:
                logger.error("Message received with unrecognised type: {}", messageObj.getType());
                break;
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        logger.warn("Closing {}", serviceNode.label);
        logger.warn("{} {} {}", code, reason, remote);
    }

    @Override
    public void onError(Exception ex) {
        logger.error("Error in " + serviceNode.label, ex);
    }
}
