package ie.ucd.mecframework.servicenode;

import ie.ucd.mecframework.messages.service.StartServiceRequest;
import ie.ucd.mecframework.messages.service.StartServiceResponse;
import ie.ucd.mecframework.metrics.ServiceNodeMetrics;
import ie.ucd.mecframework.service.AcceptServiceTask;
import ie.ucd.mecframework.service.MigrationManager;
import ie.ucd.mecframework.service.ServiceController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.core.*;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServiceNode implements Runnable {
    final String label;
    private final Logger logger = LoggerFactory.getLogger(ServiceNode.class);
    private final ExecutorService singleExecutor = Executors.newSingleThreadExecutor();
    private final ServiceNodeWsClient wsClient;
    private final ServiceNodeMetrics metrics;
    private final ServiceController serviceController;
    private final MigrationManager migrationManager;
    private UUID uuid;

    // Note the port number must be the same as is in the ServiceController being used
    private URI serviceAddress;
    private State state = State.STABLE;

    public ServiceNode(URI orchestrator, ServiceController serviceController, File serviceFile, String label,
                       long pingDelay) {
        this.wsClient = new ServiceNodeWsClient(orchestrator, this);
        this.metrics = new ServiceNodeMetrics(pingDelay);
        this.serviceController = serviceController;
        this.migrationManager = new MigrationManager(serviceFile, serviceController);
        this.label = label;
        serviceAddress = makeServiceAddress();
    }

    private static URI makeServiceAddress() {
        int portNumber = ServiceNodeProperties.get().getAdvertisedServicePortNumber();
        return URI.create("http://localhost:" + portNumber);
    }

    @Override
    public void run() {
        wsClient.run();     // this blocks (keeping the application from shutting down)
    }

    /**
     * Processes NodeInfoRequests, sent to the Service Node by the Orchestrator when a connection is opened.
     *
     * <p>Takes the assigned {@code UUID} and sends a heartbeat response.</p>
     */
    void handleNodeInfoRequest(NodeInfoRequest request) {
        uuid = request.getUuid();
        sendHeartbeatResponse();
    }

    void sendHeartbeatResponse() {
        NodeInfo nodeInfo = new NodeInfo(uuid, serviceController.isServiceRunning(), serviceAddress);
        nodeInfo.setServiceInstalled(serviceController.serviceExists());
        metrics.populateNodeInfo(nodeInfo);
        wsClient.sendAsJson(nodeInfo);
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
                    uuid, response.getSourceUuid(), response.getServiceName());
            wsClient.sendAsJson(migrationSuccess);
        }, singleExecutor);
    }

    private void sendServiceResponse(ServiceRequest request, InetSocketAddress transferServerAddress) {
        ServiceResponse response = new ServiceResponse(
                request.getTargetUuid(), uuid, transferServerAddress, request.getDesiredServiceName());
        wsClient.sendAsJson(response);
    }

    void handleLatencyRequest(NodeClientLatencyRequest request) {
        metrics.registerLatencyRequest(request);
    }

    void handleStartServiceRequest(StartServiceRequest request) {
        CompletableFuture<Void> startServiceTask =
                CompletableFuture.runAsync(this::startServiceIfItExists, singleExecutor);

        StartServiceResponse response =
                new StartServiceResponse(request.getUuid(), serviceController.isServiceRunning());
        startServiceTask.thenRunAsync(() -> wsClient.sendAsJson(response), singleExecutor);
    }

    private void startServiceIfItExists() {
        if (serviceController.serviceExists()) serviceController.startService();
    }

    public enum State {STABLE, TRANSFER_SERVER, TRANSFER_CLIENT}
}
