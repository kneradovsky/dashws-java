# dashws-java
Java implementation of the external web socket server for [dashenee](https://github.com/kneradovsky/dashenee) dashboard

## Installation
1. Build the war using maven or gradle.

**Maven**
```
mvn package
```

**Gradle**
```
gradle assemble
```

2. Deploy the war file to your favourite servlet container. (e.g. Tomcat, GlassFish, Jetty). Dashws uses the annotations to configure servlets, so the servlet container should support them (Tomcat >7.0, GlassFish >3.0, Jetty >8.0).

## Configuration
The dashws accepts the following context parameters:

- **auth_token** Sets the authentication token for the post requests from data sources
- __upstream*Name*__ Defines the WS upstream url, *Name* is the name of the upstream
- __subscribe*Name*__ Defines the subscriptions for the upstream *Name* 

## URL Mappings
The dashws defines the following url mappings relative to its context
- /data/dataid - accepts post request from *dataid* datasource.
- /dashboards/dashboardid - sends command to reload *dashboardid* to the all connected clients. *Dashboardid* could be * which means all dashboards. 
- /upstreams - displays the list of the connected upstreams.
- /websocket/connection - the Web Socket endpoint of the dashws. 

## The Protocol

### Data sources
A data source with ID=*DATAID* has to send post request to the /data/DATAID uri. 
Request requirements: 

- The request body has to be well formed JSON object. 
- JSON object has to have *auth_token* property
- JSON object could have any childred of any valid JSON type (simple types, object, list)   

### WebSocket clients 

1. Just after client connects,  the server sends 'ack' response:
```JSON
{"type":"ack","result":"ok"}
```

2. Client receives the 'ack' and sends subsribe request with list of the IDs of the data sources in the data.events property: 
```JSON
{"type":"subscribe", "data":{"events":["id1","id2"]}}
```

3. Server processes the request and responds the list of the latest events for the subscribed data sources:
```JSON
{"type":"event","data":[{"id":"id1","value":"test","temp":"100F"},{"id":"id2","value":"shutdown","rpm":"0"}]}
```

4. On data event from datasource *dataid* the server sends the data to the clients that have subscribed for the events from that data source:
```JSON
{"type":"event","data":{"id":"dataid","value": "ok","temp":"200F"}}
```
The format of the data is defined by the datasource *dataid*


#### Sample dataflow.
Packets marked by ">>>" are sent from the dashws server, packets marked by "<<<" are sent to the dashws server
```JSON
>>> {"type":"ack","result":"ok"}
<<< {"type":"subscribe", "data":{"events":["id1","id2"]}}
>>> {"type":"event","data":[{"id":"id1","value":"test","temp":"100F"},{"id":"id2","value":"shutdown","rpm":"0"}]}
.....
>>> {"type":"event","data":{"id":"id1","value": "ok","temp":"200F"}}
.....
>>> {"type":"event","data":{"id":"id2","value": "ok","rpm":"1010"}}
.....
```

