package ru.kn.dashws.ws;

import java.io.EOFException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
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
	@OnOpen
	public void onOpen(Session session) {
		ses=session;
		connid=WebSocketServer.add(ses);
	}
	
	@OnMessage
	public void onMessage(String message) {
		JsonParser parser = new JsonParser();
		try {
			JsonObject obj = parser.parse(message).getAsJsonObject();
			if(obj.has("type")) {
				switch(obj.get("type").getAsString().toLowerCase()) {
				case "subscribe": onSubscribe(obj);break;
				default	: notImplemented(obj);break  
				}
 			}
		} catch(JsonParseException | NullPointerException e) {
			ses.getBasicRemote().sendText(format_error(e.getMessage()));
		}
	}
	
	@OnClose
	public void onClose() {
		WebSocketServer.remove(connid);
	}
	
	@OnError
	public void onError(Throwable err) {
		// Most likely cause is a user closing their browser. Check to see if
		// the root cause is EOF and if it is ignore it.
		// Protect against infinite loops.
		int count = 0;
		Throwable root = t;
		while (root.getCause() != null && count < 20) {
			root = root.getCause();
			count++;
		}
		if (root instanceof EOFException) {
			// Assume this is triggered by the user closing their browser and
			// ignore it.
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
	public String format_event(Map<String,String> body,String target) {
		String evttype =  target == null ? "event" : target;
		Message msg = new Message(evttype);
		evttype.data=body;
		return gs.toJson(msg);
	}
	
	public String format_event(JsonObject elem) {
		return format_event(elem,null);
	}
}
