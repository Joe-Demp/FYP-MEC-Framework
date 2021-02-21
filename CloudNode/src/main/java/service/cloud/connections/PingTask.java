package service.cloud.connections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.isNull;

public class PingTask implements Callable<PingResult> {
    private static final Logger logger = LoggerFactory.getLogger(PingTask.class);
    private static final long DELAY = 100;

    private final WebSocketPingClient wsPingClient;
    private AtomicReference<PingResult> taskResult = new AtomicReference<>();

    public PingTask(URI clientUri) {
        this.wsPingClient = new WebSocketPingClient(clientUri, this);
    }

    public void submitPingResult(PingResult result) {
        if (isNull(taskResult.get())) {
            taskResult.set(result);
        } else throw new IllegalStateException("Only one result may be submitted to a PingTask.");
    }

    @Override
    public PingResult call() throws Exception {
        wsPingClient.sendLoadedPing();
//        waitForTaskResult();      // spinning lock
        waitForWebSocket();
        wsPingClient.closeBlocking();
        return taskResult.get();
    }

    private synchronized void waitForWebSocket() {
        try {
            wait();
        } catch (InterruptedException ie) {
            logger.error("PingTask was interrupted during wait for WebSocket: {}", ie.getMessage());
        }
    }

    // todo remove this spinning lock
    private void waitForTaskResult() {
        while (isNull(taskResult.get())) delay();
    }

    private void delay() {
        try {
            Thread.sleep(DELAY);
        } catch (InterruptedException ie) {
            logger.error(ie.getMessage());
            ie.printStackTrace();
        }
    }
}
