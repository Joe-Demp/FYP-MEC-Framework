package ie.ucd.dempsey.mecframework.service;

import ie.ucd.dempsey.mecframework.exceptions.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DockerController implements ServiceController {
    private final Logger logger = LoggerFactory.getLogger(DockerController.class);
    private final Path servicePath;
    private AtomicBoolean isServiceRunning = new AtomicBoolean();
    private Process dockerProcess;
    private Runtime runtime = Runtime.getRuntime();

    /**
     * @param servicePath the path to the location where the Controller expects to find a tar file to load into Docker.
     */
    public DockerController(Path servicePath) {
        this.servicePath = servicePath;
    }

    @Override
    public void startService() {
        if (isServiceRunning()) {
            logger.info("startService called while service already running.");
            return;
        }
        startDockerService();
    }

    private void startDockerService() {
        if (!serviceExists()) {
            throw new ServiceException("startService called when no service file exists!");
        }

        loadArchiveServiceIntoDocker();

        // send the Docker run command
        //  log the process output (ideally in a new window)
    }

    private void loadArchiveServiceIntoDocker() {
        Process loadingProcess = startLoadingProcess();
        waitForLoadingProcess(loadingProcess);
        logProcessOutput(loadingProcess);
    }

    private Process startLoadingProcess() {
        String dockerCommand = "powershell.exe docker load --input " + servicePath;
        logger.info("Issuing command {}", dockerCommand);

        try {
            return runtime.exec(dockerCommand);
        } catch (IOException ex) {
            logger.error("Exception in startLoadingProcess", ex);
            throw new ServiceException(ex);
        }
    }

    private void waitForLoadingProcess(Process loading) {
        long timeoutSeconds = 5;
        try {
            boolean processExited = loading.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            logger.info("Waited {} seconds and process finished?={}", timeoutSeconds, processExited);
        } catch (InterruptedException iex) {
            logger.error("Exception in waitForLoadingProcess", iex);
            throw new ServiceException(iex);
        }
    }

    // todo see if this method ever exits
    private void logProcessOutput(Process process) {
        try (
                Scanner standardOutScan = new Scanner(process.getInputStream());
                Scanner standardErrScan = new Scanner(process.getErrorStream())
        ) {
            while (standardOutScan.hasNextLine() || standardErrScan.hasNextLine()) {
                while (standardOutScan.hasNextLine()) {
                    logger.info("docker.stdout: {}", standardOutScan.nextLine());
                }
                while (standardErrScan.hasNextLine()) {
                    logger.info("docker.stderr: {}", standardErrScan.nextLine());
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        logger.info("Finished logging process output");
    }

    @Override
    public boolean isServiceRunning() {
        return isServiceRunning.get();
    }

    @Override
    public void stopService() {
        dockerProcess.destroyForcibly();
    }

    @Override
    public boolean serviceExists() {
        return Files.exists(servicePath);
    }

    @Override
    public String name() {
        return servicePath.getFileName().toString();
    }
}
