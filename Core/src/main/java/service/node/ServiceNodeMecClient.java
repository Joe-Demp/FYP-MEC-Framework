package service.node;

import com.google.gson.Gson;
import org.java_websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.core.Message;
import service.util.Gsons;

import java.net.URI;

// todo pull functions from the subclasses up
// todo rethink this (at least the naming -> we want nodes to *use* WebSockets, not *be* them

/**
 * A superclass for WebSocket clients that facilitate the Multi-access Edge Cloud framework by communicating with a
 * MEC orchestrator.
 */
public abstract class ServiceNodeMecClient extends WebSocketClient {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected Gson gson;

    public ServiceNodeMecClient(URI serverUri) {
        super(serverUri);
        gson = Gsons.serviceNodeGson();
    }

    /**
     * Converts the given message to JSON, and sends that JSON String along the given WebSocket.
     */
    public final void sendAsJson(Message message) {
        String json = gson.toJson(message);
        logger.debug("Sending: {}", json);
        send(json);
    }
}
