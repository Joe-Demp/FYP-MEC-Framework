package service.transfer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;

public class DockerController {
    private static final Logger logger = LoggerFactory.getLogger(DockerController.class);
    Process pr;
    private Runtime rt = Runtime.getRuntime();

    public void launchServiceOnNode(File newService) {
        //todo shutdown any old service before loading new one
        //load new service into docker
        try {
            logger.info("in the launch phase2 " + newService.getName() + " and its at " + newService.getAbsolutePath());
            rt.exec("docker load < service.tar");
            Thread.sleep(5000);
            //System.out.println("in the launch phas3");
            pr = rt.exec("docker run sample");//todo make generic
            //System.out.println("in the launch phas4");
            //This while loop prints the output of the docker image a real file would be different
            logger.info("TIME AT DOCKER LAUNCH " + Instant.now());

            logDockerContainerOutput(pr);
        } catch (IOException | InterruptedException e) {
            logger.error("");
            e.printStackTrace();
        }
    }

    public BufferedReader sendInput(String input) throws IOException {
        pr = rt.exec(input);
        return new BufferedReader(new InputStreamReader(pr.getInputStream()));
    }

    private void logDockerContainerOutput(Process process) throws IOException {
        while (true) {
            BufferedReader r = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String line = r.readLine();
            logger.info("dc :: {}", line);

//            if (line != null) {
//                logger.info("dc :: {}", line);
//            }
        }
    }
}
