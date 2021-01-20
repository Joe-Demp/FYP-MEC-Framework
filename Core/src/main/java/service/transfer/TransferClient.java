package service.transfer;

import com.google.gson.Gson;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Instant;

public class TransferClient extends WebSocketClient {
    private static final Logger logger = LoggerFactory.getLogger(TransferClient.class);

    DockerController dockerController;
    boolean dockerLaunched = false;

    public TransferClient(URI serverUri, DockerController dockerController) {
        super(serverUri);
        this.dockerController = dockerController;
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        logger.info("connected to tempServer TIME AT FIRST CONNECTION " + Instant.now());
    }

    @Override
    public void onMessage(String file) {
        Gson gson = new Gson();
        File gsonFile = gson.fromJson(file, File.class);
        dockerLaunched = true;
        logger.info("connected to tempClient TIME AT END OF MIGRATION " + Instant.now());
        dockerController.launchServiceOnNode(gsonFile);
    }

    @Override
    public void onMessage(ByteBuffer bytes) {

        byte[] b = bytes.array();
        String filename = "service.tar";

        logger.info("Trying to write file {}", filename);
        try (FileOutputStream fos = new FileOutputStream(filename)) {
            fos.write(b);
            fos.close();
            dockerLaunched = true;
            dockerController.launchServiceOnNode(new File(filename));
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

    public DockerController dockerControllerReady() {
        if (!dockerLaunched) {
            return dockerController;
        } else {
            return null;
        }
    }
}
