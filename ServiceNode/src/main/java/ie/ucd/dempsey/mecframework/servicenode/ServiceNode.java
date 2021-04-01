package ie.ucd.dempsey.mecframework.servicenode;

import ie.ucd.dempsey.mecframework.metrics.ServiceNodeMetrics;
import ie.ucd.dempsey.mecframework.metrics.latency.LatencyRequestMonitor;
import ie.ucd.dempsey.mecframework.metrics.latency.LatencyRequestor;
import ie.ucd.dempsey.mecframework.service.AcceptServiceTask;
import ie.ucd.dempsey.mecframework.service.MigrationManager;
import ie.ucd.dempsey.mecframework.service.ServiceController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.core.*;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;

import static java.util.stream.Collectors.toList;

public class ServiceNode implements Runnable {
    final String label;
    private final Logger logger = LoggerFactory.getLogger(ServiceNode.class);
    private final ScheduledExecutorService scheduleService = Executors.newScheduledThreadPool(5);
    private final ExecutorService singleExecutor = Executors.newSingleThreadExecutor();
    private final ServiceNodeWsClient wsClient;
    private final LatencyRequestMonitor latencyRequestMonitor = new LatencyRequestMonitor();
    private final LatencyRequestor latencyRequestor = new LatencyRequestor(latencyRequestMonitor);
    private final ServiceNodeMetrics metrics;
    private final ServiceController serviceController;
    private final MigrationManager migrationManager;
    private final long pingDelay;
    private UUID uuid;
    private URI serviceAddress;
    private State state = State.STABLE;


    public ServiceNode(URI orchestrator, ServiceController serviceController, File serviceFile, String label,
                       long pingDelay) {
        this.wsClient = new ServiceNodeWsClient(orchestrator, this);
        this.metrics = new ServiceNodeMetrics();
        this.serviceController = serviceController;
        this.migrationManager = new MigrationManager(serviceFile, serviceController);
        this.label = label;
        this.pingDelay = pingDelay;
    }

    @Override
    public void run() {
        scheduleService.scheduleAtFixedRate(latencyRequestor, 3, 5, TimeUnit.SECONDS);
        scheduleService.scheduleAtFixedRate(latencyRequestMonitor, 5, 5, TimeUnit.SECONDS);
        // start more threads!

        logger.info("Service running? {}", serviceController.isServiceRunning());

        // this blocks (keeping the application from shutting down)
        wsClient.run();
    }

    /**
     * Processes NodeInfoRequests, sent to the Service Node by the Orchestrator when a connection is opened.
     *
     * <p>Takes the assigned {@code UUID} and sends a heartbeat response.</p>
     */
    void handleNodeInfoRequest(NodeInfoRequest request) {
        uuid = request.getAssignedUUID();
        sendHeartbeatResponse();
    }

    void sendHeartbeatResponse() {
        NodeInfo nodeInfo = new NodeInfo(uuid, serviceController.name(), serviceAddress);

        // adding performance data
        // todo replace these with recent values only, not the entire map
        logger.warn("No updates made to historicalCPUload or historicalRamload or ...");
        metrics.populateNodeInfo(nodeInfo);

        Map<UUID, List<Long>> delayedLatencies = latenciesWithDelay(latencyRequestMonitor.takeLatencySnapshot());
        nodeInfo.setLatencies(delayedLatencies);
        // END adding performance data

        wsClient.sendAsJson(nodeInfo);
    }

    private Map<UUID, List<Long>> latenciesWithDelay(Map<UUID, List<Long>> latencies) {
        Map<UUID, List<Long>> delayedLatencies = new HashMap<>();

        for (Map.Entry<UUID, List<Long>> entry : latencies.entrySet()) {
            List<Long> delayed = entry.getValue().stream()
                    .map(latency -> latency + pingDelay)
                    .collect(toList());
            delayedLatencies.put(entry.getKey(), delayed);
        }
        return Collections.unmodifiableMap(delayedLatencies);
    }

    /**
     * Starts the process wherein this Service Node will send its application to the target Service Node.
     */
    void handleServiceRequest(ServiceRequest request) {
        if (state != State.STABLE) {
            throw new RuntimeException("ServiceNode not in a stable state. Probably migrated already.");
        }

        logger.info("{} received a ServiceRequest!", label);
        state = State.TRANSFER_SERVER;
        InetSocketAddress transferServerAddress = migrationManager.migrateService();
        sendServiceResponse(request, transferServerAddress);
    }

    /**
     * Starts the process wherein this Service Node will receive an application from the source Service Node.
     */
    void handleServiceResponse(ServiceResponse response) {
        if (state != State.STABLE) {
            throw new RuntimeException("ServiceNode not in a stable state. Probably migrated already.");
        }

        logger.info("{} received a ServiceResponse!", label);
        state = State.TRANSFER_CLIENT;

        // todo extract the following
        AcceptServiceTask acceptTask = new AcceptServiceTask(
                migrationManager, response.getTransferServerAddress(), serviceController);
        CompletableFuture<Void> task = CompletableFuture.runAsync(acceptTask, singleExecutor);
        task.thenRunAsync(() -> {
            MigrationSuccess migrationSuccess = new MigrationSuccess(
                    uuid, response.getSourceNodeUuid(), response.getServiceName());
            wsClient.sendAsJson(migrationSuccess);
        }, singleExecutor);
    }

    private void sendServiceResponse(ServiceRequest request, InetSocketAddress transferServerAddress) {
        ServiceResponse response = new ServiceResponse(
                request.getTargetNodeUuid(), uuid, transferServerAddress, request.getDesiredServiceName());
        wsClient.sendAsJson(response);
    }

    void handleLatencyRequest(NodeClientLatencyRequest request) {
        latencyRequestor.registerRequest(request);
    }

    public enum State {STABLE, TRANSFER_SERVER, TRANSFER_CLIENT}
}
