package service.core;

public interface Constants {
	int MOBILE_PING_SERVER_PORT = 8071;

	// Note: this has to be constant to get past firewalls
	/*
	 * todo note there is an issue coming down the line: Robert-PC has ports 8090-8100 exposed.
	 *  Need TransferServer on port 8095!
	 * */
	int TRANSFER_SERVER_PORT = 8085;

	int SERVICE_PORT = 8090;
}
