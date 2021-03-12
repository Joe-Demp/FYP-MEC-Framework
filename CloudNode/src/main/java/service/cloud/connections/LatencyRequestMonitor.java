package service.cloud.connections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.core.NodeClientLatencyRequest;
import service.core.NodeClientLatencyResponse;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * A class that handles NodeClientLatencyRequests, between this node and mobile clients.
 *
 * <p>Clients call startLatencyRequest and takeLatencySnapshot.
 * This class will monitor request progress and store results.
 */
public class LatencyRequestMonitor implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(LatencyRequestMonitor.class);

    private final Map<NodeClientLatencyResponse, Future<PingResult>> awaitedLatencyResponses = new HashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(5);
    private final Map<UUID, List<Long>> latencies = new Hashtable<>();

    private static PingResult extractFinishedPingResult(Future<PingResult> pingResultFuture) {
        try {
            return pingResultFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Problem getting a finished Future<PingResult>: {}", e.getMessage());
        }
        return PingResult.ERROR_PING_RESULT;
    }

    private static NodeClientLatencyResponse mapToResponse(NodeClientLatencyRequest request) {
        return new NodeClientLatencyResponse(
                request.getNodeId(), request.getClientId(), request.getClientUri(), -1);
    }

    @Override
    public void run() {
        checkForFinishedPingTasks();
    }

    // synchronized since it accesses awaitedLatencyResponses
    private synchronized void checkForFinishedPingTasks() {
        /* Using the pre Java 1.5 style for loop with iterator in order to use method Iterator#remove()
         *
         * Loops through the Map of awaitedLatencyResponses and checks for those with finished PingTasks.
         * If a task is finished, it's removed from the Map and passed to sendFinishedNodeClientLatencyResponse.
         */
        Iterator<Map.Entry<NodeClientLatencyResponse, Future<PingResult>>> it;
        for (it = getAwaitedLatencyResponsesIterator(); it.hasNext(); ) {
            Map.Entry<NodeClientLatencyResponse, Future<PingResult>> awaitedResponse = it.next();
            if (awaitedResponse.getValue().isDone()) {
                it.remove();
                UUID clientUuid = awaitedResponse.getKey().getClientId();
                saveLatency(clientUuid, extractFinishedPingResult(awaitedResponse.getValue()));
            }
        }
    }

    private Iterator<Map.Entry<NodeClientLatencyResponse, Future<PingResult>>> getAwaitedLatencyResponsesIterator() {
        return awaitedLatencyResponses.entrySet().iterator();
    }

    // synchronized since it updates latencies
    private synchronized void saveLatency(UUID clientUuid, PingResult result) {
        List<Long> clientLatencies = latencies.getOrDefault(clientUuid, new LinkedList<>());
        long pingTime = result.getRunTimeInMillis();
        clientLatencies.add(pingTime);
        latencies.put(clientUuid, clientLatencies);
    }

    // synchronized since it accesses awaitedLatencyResponses
    public synchronized void startLatencyRequest(NodeClientLatencyRequest request) {
        // create the PingTask, submit it to the ExecutorService, store the Future.
        NodeClientLatencyResponse response = mapToResponse(request);
        PingTask task = new PingTask(request.getClientUri());
        Future<PingResult> futurePingResult = executor.submit(task);
        awaitedLatencyResponses.put(response, futurePingResult);
    }

    // synchronized since it queries and clears latencies
    public synchronized Map<UUID, List<Long>> takeLatencySnapshot() {
        Map<UUID, List<Long>> latencySnapshot = Collections.unmodifiableMap(latencies);
        latencies.clear();
        return latencySnapshot;
    }
}
