package ie.ucd.mecframework.migration.transfer;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

public class TransferClient extends WebSocketClient {
    private static final Logger logger = LoggerFactory.getLogger(TransferClient.class);
    private final File file;
    private final CountDownLatch transferFinished;

    public TransferClient(URI serverUri, File file, CountDownLatch transferFinished) {
        super(serverUri);
        this.file = file;
        this.transferFinished = transferFinished;
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
        logger.debug("In TransferClient#onMessage");
        byte[] b = bytes.array();

        tryingToWriteFileMessage();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(b);
            fos.close();
            logger.info("File written and FileOutputStream closed");
        } catch (IOException e) {
            logger.error("", e);
        }

        // Transfer done, notify the waiting MigrationManager.
        transferFinished.countDown();
        logger.info("transfer of {} finished", file);
    }

    private void tryingToWriteFileMessage() {
        String filename = "stream.tar";
        try {
            logger.info("Trying to write file {} @ {}", filename, file.getCanonicalPath());
        } catch (IOException removeMe) {
            logger.warn("Could not construct the canonical pathname for {}", file);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        transferFinished.countDown();
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

    @Override
    public void onWebsocketPing(WebSocket connection, Framedata data) {
        super.onWebsocketPing(connection, data);
        logger.debug("Pinged by the TransferServer!");
    }
}
