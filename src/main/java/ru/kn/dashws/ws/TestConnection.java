package ru.kn.dashws.ws;

import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value="/websocket/conn")
public class TestConnection {
	@OnOpen
	public void onOpen(Session ses) {
		
	}
	@OnMessage
	public String onMessage(String msg) {
		return msg;
	}
}
