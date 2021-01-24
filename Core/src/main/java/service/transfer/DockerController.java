package service.transfer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;

// todo change name to DockerConnector
public class DockerController {
    private static final Logger logger = LoggerFactory.getLogger(DockerController.class);
    Process pr;
    private Runtime runtime = Runtime.getRuntime();

    // todo remove this
    private boolean volatileIsDockerRunning = false;

    public void launchServiceOnNode(File newService) {
        //todo shutdown any old service before loading new one
        //load new service into docker
        try {
            Process loadProcess = loadArchiveServiceIntoDocker(newService);
            logProcessOutput(loadProcess);

            Process runProcess = sendDockerRunSampleCommand();
            logProcessOutput(runProcess);
        } catch (IOException | InterruptedException e) {
            logger.error("");
            e.printStackTrace();
        }

        volatileIsDockerRunning = true;
    }

    private Process loadArchiveServiceIntoDocker(File newService) throws InterruptedException, IOException {
        logger.info("Loading service file from {} into Docker", newService.getAbsolutePath());
        Process process = runtime.exec("powershell.exe docker load --input service.tar");
        Thread.sleep(5000);
        logger.info("Waking after 5 second sleep. Docker should have loaded file.");
        return process;
    }

    /**
     * todo make this generic
     */
    private Process sendDockerRunSampleCommand() throws IOException {
        logger.info("Asking docker to run container 'sample'");
        return runtime.exec("powershell.exe docker run sample");
    }

    public BufferedReader sendInput(String input) throws IOException {
        pr = runtime.exec(input);
        return new BufferedReader(new InputStreamReader(pr.getInputStream()));
    }

    private void logProcessOutput(Process process) throws InterruptedException {
        logger.info("Waiting for process to stop");
        int status = process.waitFor();
        logger.info("Process finished with status {}", status);

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

    /**
     * todo implement
     *
     * @return true if Docker is running on this host, otherwise false
     */
    public boolean isDockerRunning() {
        // todo make this platform dependent for the moment
        //  use the docker-java API in future: https://github.com/docker-java/docker-java
        //  https://javadoc.io/doc/com.github.docker-java/docker-java/2.1.1/index.html

        // could check this by trying to connect to Docker's port

        // todo this was changed to true to stop the application blocking. Change it back soon
        return volatileIsDockerRunning;
    }
}
