# MEC Framework Project

This repo is a fork of [Darragh Clark's Final Year Project Codebase](https://github.com/DarraghClarke/FYP).

The following is a guide toward building and running the applications in this project.

## Core

The Core module is a dependency for the Orchestrator, ServiceNode, and console-app applications. It must be installed before building the other components. To build the Core module:

1. Navigate to the Core directory.
2. Run `mvn install`.

## Orchestrator

Before building, navigate to `service.orchestrator.Main` and choose the required `Trigger` and `Selector`. 

### Build

1. Navigate to the Orchestrator directory.
2. Run `mvn package`.

### Run

1. Navigate to the Orchestrator/target directory.
2. Run the following command:
```
java -jar Orchestrator-0.8.0-jar-with-dependencies.jar <port-number>
```

Where `<port-number>` should be replaced by the port number that you want the Orchestrator's WebSocket to listen on.
 
## ServiceNode

Before building, navigate to `ie.ucd.mecframework.Main` and choose the required `MigrationStrategy` and `ServiceConroller`. 

### Build

1. Navigate to the ServiceNode directory.
2. Run `mvn package`.

### Run

1. Navigate to the ServiceNode/target directory.
2. Run the following command:
```
java -jar ServiceNode-1.0-jar-with-dependencies.jar <orchestrator-URI> <service-file> <service-state> \
    <service-address> <node-label> <latency-delay> <start-service>
```

With the following substitutions:

* `<orchestrator-URI>` - the URI of the Orchestrator e.g. ws://csi420-01-vm1.ucd.ie
* `<service-file>` - the path of the service that you want this ServiceNode to run. This is mandatory even if the
service is not installed. This is the location where the service will be saved if this ServiceNode receives the
service through a service migration. e.g. ../../streaming-sample/streaming-sample-1.jar
* `<service-state>` - the path of the service state that you want this ServiceNode to migrate along with the service
instance. This will not be used for stateless services. e.g. data/streamData.dat
* `<service-address>` - the local address that you expect the service to run on. This application actually only uses the
address's port number. e.g. ws://localhost:8090 ; This will let clients know that the service is running on port 8090.
The Orchestrator will replace the localhost part with the proper address. 
* `<node-label>` - a name for this ServiceNode. e.g. edge-node
* `<latency-delay>` - a delay in milliseconds that will be added to every latency reading. Useful for simulating delays.
e.g. 200
* `<start-service>` - a boolean value indicating whether the service should be started on launch. e.g. true

For example:
```
java -jar ServiceNode-1.0-jar-with-dependencies.jar ws://csi420-01-vm1.ucd.ie \
    ../../streaming-sample/streaming-sample-1.jar \ 
    data/streamData \
    ws://localhost:8090 \
    edge \
    200 \
    true
```
