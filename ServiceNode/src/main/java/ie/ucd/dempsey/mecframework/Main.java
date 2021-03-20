package ie.ucd.dempsey.mecframework;

import ie.ucd.dempsey.mecframework.metrics.latency.LatencyRequestMonitor;
import ie.ucd.dempsey.mecframework.metrics.latency.LatencyRequestor;
import ie.ucd.dempsey.mecframework.websocket.ServiceNodeWsClient;
import picocli.CommandLine;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@CommandLine.Command(name = "ServiceNode Driver", mixinStandardHelpOptions = true, version = "0.1")
public class Main implements Runnable {
    private final ScheduledExecutorService scheduleService = Executors.newScheduledThreadPool(5);

    // input parameters
    @Parameters(index = "0", paramLabel = "orchestrator",
            description = "The address of the orchestrator. Format ws://{ip address}:{port}")
    private URI serverUri;
    @Parameters(index = "1", paramLabel = "file", description = "The location of the service you wish to run")
    private File file;
    @Parameters(index = "2", paramLabel = "serviceAddress",
            description = "The address any services will run out of on this machine {ip}:{port}")
    private URI serviceAddress;
    @Parameters(index = "3", paramLabel = "nodeLabel", description = "An identifying name for this Service Node",
            defaultValue = "some-service-node")
    private String nodeLabel;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        LatencyRequestMonitor latencyRequestMonitor = new LatencyRequestMonitor();
        LatencyRequestor latencyRequestor = new LatencyRequestor(latencyRequestMonitor);
        ServiceNodeWsClient serviceNodeWsClient = new ServiceNodeWsClient(
                serverUri, file, serviceAddress, latencyRequestMonitor, latencyRequestor, nodeLabel);

        scheduleService.scheduleAtFixedRate(latencyRequestor, 3, 5, TimeUnit.SECONDS);
        scheduleService.scheduleAtFixedRate(latencyRequestMonitor, 5, 5, TimeUnit.SECONDS);
        serviceNodeWsClient.run();
    }
}
