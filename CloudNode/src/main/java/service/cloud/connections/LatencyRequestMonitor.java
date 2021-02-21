package service.cloud.connections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.cloud.Cloud;
import service.core.NodeClientLatencyRequest;
import service.core.NodeClientLatencyResponse;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A class that handles NodeClientLatencyRequests, between this node and mobile clients.
 *
 * <p>The Node should pass its NodeClientLatencyRequests to this class through method startLatencyRequest. This class
 * will monitor the progress of those requests and send responses when they're ready.
 */
public class LatencyRequestMonitor implements Runnable {
    private static final long PULSE = 1000L;
    private static final Logger logger = LoggerFactory.getLogger(LatencyRequestMonitor.class);

    // todo this should be a "Node", because Clouds and Edges should be location agnostic
    //  reason why: need to use method Cloud#sendAsJson
    private final Cloud wsClient;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Map<NodeClientLatencyResponse, Future<PingResult>> awaitedLatencyResponses = new HashMap<>();
    private final Lock futurePingResultsIterationLock = new ReentrantLock();
    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    public LatencyRequestMonitor(Cloud wsClient) {
        this.wsClient = wsClient;
    }

    private static PingResult extractFinishedPingResult(Future<PingResult> pingResultFuture) {
        try {
            return pingResultFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Problem getting a finished Future<PingResult>: {}", e.getMessage());
        }
        return PingResult.ERROR_PING_RESULT;
    }

    private static void waitForPulse() {
        try {
            Thread.sleep(PULSE);
        } catch (InterruptedException ie) {
            logger.error("Interrupted while sleeping.");
        }
    }

    private static NodeClientLatencyResponse mapToResponse(NodeClientLatencyRequest request) {
        return new NodeClientLatencyResponse(request.nodeId, request.clientId, request.clientUri, -1);
    }

    @Override
    public void run() {
        while (running.get()) {
            futurePingResultsIterationLock.lock();
            checkForFinishedPingTasks();
            futurePingResultsIterationLock.unlock();

            waitForPulse();
        }
    }

    private void checkForFinishedPingTasks() {
        /* Using the pre Java 1.5 style for loop with iterator in order to use method Iterator#remove()
         *
         * Loops through the Map of awaitedLatencyResponses and checks for those with finished PingTasks.
         * If a task is finished, it's removed from the Map and passed to sendFinishedNodeClientLatencyResponse.
         */
        for (Iterator<Map.Entry<NodeClientLatencyResponse, Future<PingResult>>> it = getAwaitedLatencyResponsesIterator();
             it.hasNext(); ) {
            Map.Entry<NodeClientLatencyResponse, Future<PingResult>> awaitedResponse = it.next();
            if (awaitedResponse.getValue().isDone()) {
                it.remove();
                sendFinishedNodeClientLatencyResponse(
                        awaitedResponse.getKey(), extractFinishedPingResult(awaitedResponse.getValue())
                );
            }
        }
    }

    private Iterator<Map.Entry<NodeClientLatencyResponse, Future<PingResult>>> getAwaitedLatencyResponsesIterator() {
        return awaitedLatencyResponses.entrySet().iterator();
    }

    private void sendFinishedNodeClientLatencyResponse(NodeClientLatencyResponse response, PingResult result) {
        response.latency = result.getRunTimeInMillis();
        wsClient.sendAsJson(response);
    }

    public void startLatencyRequest(NodeClientLatencyRequest request) {
        // create the PingTask, submit it to the ExecutorService, store the Future.
        NodeClientLatencyResponse response = mapToResponse(request);
        PingTask task = new PingTask(request.clientUri);
        Future<PingResult> futurePingResult = executor.submit(task);
        awaitedLatencyResponses.put(response, futurePingResult);
    }
}
