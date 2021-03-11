package service.orchestrator.properties;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import static java.util.Objects.isNull;

public class OrchestratorProperties {
    private static final String FILENAME = "orchestrator.properties";
    private static final String DEFAULT_DOUBLE = Double.valueOf(1.0).toString();
    private static OrchestratorProperties instance;
    private Properties properties;

    private OrchestratorProperties() throws IOException {
        properties = new Properties();
        properties.load(new FileInputStream(getFilePath()));
    }

    /**
     * Gets the singleton OrchestratorProperties.
     *
     * @return an OrchestratorProperties object, if one exists already, or was created during this invocation.
     * @throws IOException if the underlying properties file could not be found or if there was a problem while reading
     *                     said properties file.
     */
    public static OrchestratorProperties get() throws IOException {
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
        String maxLatency = (String) properties.getOrDefault("application.limit.max-latency", MAX_VALUE_STRING);
        return Integer.parseInt(maxLatency);
    }

    public double getMaxCpu() {
        String maxCpu = (String) properties.getOrDefault("application.limit.max-cpu", DEFAULT_DOUBLE);
        return Double.parseDouble(maxCpu);
    }

    public double getMaxMemory() {
        String maxCpu = (String) properties.getOrDefault("application.limit.max-memory-used", DEFAULT_DOUBLE);
        return Double.parseDouble(maxCpu);
    }

    public double getMinStorage() {
        final String DOUBLE_ZERO = Double.toString(0.0);
        String maxCpu = (String) properties.getOrDefault("application.limit.min-storage", DOUBLE_ZERO);
        return Double.parseDouble(maxCpu);
    }
}
