package service.orchestrator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Parameters;
import service.orchestrator.migration.*;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@CommandLine.Command(name = "cmMain", mixinStandardHelpOptions = true, version = "0.8")
public class Main implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final ScheduledExecutorService scheduledService = Executors.newSingleThreadScheduledExecutor();
    @Parameters(index = "0", paramLabel = "port", description = "The port the orchestrator should run on")
    private int port;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        logger.info("Starting Orchestrator");
        Selector selector = getSelector();
        Orchestrator orchestrator = new Orchestrator(port, selector);
        Trigger trigger = getTrigger(selector, orchestrator);
        scheduledService.scheduleAtFixedRate(trigger, 5, 5, TimeUnit.SECONDS);
        orchestrator.run();
    }

    private Selector getSelector() {
//        return new SimpleSelector();
//        return new LatencySelector();
//        return new CpuSelector();
        return new HighAvailabilitySelector(new LatencySelector());
    }

    private Trigger getTrigger(Selector selector, Orchestrator orchestrator) {
//        return new LatencyTrigger(selector, orchestrator);
//        return new CpuTrigger(selector, orchestrator);

        DeferredMigrator deferredMigrator = new DeferredMigrator();
        Trigger latency = new LatencyTrigger(selector, deferredMigrator);
        Trigger cpu = new CpuTrigger(selector, deferredMigrator);

        return new CombinedTrigger(selector, orchestrator, deferredMigrator, cpu, latency);
    }
}
