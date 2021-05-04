package ie.ucd.mecframework.service;

import ie.ucd.mecframework.exceptions.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DockerController implements ServiceController {
    private final Logger logger = LoggerFactory.getLogger(DockerController.class);
    private final Path servicePath;
    private final AtomicBoolean isServiceRunning = new AtomicBoolean();
    private Process dockerProcess;
    private final OSRuntime runtime = OSRuntime.get();
    private final ExecutorService serviceOutputExecutor = Executors.newSingleThreadExecutor();

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
        issueDockerRunCommand();
        // send the Docker run command
        //  log the process output (ideally in a new window)
    }

    private void loadArchiveServiceIntoDocker() {
        Process loadingProcess = startLoadingProcess();
        waitForLoadingProcess(loadingProcess);
        logProcessOutput(loadingProcess);
    }

    private Process startLoadingProcess() {
        String dockerCommand = "docker load --input " + servicePath;

        try {
            return runtime.exec(dockerCommand);
        } catch (IOException ex) {
            logger.error("Exception in startLoadingProcess", ex);
            throw new ServiceException(ex);
        }
    }

    private void waitForLoadingProcess(Process loading) {
        long timeoutSeconds = 20;
        try {
            boolean processExited = loading.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            logger.info("Waited up to {} seconds and loading finished?={}", timeoutSeconds, processExited);
        } catch (InterruptedException iex) {
            logger.error("Exception in waitForLoadingProcess", iex);
            throw new ServiceException(iex);
        }
    }

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

    private void issueDockerRunCommand() {
        startDockerRunProcess();
        startServiceOutputThread(dockerProcess);
    }

    /**
     * Starts a Docker container using the image accessible to this Controller. Maps port 8080 from the container to
     * port 8090 on this machine.
     */
    private void startDockerRunProcess() {
        // Running on port 8090. Has to be the same as the port in ServiceNode.
        String runCommand = "docker run -p 8090:8080 " + dockerImageName();
        try {
            dockerProcess = runtime.exec(runCommand);
            isServiceRunning.set(true);
        } catch (IOException e) {
            logger.error("Problem with " + runCommand, e);
            throw new ServiceException(e);
        }
    }

    private void startServiceOutputThread(Process process) {
        serviceOutputExecutor.execute(() -> {
            Logger logger = LoggerFactory.getLogger("ServiceOutput");

            try (InputStream input = process.getInputStream();
                 Scanner scan = new Scanner(input)) {
                while (process.isAlive()) {
                    logger.info(scan.nextLine());
                }
            } catch (IOException ioe) {
                logger.info("Problem while logging service output", ioe);
                throw new ServiceException(ioe);
            } catch (NoSuchElementException nsee) {
                logger.info("dockerProcess seems to have stopped. DockerController does not know this.", nsee);
            }
        });
    }

    private String dockerImageName() {
        final String TAR = ".tar";
        final String LATEST = ":latest";

        String filename = servicePath.getFileName().toString();
        logger.debug(servicePath.toString());
        logger.debug(filename);

        if (filename.endsWith(".tar")) {
            return filename.substring(0, filename.length() - TAR.length()) + LATEST;
        }
        logger.debug("DockerController.dockerImageName()={}", filename + LATEST);
        return filename + LATEST;
    }

    @Override
    public boolean isServiceRunning() {
        return isServiceRunning.get();
    }

    /**
     * At present, this method does nothing. We cannot stop the Docker container without knowing the container id.<br>
     * todo fix this issue once using a proper Docker connector.
     */
    @Override
    public void stopService() {
        logger.info("Trying to stop the Docker process. dockerProcess.isAlive?={}", dockerProcess.isAlive());
        dockerProcess.destroyForcibly();
        isServiceRunning.set(false);
    }

    @Override
    public boolean serviceExists() {
        logger.debug("Checking Files.exists({})?={}", servicePath, Files.exists(servicePath));
        return Files.exists(servicePath);
    }

    @Override
    public String name() {
        logger.debug("DockerController.name()={}", servicePath.getFileName().toString());
        return servicePath.getFileName().toString();
    }

    @Override
    public void shutdown() {
        serviceOutputExecutor.shutdown();
    }
}
