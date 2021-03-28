package ie.ucd.dempsey.mecframework.metrics.latency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.isNull;

public class PingTask implements Callable<PingResult> {
    private static final Logger logger = LoggerFactory.getLogger(PingTask.class);

    private final WebSocketPingClient wsPingClient;
    private AtomicReference<PingResult> taskResult = new AtomicReference<>();

    public PingTask(URI clientUri) {
        this.wsPingClient = new WebSocketPingClient(clientUri, this);
        // wsPingClient.connect called in method call()
    }

    public void submitPingResult(PingResult result) {
        if (isNull(taskResult.get())) {
            taskResult.set(result);
        } else throw new IllegalStateException("Only one result may be submitted to a PingTask.");
    }

    @Override
    public PingResult call() throws Exception {
        connectAndSendPing();
        waitForWebSocket();
        wsPingClient.closeBlocking();
        return taskResult.get();
    }

    private void connectAndSendPing() throws Exception {
        wsPingClient.connectBlocking();
        wsPingClient.sendLoadedPing();
    }

    private synchronized void waitForWebSocket() {
        try {
            // todo replace with a CountDownLatch
            wait();
        } catch (InterruptedException ie) {
            logger.error("PingTask was interrupted during wait for WebSocket: {}", ie.getMessage());
        }
    }
}
