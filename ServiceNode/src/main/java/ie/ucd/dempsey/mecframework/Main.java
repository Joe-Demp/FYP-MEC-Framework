package ie.ucd.dempsey.mecframework;

import ie.ucd.dempsey.mecframework.service.ServiceController;
import ie.ucd.dempsey.mecframework.servicenode.ServiceNode;
import picocli.CommandLine;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.net.URI;

@CommandLine.Command(name = "ServiceNode Driver", mixinStandardHelpOptions = true, version = "0.1")
public class Main implements Runnable {
    // input parameters
    @Parameters(index = "0", paramLabel = "orchestrator",
            description = "The address of the orchestrator. Format ws://{ip address}:{port}")
    private URI serverUri;
    @Parameters(index = "1", paramLabel = "file", description = "The name of the file storing the service you wish to run.")
    private File serviceFile;
    @Parameters(index = "2", paramLabel = "serviceAddress",
            description = "The address any services will run out of on this machine {ip}:{port}")
    private URI serviceAddress;
    @Parameters(index = "3", paramLabel = "nodeLabel", description = "An identifying name for this Service Node",
            defaultValue = "some-service-node")
    private String nodeLabel;
    @Parameters(index = "4", paramLabel = "latencyDelay", description = "An extra delay added on to the latency" +
            " values collected by this node", defaultValue = "0")
    private int latencyDelay;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        ServiceController mockController = new ServiceController() {
            @Override
            public void startService() {

            }

            @Override
            public boolean isServiceRunning() {
                return false;
            }

            @Override
            public void stopService() {

            }

            @Override
            public boolean serviceExists() {
                return false;
            }

            @Override
            public String name() {
                return null;
            }
        };

        ServiceNode serviceNode = new ServiceNode(serverUri, mockController, serviceFile, nodeLabel, latencyDelay);
        serviceNode.run();  // run instead of start a Thread to stop the program from finishing immediately
    }
}
