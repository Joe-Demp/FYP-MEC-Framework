package service.transfer;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;

public class TransferServer extends WebSocketServer {
    private static final Logger logger = LoggerFactory.getLogger(TransferServer.class);
    private File serviceFile;

    public TransferServer(InetSocketAddress address, File serviceFile) {
        super(address);
        this.serviceFile = serviceFile;
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        logger.debug("connected to client at {}", webSocket.getRemoteSocketAddress());
        byte[] bytesArray = new byte[(int) serviceFile.length()];

        // todo array above is of limited size

        int readResult = Integer.MAX_VALUE;
        logger.debug("Trying to send the file");
        try (FileInputStream fileInputStream = new FileInputStream(serviceFile)) {
            readResult = fileInputStream.read(bytesArray);
            webSocket.send(bytesArray);
            logger.debug("File seems to have been sent successfully");
        } catch (IOException e) {
            e.printStackTrace();
            logger.debug("readResult={}", readResult);
        }
    }

    @Override
    public void onClose(WebSocket webSocket, int code, String reason, boolean remote) {
        logger.info("In TransferServer#onClose");
        logger.info("Remote WebSocket client {}", webSocket.getRemoteSocketAddress());
        logger.info("Code, Reason, Remote: {}, {}, {}", code, reason, remote);

        // todo make sure the TransferServer stops i.e. make sure this method runs to completion
        try {
            // When the connection is closed, the server should be stopped
            logger.info("Trying to stop this TransferServer");
            stop();//if this is unstable this could be moved to the owner of this object and they could close it
        } catch (IOException | InterruptedException e) {
            logger.error("Error trying to stop the Transfer Server.", e);
        }
        logger.info("TransferServer has stopped.");

        // todo remove the issue here: it doesn't make sense to have the "delete" code in the TransferServer.
//        logger.info("Deleting the old service.");
//        boolean fileDeleted = serviceFile.delete();
//        logger.info("Old service deleted? {}", fileDeleted);
    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        logger.error("in onMessage received: {}", s);
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        logger.error("In TransferServer#onError");
        e.printStackTrace();
    }

    @Override
    public void onStart() {
        logger.debug("Launched TransferServer listening on {}", getAddress());
    }
}
