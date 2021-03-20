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

    DockerController dockerController;
    private boolean dockerLaunched = false;

    public TransferClient(URI serverUri, DockerController dockerController) {
        super(serverUri);
        this.dockerController = dockerController;
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
     * todo remove asap
     */
    @Override
    public void onMessage(String file) {
        logger.error("Unexpected TransferClient.onMessage(String) call");
    }

    @Override
    public void onMessage(ByteBuffer bytes) {

        byte[] b = bytes.array();
        String filename = "service.tar";

        logger.info("Trying to write file {}", filename);
        try (FileOutputStream fos = new FileOutputStream(filename)) {
            fos.write(b);
            fos.close();
            logger.info("File written and FileOutputStream closed");

            dockerController.launchServiceOnNode(new File(filename));
            dockerLaunched = true;
            logger.info("All Docker work should be done by now");
        } catch (IOException e) {
            logger.error("");
            e.printStackTrace();
        }
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

    // todo change this to return a boolean or a completable future or something useful
    public DockerController dockerControllerReady() {
        if (dockerController.isDockerRunning()) {
            return dockerController;
        } else {
            return null;
        }
    }
}
