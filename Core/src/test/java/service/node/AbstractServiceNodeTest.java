package service.node;

import org.junit.jupiter.api.Test;
import service.core.Message;
import service.core.ServiceResponse;

import java.net.InetSocketAddress;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbstractServiceNodeTest {
    static InetSocketAddress socketAddress = new InetSocketAddress("www.irishtimes.com", 80);
    static String uuidString = "29d03298-9e3e-4f74-bb3f-57be11e4140c";
    static UUID someUUID = UUID.fromString(uuidString);
    static String serviceName = "myService";
    AbstractServiceNode serviceNode = new MockAbstractServiceNode();
    Message message = new ServiceResponse(someUUID, someUUID, socketAddress, serviceName);
    String messageJson = "{\n" +
            "  \"targetNodeUuid\": \"29d03298-9e3e-4f74-bb3f-57be11e4140c\",\n" +
            "  \"sourceNodeUuid\": \"29d03298-9e3e-4f74-bb3f-57be11e4140c\",\n" +
            "  \"transferServerAddress\": \"www.irishtimes.com:80\",\n" +
            "  \"serviceName\": \"myService\",\n" +
            "  \"type\": \"ServiceResponse\"\n" +
            "}";

    @Test
    void serviceNodeLoadsCorrectly() {
    }

    @Test
    void gsonSerializesCorrectly() {
        assertEquals(messageJson, serviceNode.gson.toJson(message));
    }

    @Test
    void gsonDeserializesCorrectly() {
        Message deserialized = serviceNode.gson.fromJson(messageJson, Message.class);
        ServiceResponse response = (ServiceResponse) deserialized;

        assertEquals(someUUID, response.getSourceNodeUuid());
        assertEquals(someUUID, response.getTargetNodeUuid());
        assertEquals(socketAddress, response.getTransferServerAddress());
        assertEquals(serviceName, response.getServiceName());
    }
}
