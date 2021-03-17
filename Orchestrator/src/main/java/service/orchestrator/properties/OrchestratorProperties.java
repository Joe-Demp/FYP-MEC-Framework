package service.orchestrator.properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.MissingResourceException;
import java.util.Properties;

import static java.util.Objects.isNull;

public class OrchestratorProperties {
    private static final Logger logger = LoggerFactory.getLogger(OrchestratorProperties.class);
    private static final String FILENAME = "orchestrator.properties";
    private static final String DEFAULT_DOUBLE = Double.valueOf(1.0).toString();
    private static OrchestratorProperties instance;
    private Properties properties;

    private OrchestratorProperties() {
        properties = new Properties();

        try {
            properties.load(new FileInputStream(getFilePath()));
        } catch (IOException ioe) {
            logger.error(ioe.getMessage());
            ioe.printStackTrace();
            throw new MissingResourceException(
                    String.format("No Orchestrator properties file found: %s", ioe.getMessage()),
                    OrchestratorProperties.class.getSimpleName(),
                    "orchestrator.properties"
            );
        }
    }

    /**
     * Gets the singleton OrchestratorProperties.
     *
     * @return an OrchestratorProperties object, if one exists already, or was created during this invocation.
     */
    public static OrchestratorProperties get() {
        if (isNull(instance)) {
            instance = new OrchestratorProperties();
        }
        return instance;
    }

    private String getFilePath() throws IOException {
        URL fileUrl = getClass().getClassLoader().getResource(FILENAME);
        if (isNull(fileUrl)) {
            throw new IOException("OrchestratorProperties file not found");
        }
        return fileUrl.getPath();
    }

    public int getMaxLatency() {
        final String MAX_VALUE_STRING = Integer.toString(Integer.MAX_VALUE);
        String maxLatency = properties.getProperty("application.limit.max-latency", MAX_VALUE_STRING);
        return Integer.parseInt(maxLatency);
    }

    public double getMaxCpu() {
        String maxCpu = properties.getProperty("application.limit.max-cpu", DEFAULT_DOUBLE);
        return Double.parseDouble(maxCpu);
    }

    public double getMaxMemory() {
        String maxCpu = properties.getProperty("application.limit.max-memory-used", DEFAULT_DOUBLE);
        return Double.parseDouble(maxCpu);
    }

    public double getMinStorage() {
        final String DOUBLE_ZERO = Double.toString(0.0);
        String maxCpu = properties.getProperty("application.limit.min-storage", DOUBLE_ZERO);
        return Double.parseDouble(maxCpu);
    }

    public int getClientPingServerPort() {
        String clientPingServerPort = properties.getProperty("client.pingserver.port", "8092");
        return Integer.parseInt(clientPingServerPort);
    }

    public long getHeartbeatPeriod() {
        String heartbeatFrequency = properties.getProperty("orchestrator.heartbeat.frequency", "10");
        return Long.parseLong(heartbeatFrequency);
    }
}
