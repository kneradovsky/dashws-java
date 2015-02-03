package ru.kn.dashws.ws;

import java.io.EOFException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sql.rowset.spi.SyncResolver;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

class Message {
	String type;
	Map<String,String> data=new HashMap<>();
	public Message(String t) {
		type=t;
	}
}

class MessageList extends Message {
	List<Map<String,String>> data;
	public MessageList(String type) {
		super(type);
	}
}


@ServerEndpoint(value="/websocket/connection")
public class ClientConnection {
	Session ses;
	Integer connid;
	Logger logger = LoggerFactory.getLogger(ClientConnection.class);
	Gson gs = new Gson();
	WebSocketServer wsserver;
	@OnOpen
	public void onOpen(Session session) {
		try {
			ses=session;
			//wsserver=(WebSocketServer)WebSocketServerFactory.getServer();
			//connid=wsserver.add(this);
			//sendText("{\"type\":\"ack\",\"result\":\"ok\"}");
		} catch(Throwable t) {
			logException(t);
		}
 		
	}
	
	@OnMessage
	public void onMessage(String message) {
		try {
		JsonParser parser = new JsonParser();
		String response=null;
		try {
			JsonObject obj = parser.parse(message).getAsJsonObject();
			if(obj.has("type")) {
				switch(obj.get("type").getAsString().toLowerCase()) {
				case "subscribe": response=onSubscribe(obj);break;
				default	: response=notImplemented(obj);break;  
				}
 			}
		} catch(JsonParseException | NullPointerException e) {
			response=format_error(e.getMessage());
		} 
		logger.debug("onMessage ThreadID:"+Thread.currentThread().getId());
		sendText(response);
		} catch(Throwable t) {
			logException(t);
		}
	}
	
	@OnClose
	public void onClose() {
		try {
		wsserver.remove(connid);
		} catch(Throwable t) {
			logException(t);
		}
	}
	
	@OnError
	public void onError(Throwable err) {
		// Most likely cause is a user closing their browser. Check to see if
		// the root cause is EOF and if it is ignore it.
		// Protect against infinite loops.
		int count = 0;
		Throwable root = err;
		while (root.getCause() != null && count < 20) {
			root = root.getCause();
			count++;
		}
		if (root instanceof EOFException) {
			logException(err);
		} else {
			StringWriter wr=new StringWriter();
			err.printStackTrace(new PrintWriter(wr));
			logger.info("ws connection exception:"+wr.toString());
		}
	}
	
	public String format_error(String err) {
		Message msg = new Message("error");
		msg.data.put("message", err);
		return gs.toJson(msg);
	}
	public String format_event(JsonElement body,String target) {
		String evttype =  target == null ? "event" : target;
		JsonObject msg=new JsonObject();
		msg.addProperty("type", evttype);
		msg.add("data", body);
		return gs.toJson(msg);
	}
	
	public String format_event(JsonElement body) {
		return format_event(body,null);
	}
	
	public synchronized void sendText(String msg) {
		try {
			logger.debug("sendText: in");
			ses.getBasicRemote().sendText(msg);
			//ses.getBasicRemote().getSendWriter().write(msg);
			//ses.getBasicRemote().getSendWriter().flush();
			logger.debug("ses:"+ses.getId());
			logger.debug("sendText ThreadID:"+Thread.currentThread().getId());
		} catch (IOException e) {
			logException(e);
		} catch(Throwable t) {
			logException(t);
		}
	}
	
	public String notImplemented(JsonObject obj) {
		return format_error("not implemented");
	}
	
	public String onSubscribe(JsonObject obj) {
		try {
			JsonElement events=obj.get("data").getAsJsonObject().get("events");
			final List<String> subs_ids=new ArrayList<>();
			for(JsonElement jsid : events.getAsJsonArray()) {
				subs_ids.add(jsid.getAsString());
			}
			List<JsonObject> history=new ArrayList<>();
			//List<JsonObject> history=wsserver.subscribe(connid,subs_ids);
			JsonArray objdata=new JsonArray();
			for(JsonObject objelem: history) 
				objdata.add(objelem);
			String msg=format_event(objdata);
			return msg;

			
		} catch(NullPointerException e) {
			String errmsg=format_error("invalid subscription message");
			logException(e);
			return errmsg;
		} 
 	}
	
	public void logException(Throwable e) {
		StringWriter wr=new StringWriter();
		e.printStackTrace(new PrintWriter(wr));
		logger.info("ws send exception:"+wr.toString());
	}
}
