package service.orchestrator.properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
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
            properties.load(getFileStream());
        } catch (IOException ioe) {
            logger.error("Couldn't load " + FILENAME, ioe);
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

    private InputStream getFileStream() {
        return getClass().getClassLoader().getResourceAsStream(FILENAME);
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
        String maxMemory = properties.getProperty("application.limit.max-memory", DEFAULT_DOUBLE);
        return Double.parseDouble(maxMemory);
    }

    public double getMinMemoryGibibytes() {
        String minMemory = properties.getProperty("application.limit.min-memory-gb", "0.0");
        return Double.parseDouble(minMemory);
    }

    public long getHeartbeatPeriod() {
        String heartbeatFrequency = properties.getProperty("orchestrator.heartbeat.frequency", "10");
        return Long.parseLong(heartbeatFrequency);
    }
}
