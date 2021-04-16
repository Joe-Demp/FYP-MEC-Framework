package service.transfer;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;

public class TransferClient extends WebSocketClient {
    private static final Logger logger = LoggerFactory.getLogger(TransferClient.class);
    private final File service;

    public TransferClient(URI serverUri, File service) {
        super(serverUri);
        this.service = service;
        logger.debug("Launching TransferClient for Server at {}", serverUri);
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        logger.info("connected to TransferServer at {}", getRemoteSocketAddress());
    }

    /**
     * Note this method is not used (so far that I can see)
     * Prefer `onMessage(ByteBuffer bytes)`
     * <p>
     */
    @Override
    public void onMessage(String file) {
        logger.error("Unexpected TransferClient.onMessage(String) call");
    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        byte[] b = bytes.array();
        String filename = "stream.tar";

        try {
            logger.info("Trying to write file {} @ {}", filename, service.getCanonicalPath());
        } catch (Exception removeMe) {
        }

        try (FileOutputStream fos = new FileOutputStream(service)) {
            fos.write(b);
            fos.close();
            logger.info("File written and FileOutputStream closed");
        } catch (IOException e) {
            logger.error("", e);
        }

        // Transfer done, close this client.
        close(0, "Transfer Finished");
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        logger.info("In TransferClient#onClose");
        logger.info("Code: {}", code);
        logger.info("Reason: {}", reason);
        logger.info("Remote: {}", remote);
    }

    @Override
    public void onError(Exception e) {
        logger.error("In TransferClient#onError");
        e.printStackTrace();
    }
}
