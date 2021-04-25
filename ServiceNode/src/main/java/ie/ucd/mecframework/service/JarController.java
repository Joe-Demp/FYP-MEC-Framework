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
import java.util.concurrent.atomic.AtomicBoolean;

public class JarController implements ServiceController {
    private final Logger logger = LoggerFactory.getLogger(JarController.class);
    private final Path servicePath;
    private AtomicBoolean isServiceRunning = new AtomicBoolean();
    private Process javaProcess;
    private OSRuntime runtime = OSRuntime.get();
    private ExecutorService serviceOutputExecutor = Executors.newSingleThreadExecutor();

    public JarController(Path servicePath) {
        this.servicePath = servicePath;
    }

    @Override
    public void startService() {
        if (isServiceRunning()) {
            logger.info("startService called while service already running.");
            return;
        }
        startJarService();
    }

    private void startJarService() {
        if (!serviceExists()) {
            throw new ServiceException("startService called when no service file exists!");
        }
        issueJavaJarCommand();
    }

    private void issueJavaJarCommand() {
        startJavaProcess();
        startServiceOutputThread(javaProcess);
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
                logger.info("javaProcess seems to have stopped. JarController does not know this.", nsee);
            }
            logger.info("process.isAlive()={}", process.isAlive());
        });
    }

    private void startJavaProcess() {
        String command = "java -jar " + servicePath;
        try {
            javaProcess = runtime.exec(command);
            isServiceRunning.set(true);
        } catch (IOException ioe) {
            logger.error("Problem with " + command, ioe);
            throw new ServiceException(ioe);
        }
    }

    @Override
    public boolean isServiceRunning() {
        return isServiceRunning.get();
    }

    @Override
    public void stopService() {
        if (isServiceRunning()) return;

        logger.info("Trying to stop the Java process. javaProcess.isAlive?={}", javaProcess.isAlive());
        javaProcess.destroyForcibly();
        try {
            javaProcess.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        isServiceRunning.set(false);
    }

    @Override
    public boolean serviceExists() {
        logger.debug("Checking Files.exists({})?={}", servicePath, Files.exists(servicePath));
        return Files.exists(servicePath);
    }

    @Override
    public String name() {
        logger.debug("JarController.name()={}", servicePath.getFileName().toString());
        return servicePath.getFileName().toString();
    }

    @Override
    public void shutdown() {
        serviceOutputExecutor.shutdown();
    }
}
