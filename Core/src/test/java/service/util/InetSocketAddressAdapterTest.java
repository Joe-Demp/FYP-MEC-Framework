package service.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InetSocketAddressAdapterTest {
    static final String RAW_ADDRESS = "192.168.0.1:80";
    final InetSocketAddress homeRouter = new InetSocketAddress("192.168.0.1", 80);
    final InetSocketAddressAdapter testAdapter = new InetSocketAddressAdapter();

    @Test
    void deserialize() {
        JsonElement iNetSocketAddressJson = new JsonPrimitive(RAW_ADDRESS);
        InetSocketAddress deserialized = testAdapter.deserialize(iNetSocketAddressJson, null, null);

        assertEquals("192.168.0.1", deserialized.getHostString());
        assertEquals(80, deserialized.getPort());
    }

    @Test
    void serialize() {
        JsonElement serialized = testAdapter.serialize(homeRouter, null, null);
        assertEquals("192.168.0.1:80", serialized.getAsString());
    }
}
