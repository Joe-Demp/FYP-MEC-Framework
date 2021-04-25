package ie.ucd.mecframework.service;

public interface ServiceController {
    void startService();

    boolean isServiceRunning();

    /**
     * Stops the service if it is running. Otherwise does nothing.
     */
    void stopService();

    /**
     * @return {@code true} if this {@code ServiceController} holds a reference to an instance of a service that it can
     * run.
     */
    boolean serviceExists();

    /**
     * @return The name of the service running at present, or the empty string if no service is running.
     */
    String name();

    /**
     * Cleans up any Threads, executors, etc.
     */
    void shutdown();
}
