package service.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import service.core.*;

import java.net.InetSocketAddress;

/**
 * Static utilities to keep Gson consistent.
 */
public class Gsons {
    private static final RuntimeTypeAdapterFactory<Message> MOBILE_CLIENT_RTA_FACTORY =
            RuntimeTypeAdapterFactory.of(Message.class, "type")
                    .registerSubtype(NodeInfoRequest.class, Message.MessageTypes.NODE_INFO_REQUEST)
                    .registerSubtype(NodeInfo.class, Message.MessageTypes.NODE_INFO)
                    .registerSubtype(HostRequest.class, Message.MessageTypes.HOST_REQUEST)
                    .registerSubtype(HostResponse.class, Message.MessageTypes.HOST_RESPONSE)
                    .registerSubtype(MigrationAlert.class, Message.MessageTypes.MIGRATION_ALERT)
                    .registerSubtype(MigrationSuccess.class, Message.MessageTypes.MIGRATION_SUCCESS)
                    .registerSubtype(ServerHeartbeatRequest.class, Message.MessageTypes.SERVER_HEARTBEAT_REQUEST);

    private static final RuntimeTypeAdapterFactory<Message> SERVICE_NODE_RTA_FACTORY =
            RuntimeTypeAdapterFactory.of(Message.class, "type")
                    .registerSubtype(NodeInfo.class, Message.MessageTypes.NODE_INFO)
                    .registerSubtype(ServerHeartbeatRequest.class, Message.MessageTypes.SERVER_HEARTBEAT_REQUEST)
                    .registerSubtype(ServiceRequest.class, Message.MessageTypes.SERVICE_REQUEST)
                    .registerSubtype(ServiceResponse.class, Message.MessageTypes.SERVICE_RESPONSE)
                    .registerSubtype(NodeInfoRequest.class, Message.MessageTypes.NODE_INFO_REQUEST)
                    .registerSubtype(MigrationAlert.class, Message.MessageTypes.MIGRATION_ALERT)
                    .registerSubtype(MigrationSuccess.class, Message.MessageTypes.MIGRATION_SUCCESS)
                    .registerSubtype(NodeClientLatencyRequest.class, Message.MessageTypes.NODE_CLIENT_LATENCY_REQUEST);

    private static final RuntimeTypeAdapterFactory<Message> ORCHESTRATOR_RTA_FACTORY =
            RuntimeTypeAdapterFactory.of(Message.class, "type")
                    .registerSubtype(NodeInfo.class, Message.MessageTypes.NODE_INFO)
                    .registerSubtype(ServiceRequest.class, Message.MessageTypes.SERVICE_REQUEST)
                    .registerSubtype(ServiceResponse.class, Message.MessageTypes.SERVICE_RESPONSE)
                    .registerSubtype(HostRequest.class, Message.MessageTypes.HOST_REQUEST)
                    .registerSubtype(NodeInfoRequest.class, Message.MessageTypes.NODE_INFO_REQUEST)
                    .registerSubtype(MigrationSuccess.class, Message.MessageTypes.MIGRATION_SUCCESS)
                    .registerSubtype(MobileClientInfo.class, Message.MessageTypes.MOBILE_CLIENT_INFO);

    private Gsons() {
    }

    private static Gson buildGson(RuntimeTypeAdapterFactory<Message> runtimeTypeAdapterFactory) {
        return new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapterFactory(runtimeTypeAdapterFactory)
                .registerTypeAdapter(InetSocketAddress.class, new InetSocketAddressAdapter())
                .create();
    }

    public static Gson mobileClientGson() {
        return buildGson(MOBILE_CLIENT_RTA_FACTORY);
    }

    public static Gson serviceNodeGson() {
        return buildGson(SERVICE_NODE_RTA_FACTORY);
    }

    public static Gson orchestratorGson() {
        return buildGson(ORCHESTRATOR_RTA_FACTORY);
    }
}
