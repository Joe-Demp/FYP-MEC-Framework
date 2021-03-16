package service.util;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.net.InetSocketAddress;

public class InetSocketAddressAdapter implements JsonSerializer<InetSocketAddress>,
        JsonDeserializer<InetSocketAddress> {

    @Override
    public InetSocketAddress deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String addressString = json.getAsJsonPrimitive().getAsString();
        String[] splitAroundColon = addressString.split(":");
        String address = splitAroundColon[0];
        int port = Integer.parseInt(splitAroundColon[1]);
        return new InetSocketAddress(address, port);
    }

    @Override
    public JsonElement serialize(InetSocketAddress src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.getHostString() + ":" + src.getPort());
    }
}
