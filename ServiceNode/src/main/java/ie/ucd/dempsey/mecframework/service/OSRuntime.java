package ie.ucd.dempsey.mecframework.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static java.util.Objects.isNull;

public class OSRuntime {
    private static final Logger logger = LoggerFactory.getLogger(OSRuntime.class);
    private static OSRuntime osRuntime;
    private Runtime runtime = Runtime.getRuntime();
    private boolean isWindows;
    private boolean isLinux;

    private OSRuntime() {
        String osName = System.getProperty("os.name");
        if (osName.contains("Windows")) {
            isWindows = true;
        } else if (osName.contains("Linux")) {
            isLinux = true;
        }
    }

    public static OSRuntime get() {
        if (isNull(osRuntime)) {
            osRuntime = new OSRuntime();
        }
        return osRuntime;
    }

    /**
     * Sends a command to the {@code Runtime} and returns the {@code Process} that launches.
     * On Windows, this method will direct the command to powershell.exe.
     *
     * <p>
     * This method delegates to {@code Runtime.exec(String)}. See that javadoc for more information.
     * </p>
     *
     * @param command the command you wish to issue to the {@code Runtime}, as you would enter into a terminal.
     * @return the {@code Process} object, representing the process launched by the Runtime.
     * @throws IOException if an I/O Error occours.
     */
    public Process exec(String command) throws IOException {
        String osCommand = getOSCommand(command);
        logger.info("Executing {}", osCommand);
        return runtime.exec(osCommand);
    }

    private String getOSCommand(String command) {
        if (isWindows) {
            return "powershell.exe " + command;
        } else if (isLinux) {
            return command;
        } else {
            throw new RuntimeException("Unidentified OS in OSRuntime");
        }
    }

}
