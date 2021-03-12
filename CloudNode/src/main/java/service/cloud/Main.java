package service.cloud;

import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import service.cloud.connections.LatencyRequestMonitor;
import service.cloud.connections.LatencyRequestor;

import java.io.File;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@CommandLine.Command(name = "cmMain", mixinStandardHelpOptions = true, version = "0.7")
public class Main implements Runnable {

    @Option(names = {"-s", "--secure"},
            description = "Secure mode, only engages with orchestrator using SSL")
    private boolean secure;

    @Parameters(index = "0", paramLabel = "address", description = "The address of the orchestrator format wss://{ip}:{port}")
    private URI address;

    @Parameters(index = "1", paramLabel = "file", description = "The location of the service you wish to run")
    private File file;

    @Parameters(index = "2", paramLabel = "serviceAddress", description = "The address any services will run out of on this machine {ip}:{port}")
    private URI port;

    private static final ScheduledExecutorService scheduleService = Executors.newScheduledThreadPool(5);

    @Override
    public void run() {
        if (!secure) {
            // todo tidy this
            LatencyRequestMonitor latencyRequestMonitor = new LatencyRequestMonitor();
            LatencyRequestor latencyRequestor = new LatencyRequestor(latencyRequestMonitor);
            Cloud cloud = new Cloud(address, file, port, secure, latencyRequestMonitor, latencyRequestor);

            scheduleService.scheduleAtFixedRate(latencyRequestor, 3, 5, TimeUnit.SECONDS);
            scheduleService.scheduleAtFixedRate(latencyRequestMonitor, 5, 5, TimeUnit.SECONDS);
            cloud.run();
        } else {
            try {
                new SecureCloud(address, file, port,secure);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}
