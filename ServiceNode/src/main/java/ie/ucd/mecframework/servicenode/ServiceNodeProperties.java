package ie.ucd.mecframework.servicenode;

import static java.util.Objects.isNull;
import static service.core.Constants.*;

public class ServiceNodeProperties {
    private static ServiceNodeProperties instance;
    private final OS os;

    private ServiceNodeProperties() {
        String osName = System.getProperty("os.name");
        if (osName.contains("Windows")) {
            os = OS.WINDOWS;
        } else if (osName.contains("Linux")) {
            os = OS.LINUX;
        } else {
            throw new RuntimeException("Couldn't determine OS in " + ServiceNodeProperties.class.getSimpleName());
        }
    }

    /**
     * Gets the singleton OrchestratorProperties.
     *
     * @return an OrchestratorProperties object, if one exists already, or was created during this invocation.
     */
    public static ServiceNodeProperties get() {
        if (isNull(instance)) {
            instance = new ServiceNodeProperties();
        }
        return instance;
    }

    public int getAdvertisedServicePortNumber() {
        return (os == OS.WINDOWS) ? ROBERT_PC_SERVICE_PORT : RPI_SERVICE_PORT;
    }

    public int getAdvertisedTransferServerPortNumber1() {
        return (os == OS.WINDOWS) ? ROBERT_PC_TRANSFER_PORT_1 : RPI_TRANSFER_PORT_1;
    }

    public int getAdvertisedTransferServerPortNumber2() {
        return (os == OS.WINDOWS) ? ROBERT_PC_TRANSFER_PORT_2 : RPI_TRANSFER_PORT_2;
    }

    public int getActualTransferServerPortNumber1() {
        return ROBERT_PC_TRANSFER_PORT_1;
    }

    public int getActualTransferServerPortNumber2() {
        return ROBERT_PC_TRANSFER_PORT_2;
    }

    private enum OS {WINDOWS, LINUX}
}
