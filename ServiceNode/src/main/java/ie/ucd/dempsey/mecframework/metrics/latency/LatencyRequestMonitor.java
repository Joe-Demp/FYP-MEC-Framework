package ie.ucd.dempsey.mecframework.metrics.latency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.core.NodeClientLatencyRequest;

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

    private final Map<UUID, Future<PingResult>> awaitedLatencyResponses = new Hashtable<>();
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
        Iterator<Map.Entry<UUID, Future<PingResult>>> it;
        for (it = clientUUIDtoPingResultIterator(); it.hasNext(); ) {
            Map.Entry<UUID, Future<PingResult>> awaitedResponseEntry = it.next();
            Future<PingResult> pingResult = awaitedResponseEntry.getValue();

            if (pingResult.isDone()) {
                it.remove();
                UUID clientUuid = awaitedResponseEntry.getKey();
                saveLatency(clientUuid, extractFinishedPingResult(pingResult));
            }
        }
    }

    private Iterator<Map.Entry<UUID, Future<PingResult>>> clientUUIDtoPingResultIterator() {
        return awaitedLatencyResponses.entrySet().iterator();
    }

    // synchronized since it updates latencies
    private synchronized void saveLatency(UUID clientUuid, PingResult result) {
        List<Long> clientLatencies = latencies.getOrDefault(clientUuid, new LinkedList<>());
        long pingTime = result.getRunTimeInMillis();
        clientLatencies.add(pingTime);
        latencies.put(clientUuid, clientLatencies);
    }

    public void startLatencyRequest(NodeClientLatencyRequest request) {
        PingTask task = new PingTask(request.getClientUri());
        Future<PingResult> futurePingResult = executor.submit(task);
        awaitedLatencyResponses.put(request.getClientId(), futurePingResult);
    }

    // synchronized since it queries and clears latencies
    public synchronized Map<UUID, List<Long>> takeLatencySnapshot() {
        Map<UUID, List<Long>> latencySnapshot = Map.copyOf(latencies);
        latencies.clear();
        return latencySnapshot;
    }
}
