package service.orchestrator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import service.orchestrator.migration.LatencyTrigger;
import service.orchestrator.migration.Selector;
import service.orchestrator.migration.SimpleSelector;
import service.orchestrator.migration.Trigger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@CommandLine.Command(name = "cmMain", mixinStandardHelpOptions = true, version = "0.8")
public class Main implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    @Option(names = {"--RollingAverage"}, defaultValue = "80", paramLabel = "Rolling Average", description = "The value that should be used in the rolling average, format: input 80 for 80/20 rolling average, Defaults to 80")
    int rollingAverage;

    @Option(names = {"-s", "--secure"},
            description = "Secure mode, only engages with orchestrators using SSL")
    private boolean secure;

    @Parameters(index = "0", paramLabel = "port", description = "The port the orchestrator should run on")
    private int port;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    private static final ScheduledExecutorService scheduledService = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void run() {
        if (!secure) {
            // todo refactor ( long and wordy )
            logger.info("Starting Orchestrator");
            Selector selector = new SimpleSelector();
            Orchestrator orchestrator = new Orchestrator(port, selector);
            Trigger trigger = new LatencyTrigger(selector, orchestrator);
            scheduledService.scheduleAtFixedRate(trigger, 5, 5, TimeUnit.SECONDS);
            orchestrator.run();
        } else {
            try {
                new SecureOrchestrator(port, rollingAverage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
