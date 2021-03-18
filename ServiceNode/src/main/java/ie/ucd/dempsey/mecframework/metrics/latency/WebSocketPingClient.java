package ie.ucd.dempsey.mecframework.metrics.latency;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Instant;

public class WebSocketPingClient extends WebSocketClient {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketPingClient.class);
    private final PingTask pingTask;
    private final PingResult result = new PingResult();
    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();


    public WebSocketPingClient(URI serverUri, PingTask pingTask) {
        super(serverUri);
        this.pingTask = pingTask;
        logger.debug("Creating client for server at {}", serverUri);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {

    }

    @Override
    public void onMessage(String message) {
        Instant timeReceived = Instant.now();
        logger.debug("from {}\n{}", getRemoteSocketAddress(), message);

        gson.fromJson(message, PingMessage.class);  // check if message was a PingMessage. Throw an error otherwise

        result.finishTime = timeReceived;
        pingTask.submitPingResult(result);
        notifyPingTask();
    }

    private void notifyPingTask() {
        /* Have to use the synchronized block so that this Thread is the owner of the PingTask's monitor
         *
         * "A thread becomes the owner of the object's monitor ...
         *      By executing the body of a synchronized statement that synchronizes on the object."
         */
        synchronized (pingTask) {
            pingTask.notify();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        logger.debug("Closing. code={} reason={} remote?={}", code, reason, remote);
    }

    @Override
    public void onError(Exception ex) {
        logger.error(ex.getMessage());
        // could print the stack trace here
    }

    /**
     * Creates a record in this WebSocketPingClient's ping cache and sends a {@code PingMessage} to the server.
     */
    public void sendLoadedPing() {
        sendAndTime(new PingMessage());
    }

    private void sendAndTime(PingMessage message) {
        String json = gson.toJson(message);
        logger.debug("Sending: {}", json);
        result.startTime = Instant.now();
        send(json);
    }
}
