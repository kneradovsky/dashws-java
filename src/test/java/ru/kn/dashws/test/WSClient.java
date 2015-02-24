package ru.kn.dashws.test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

@ClientEndpoint
public class WSClient {
	static final int TIMEOUT=5000;
	static final String AUTH_TOKEN="token";
	Semaphore onOpen,onMessage,onClose,onError;
	Session sess;
	BlockingQueue<String> messages = new LinkedBlockingQueue<>();
	public WSClient() {
	}
	@OnOpen
	public void onOpen(Session sess) throws InterruptedException {
		this.sess = sess;
		messages.put("onopen");
	}
	@OnMessage
	public void onMessage(String msg) throws InterruptedException {
		messages.put(msg);
	}
	@OnClose
	public void onClose(CloseReason reason) throws InterruptedException {
		messages.put("onclose:"+reason.getReasonPhrase());
	}

	public void onError(Throwable err) throws InterruptedException {
		messages.put("onerror:"+err.getMessage());
	}
	
	public void sendMessage(String data) throws IOException {
		sess.getBasicRemote().sendText(data);
	}
	public void close() throws IOException {
		sess.close();
	}
}

class Message { 
	String type;
	Map<String,String> data;
}

class LastEventsMessage {
	String type;
	List<Map<String,String>> data;
}

class AdvertisingMessage {
	String type;
	List<String> data;
}

