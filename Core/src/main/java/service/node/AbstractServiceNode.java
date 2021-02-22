package service.node;

import org.java_websocket.client.WebSocketClient;

import java.net.URI;

// todo pull functions from the subclasses up
public abstract class AbstractServiceNode extends WebSocketClient {
    public AbstractServiceNode(URI serverUri) {
        super(serverUri);
    }
}
