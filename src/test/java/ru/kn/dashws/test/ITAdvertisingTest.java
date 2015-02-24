package ru.kn.dashws.test;

import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.WebSocketContainer;

import static org.hamcrest.collection.IsArrayContaining.*;
import static org.hamcrest.core.IsEqual.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.*;


import org.junit.Test;

import com.google.gson.Gson;

public class ITAdvertisingTest {
	private WebSocketContainer container = ContainerProvider.getWebSocketContainer();
	@Test
	public void testAdvertiseNoSubsctriptions() throws DeploymentException, IOException, URISyntaxException, InterruptedException {
		given().with().body("{\"auth_token\":\""+WSClient.AUTH_TOKEN+"\",\"data\":\"a\"}").post("/data/d1")
		.then().assertThat().statusCode(204);
		WSClient client = new WSClient();
		container.connectToServer(client, new URI("ws://localhost:8080/websocket/connection"));
		String message = null;
		assertNotNull(message = client.messages.poll(WSClient.TIMEOUT, TimeUnit.MILLISECONDS));
		assertTrue(message.startsWith("onopen"));
		// get ack
		assertNotNull(message = client.messages.poll(WSClient.TIMEOUT, TimeUnit.MILLISECONDS));
		// skip advertising
		assertNotNull(message = client.messages.poll(WSClient.TIMEOUT, TimeUnit.MILLISECONDS));
		Gson gs = new Gson();
		AdvertisingMessage msg = gs.fromJson(message, AdvertisingMessage.class);
		assertThat(msg.data.toArray(new String[0]), hasItemInArray(equalTo("d1")));
		client.close();
	}
	@Test
	public void testAdvertiseWithSubscription() throws DeploymentException, IOException, URISyntaxException, InterruptedException {
		given().with().body("{\"auth_token\":\""+WSClient.AUTH_TOKEN+"\",\"data\":\"a\"}").post("/data/d1")
			.then().assertThat().statusCode(204);
		
		//connect and subscribe for d1
		WSClient client = new WSClient();
		container.connectToServer(client, new URI("ws://localhost:8080/websocket/connection"));
		String message = null;
		assertNotNull(message = client.messages.poll(WSClient.TIMEOUT, TimeUnit.MILLISECONDS));
		assertTrue(message.startsWith("onopen"));
		// get ack
		assertNotNull(message = client.messages.poll(WSClient.TIMEOUT, TimeUnit.MILLISECONDS));
		// skip advertising
		assertNotNull(message = client.messages.poll(WSClient.TIMEOUT, TimeUnit.MILLISECONDS));
		Gson gs = new Gson();
		AdvertisingMessage msg = gs.fromJson(message, AdvertisingMessage.class);
		assertThat(msg.data.toArray(new String[0]), hasItemInArray(equalTo("d1")));
		//send subscribtion
		client.sendMessage("{\"type\": \"subscribe\",\"data\": {\"events\":[\"d2\"]}}");
		
		given().with().body("{\"auth_token\":\""+WSClient.AUTH_TOKEN+"\",\"data\":\"a\"}").post("/data/d2")
			.then().assertThat().statusCode(204);
		WSClient client2 = new WSClient();
		container.connectToServer(client2, new URI("ws://localhost:8080/websocket/connection"));
		assertNotNull(message = client2.messages.poll(WSClient.TIMEOUT, TimeUnit.MILLISECONDS));
		assertTrue(message.startsWith("onopen"));
		// get ack
		assertNotNull(message = client2.messages.poll(WSClient.TIMEOUT, TimeUnit.MILLISECONDS));
		// skip advertising
		assertNotNull(message = client2.messages.poll(WSClient.TIMEOUT, TimeUnit.MILLISECONDS));
		msg = gs.fromJson(message, AdvertisingMessage.class);
		assertThat(msg.data.toArray(new String[0]), allOf(hasItemInArray(equalTo("d1")),hasItemInArray(equalTo("d2"))));
		client.close();
		client2.close();
	}
	
}
