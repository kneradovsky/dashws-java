package ru.kn.dashws.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.WebSocketContainer;

import org.junit.Test;

import com.google.gson.Gson;






public class WebSocketTest {
	private WebSocketContainer container = ContainerProvider.getWebSocketContainer();
	
	@Test
	public void invalidJson() throws DeploymentException, IOException, URISyntaxException, InterruptedException {
		WSClient client = new WSClient();
		container.connectToServer(client, new URI("ws://localhost:8080/websocket/connection"));
		String message=null;
		assertNotNull(message=client.messages.poll(5000, TimeUnit.MILLISECONDS));
		assertTrue(message.startsWith("onopen"));
		client.sendMessage("{data: a}}");
		assertNotNull(message=client.messages.poll(5000, TimeUnit.MILLISECONDS));
		assertTrue(message.contains("error"));
	}
	
	@Test
	public void validJson() throws DeploymentException, IOException, URISyntaxException, InterruptedException {
		WSClient client = new WSClient();
		container.connectToServer(client, new URI("ws://localhost:8080/websocket/connection"));
		String message=null,message1=null;
		assertNotNull(message=client.messages.poll(5000, TimeUnit.MILLISECONDS));
		assertTrue(message.startsWith("onopen"));
		client.sendMessage("{\"type\": \"subscribe\",\"data\":{\"events\":[\"id1\",\"id2\"]}}");
		assertNotNull(message=client.messages.poll(5000, TimeUnit.MILLISECONDS));
		assertNotNull(message1=client.messages.poll(5000, TimeUnit.MILLISECONDS));
		Gson gs = new Gson();
		Message msg = gs.fromJson(message, Message.class);
		LastEventsMessage msg1 = gs.fromJson(message1, LastEventsMessage.class);
		assertThat(msg.type, equalTo("subscribe"));
		assertThat(msg.data, hasEntry("result", "ok"));
		assertThat(msg1.type, equals("event"));
		assertThat(msg1.data, notNullValue());
	}
 
}
