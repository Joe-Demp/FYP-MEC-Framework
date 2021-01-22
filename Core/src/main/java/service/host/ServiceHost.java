package service.host;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.transfer.DockerController;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class ServiceHost extends WebSocketServer {
    private static final Logger logger = LoggerFactory.getLogger(ServiceHost.class);
    DockerController dockerController;

    public ServiceHost(int port, DockerController dockerController) throws UnknownHostException {
        super(new InetSocketAddress(port));
        logger.info("Launched a ServiceHost on port {}", port);
        this.dockerController = dockerController;
        logger.info("This server listens to {}", getAddress());

    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        logger.info("Opening");
    }

    @Override
    public void onClose(WebSocket webSocket, int code, String reason, boolean remote) {
        logger.info("In ServiceHost#onClose");
        logger.info("Code: {}", code);
        logger.info("Reason: {}", reason);
        logger.info("Remote: {}", remote);
    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        try {
            dockerController.sendInput(s);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        logger.error("Error in ServiceHost with respect to socket at {}", webSocket.getRemoteSocketAddress());
    }

    @Override
    public void onStart() {

    }
}
