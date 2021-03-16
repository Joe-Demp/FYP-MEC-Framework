package service.node;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import org.java_websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.core.*;
import service.util.InetSocketAddressAdapter;

import java.net.InetSocketAddress;
import java.net.URI;

// todo pull functions from the subclasses up
// todo rethink this (at least the naming -> we want nodes to *use* WebSockets, not *be* them
public abstract class AbstractServiceNode extends WebSocketClient {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected Gson gson;

    public AbstractServiceNode(URI serverUri) {
        super(serverUri);
        initializeGson();
    }

    private void initializeGson() {
        RuntimeTypeAdapterFactory<Message> adapter = RuntimeTypeAdapterFactory
                .of(Message.class, "type")
                .registerSubtype(NodeInfo.class, Message.MessageTypes.NODE_INFO)
                .registerSubtype(ServerHeartbeatRequest.class, Message.MessageTypes.SERVER_HEARTBEAT_REQUEST)
                .registerSubtype(ServiceRequest.class, Message.MessageTypes.SERVICE_REQUEST)
                .registerSubtype(ServiceResponse.class, Message.MessageTypes.SERVICE_RESPONSE)
                .registerSubtype(NodeInfoRequest.class, Message.MessageTypes.NODE_INFO_REQUEST)
                .registerSubtype(NodeClientLatencyRequest.class, Message.MessageTypes.NODE_CLIENT_LATENCY_REQUEST);

        gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapterFactory(adapter)
                .registerTypeAdapter(InetSocketAddress.class, new InetSocketAddressAdapter())
                .create();
    }

    /**
     * Converts the given message to JSON, and sends that JSON String along the given WebSocket.
     */
    public void sendAsJson(Message message) {
        String json = gson.toJson(message);
        logger.debug("Sending: {}", json);
        send(json);
    }
}
