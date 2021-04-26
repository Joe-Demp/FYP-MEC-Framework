package service.core;

public interface Constants {
	int MOBILE_PING_SERVER_PORT = 8071;

	// Robert-PC Port Numbers
	int ROBERT_PC_SERVICE_PORT = 8090;
	int ROBERT_PC_TRANSFER_PORT_1 = 8091;
	int ROBERT_PC_TRANSFER_PORT_2 = 8092;

	/*
		Note that the Raspberry Pi exposes the same ports as Robert-PC on the subnet.
		The Raspberry Pi should use ports 8090-8092 and the subnet router should map ports 8093-8095 to those on the
		Raspberry Pi.

		This way, the service migrates and still uses the same port. The client uses the "global" IP address which
		gets forwarded to the correct port on the correct host, via the router.
	 */
	// Raspberry Pi Port Numbers
	int RPI_SERVICE_PORT = 8093;
	int RPI_TRANSFER_PORT_1 = 8094;
	int RPI_TRANSFER_PORT_2 = 8095;
}
