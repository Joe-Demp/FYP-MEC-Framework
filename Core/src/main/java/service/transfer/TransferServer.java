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
        } catch (IOException e) {
            e.printStackTrace();
            logger.debug("readResult={}", readResult);
        }
        logger.debug("File seems to have been sent successfully");
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        // todo remove the .close here?
        webSocket.close();

        try {
            // When the connection is closed, the server should be stopped

            stop();//if this is unstable this could be moved to the owner of this object and they could close it
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        // Does nothing with messages received
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        // todo implement something here
    }

    @Override
    public void onStart() {
        logger.debug("Launched the transfer server");
    }
}
