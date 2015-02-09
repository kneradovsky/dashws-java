/*
 * Copyright 2015 SBT-Neradovskiy-KL.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.kn.dashws.ws;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author SBT-Neradovskiy-KL
 */
@ClientEndpoint
public class WSUpstreamConnection {
    String url;
    String[] subscriptions;
    String subs;
    String name;
    Logger logger = LoggerFactory.getLogger(WSUpstreamConnection.class);
    Session sess;
    WebSocketServer wsserver;
    JsonParser parser=new JsonParser();
    
    public WSUpstreamConnection(String name,String wsurl,String subs) {
        url=wsurl;
        if(subs!=null) subscriptions=subs.split(",");
        this.subs=subs;
        this.name=name;
        wsserver=(WebSocketServer)WebSocketServerFactory.getServer();
    }
    public String getURL() {
        return url;
    }
    public String getSubscriptions() {
        return subs;
    }
    
    @OnOpen
    public void onOpen(Session sess) {
        this.sess=sess;
        try {
            Gson gs = new Gson();
            String msg="{\"type\": \"subscribe\",\"data\": {\"events\":"+gs.toJson(subscriptions)+"}}";
            sess.getBasicRemote().sendText(msg);
        } catch(Exception e) {
            logException(e);
        }
    }
    
    @OnMessage
    public void onMessage(String text) {
        logger.debug("Message from "+name+":"+text);
        try {
            JsonObject msg = parser.parse(text).getAsJsonObject();
            String msgtype=msg.get("type").getAsString();
            String target;
            switch(msgtype) {
                case "subscribe":
                case "ack": return; //ignore that message types;
                case "event": target="";break;
                default: target=msgtype;break;
            }
            if(msg.get("data").isJsonArray()) {
                //history received
                JsonArray arr=msg.get("data").getAsJsonArray();
                for(JsonElement el : arr) {
                    if(el.isJsonObject())
                        wsserver.send(null, el.getAsJsonObject(), "");
                    else logger.debug("Invalid event:"+el.toString());
                }
            } else {
                JsonObject body=msg.get("data").getAsJsonObject();
                wsserver.send(null,body,target);
            }
        } catch(Exception e) {
            logException(e);
        }
    }
    
    public boolean connect() {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            if(container.connectToServer(this, URI.create(url))!=null) {
                logger.debug("Connected to upstream "+url);
                return true;
            }
        } catch(Exception e) {
            logger.info("Exception while connecting to "+url);
            logException(e);
        }
        return false;
    }
    public void logException(Throwable e) {
        StringWriter wr=new StringWriter();
        e.printStackTrace(new PrintWriter(wr));
        logger.info("upstream "+name+" exception:"+wr.toString());
    }            
    
}
