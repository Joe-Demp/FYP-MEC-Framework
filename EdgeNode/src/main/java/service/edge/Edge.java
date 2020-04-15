package service.edge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import service.core.*;
import service.edge.transferServices.TransferClient;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class Edge extends WebSocketClient {
    SystemInfo nodeSystem = new SystemInfo();
    HardwareAbstractionLayer hal = nodeSystem.getHardware();
    CentralProcessor processor = hal.getProcessor();
    GlobalMemory memory= hal.getMemory();
    DockerController dockerController;
    private File service;
    private UUID assignedUUID;
    private Map<Integer,Double> historicalCPUload = new HashMap<>();
    private Map<Integer,Double> historicalRamload = new HashMap<>();

    public Edge(URI serverUri,boolean trustWorthy){//, File service) {
        super(serverUri);
        dockerController=new DockerController();
        //this.service = service;//service is stored in edge node
    }

    public static void main(String[] args) throws URISyntaxException {
        Edge edge=new Edge(new URI( "ws://localhost:443" ),false);
        edge.run();
    }

    public void serviceRequestor() {
        Gson gson = new Gson();

        ServiceRequest serviceRequest = new ServiceRequest(assignedUUID,"docker.tar");//atm assumes there is only 1 service and leaves it up to orchestrator to find it
        String jsonStr = gson.toJson(serviceRequest);
        while(historicalCPUload.size()<5 &&historicalRamload.size()<5) {
            System.out.println(historicalCPUload.size());
        }
        send(jsonStr);
        System.out.println(serviceRequest.getType());
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        System.out.println("connected to orchestrator");
        System.out.println(Edge.this.getLocalSocketAddress());//this is the local address in theory
        getCPULoad();
        getRamLoad();
    }

    @Override
    public void onMessage(String message) {
        RuntimeTypeAdapterFactory<Message> adapter = RuntimeTypeAdapterFactory
                .of(Message.class, "type")
                .registerSubtype(NodeInfo.class, Message.MessageTypes.NODE_INFO)
                .registerSubtype(Service.class, Message.MessageTypes.SERVICE)
                .registerSubtype(ServiceRequest.class, Message.MessageTypes.SERVICE_REQUEST)
                .registerSubtype(ServerHeartbeatRequest.class, Message.MessageTypes.SERVER_HEARTBEAT_REQUEST)
                .registerSubtype(ServiceResponse.class, Message.MessageTypes.SERVICE_RESPONSE)
                .registerSubtype(NodeInfoRequest.class, Message.MessageTypes.NODE_INFO_REQUEST);

        Gson gson = new GsonBuilder().setPrettyPrinting().registerTypeAdapterFactory(adapter).create();

        Message messageObj = gson.fromJson(message, Message.class);

        System.out.println(messageObj.getType());
        System.out.println(message);

        //this routes inbound messages based on type and then moves them to other methods
        switch (messageObj.getType()) {
            case Message.MessageTypes.NODE_INFO_REQUEST:
                NodeInfoRequest infoRequest = (NodeInfoRequest) messageObj;
                assignedUUID = infoRequest.getAssignedUUID();
                sendHeartbeatResponse();
                serviceRequestor();
                break;
            case Message.MessageTypes.SERVER_HEARTBEAT_REQUEST:
                sendHeartbeatResponse();
                break;
            case Message.MessageTypes.SERVICE_REQUEST:
                gson = new Gson();
                Service serviceToReturn = new Service(assignedUUID, service);
                String jsonStr = gson.toJson(serviceToReturn);
                send(jsonStr);
                break;
            case Message.MessageTypes.SERVICE_RESPONSE:
                //this gives the proxy address we want
                ServiceResponse response = (ServiceResponse) messageObj;
                System.out.println(response);
                System.out.println(response.getServiceOwnerAddress());

                try {
                    launchTransferClient(response.getServiceOwnerAddress());

                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    public void sendHeartbeatResponse(){
        Gson gson = new Gson();
        NodeInfo nodeInfo = new NodeInfo(assignedUUID, null ,null);
        if (!historicalCPUload.isEmpty()){
            nodeInfo.setCPUload(historicalCPUload);
        }
        if(!historicalRamload.isEmpty()){
            nodeInfo.setRamLoad(historicalRamload);
        }
        String jsonStr = gson.toJson(nodeInfo);
        send(jsonStr);
    }

    public void launchTransferClient(String serverAddress) throws URISyntaxException {
        System.out.println("GOT HERE");
        System.out.println(serverAddress);
        TransferClient transferClient = new TransferClient(new URI("ws://localhost:6969"),dockerController);
        transferClient.connect();

    }

    public void getSystemSpecs() {
        HardwareAbstractionLayer hal = nodeSystem.getHardware();
        System.out.println("processor" + hal.getProcessor().toString());
    }

    public void getCPULoad(){
        new Timer().schedule(
                new TimerTask() {
                    int secondcounter=0;
                    @Override
                    public void run() {
                        secondcounter++;
                        historicalCPUload.put(secondcounter,processor.getSystemCpuLoadBetweenTicks() * 100);
                    }
                }, 0, 1000);

    }

    public void getRamLoad(){
        new Timer().schedule(
                new TimerTask() {
                    int secondcounter=0;
                    @Override
                    public void run() {
                        secondcounter++;
                        historicalRamload.put(secondcounter, (double) ((memory.getAvailable() / memory.getTotal()) * 100));
                    }
                }, 0, 1000);
    }



    @Override
    public void onClose(int i, String s, boolean b) {

    }

    @Override
    public void onError(Exception e) {

    }
}
