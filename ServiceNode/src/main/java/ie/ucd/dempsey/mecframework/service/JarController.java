package ie.ucd.dempsey.mecframework.service;

import ie.ucd.dempsey.mecframework.exceptions.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class JarController implements ServiceController {
    private final Logger logger = LoggerFactory.getLogger(JarController.class);
    private final Path servicePath;
    private AtomicBoolean isServiceRunning = new AtomicBoolean();
    private Process javaProcess;
    private OSRuntime runtime = OSRuntime.get();
    private Executor serviceOutputExecutor = Executors.newSingleThreadExecutor();

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
        });
    }

    private void startJavaProcess() {

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
}
