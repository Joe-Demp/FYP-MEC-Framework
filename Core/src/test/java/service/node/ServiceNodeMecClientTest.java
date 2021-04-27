package service.node;

import ie.ucd.mecframework.messages.migration.ServiceResponse;
import org.junit.jupiter.api.Test;
import service.core.Message;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServiceNodeMecClientTest {
    static List<InetSocketAddress> socketAddress = List.of(new InetSocketAddress("www.irishtimes.com", 80));
    static String uuidString = "29d03298-9e3e-4f74-bb3f-57be11e4140c";
    static UUID someUUID = UUID.fromString(uuidString);
    static String serviceName = "myService";
    ServiceNodeMecClient serviceNode = new MockServiceNodeMecClient();
    Message message = new ServiceResponse(someUUID, someUUID, socketAddress, serviceName);
    String messageJson = "{\n" +
            "  \"targetUuid\": \"29d03298-9e3e-4f74-bb3f-57be11e4140c\",\n" +
            "  \"sourceUuid\": \"29d03298-9e3e-4f74-bb3f-57be11e4140c\",\n" +
            "  \"transferServerAddresses\": [\n" +
            "    \"www.irishtimes.com:80\"\n" +
            "  ],\n" +
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

        assertEquals(someUUID, response.getSourceUuid());
        assertEquals(someUUID, response.getTargetUuid());
        assertEquals(socketAddress, response.getTransferServerAddresses());
        assertEquals(serviceName, response.getServiceName());
    }
}
