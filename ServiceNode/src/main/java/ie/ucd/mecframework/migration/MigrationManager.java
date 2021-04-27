package ie.ucd.mecframework.migration;

import java.net.InetSocketAddress;

public class MigrationManager {
    private MigrationStrategy strategy;

    public MigrationManager(MigrationStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Stops the running service, launches a {@code TransferServer} and returns the {@code InetSocketAddress} that the
     * {@code TransferClient} on the target node can use to connect to the {@code TransferServer}.
     *
     * @return the address that the {@code TransferClient} can use to connect to the {@code TransferServer}.
     * The {@code TransferServer} port number might not be the same as the advertised port number because of NAT rules.
     */
    public InetSocketAddress migrateService() {
        return strategy.migrateService();
    }

    /**
     * Makes this node set up a {@code TransferClient} and waits for the client to finish accepting the migrated service.
     */
    public void acceptService(InetSocketAddress serverAddress) {
        strategy.acceptService(serverAddress);
    }
}
