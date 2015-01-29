package ru.kn.dashws.test;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.WebSocketContainer;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.Gson;


public class HistoryTest {
	private WebSocketContainer container = ContainerProvider.getWebSocketContainer();
	private WSClient client;
	
	@BeforeClass
	public void connectWs() throws DeploymentException, IOException, URISyntaxException, InterruptedException {
		client = new WSClient();
		container.connectToServer(client, new URI("ws://localhost:8080/websocket/connection"));
		String message=null;
		assertNotNull(message=client.messages.poll(WSClient.TIMEOUT, TimeUnit.MILLISECONDS));
		assertTrue(message.startsWith("onopen"));		
	}
	
	
	@Test
	public void receiveLastEvents() throws IOException, InterruptedException {
		//send data before subscriptio
		given().with().body("{\"auth_token\":\"token1243\",\"data1\":\"a1\"}").post("/data/d5")
		.then().assertThat().statusCode(204);
		given().with().body("{\"auth_token\":\"token1243\",\"data2\":\"a2\"}").post("/data/d5")
		.then().assertThat().statusCode(204);
		//send subscribe
		Gson gs = new Gson();
		client.sendMessage("{\"type\": \"subscribe\",\"data\": {\"events\":[\"d5\"]}}");
		Message msg=null;
		LastEventsMessage submsg=null;
		String strmsg=null;
		assertNotNull(strmsg=client.messages.poll(WSClient.TIMEOUT, TimeUnit.MILLISECONDS));
		msg=gs.fromJson(strmsg, Message.class);
		assertThat(msg.type, equalTo("subscribe"));
		assertThat(msg.data, hasEntry("result", "ok"));
		assertNotNull(strmsg=client.messages.poll(WSClient.TIMEOUT, TimeUnit.MILLISECONDS));
		submsg = gs.fromJson(strmsg, LastEventsMessage.class);
		assertThat(submsg.data.get(0), allOf(hasKey("data1"),hasKey("data2")));
	}
	
	@Test
	public void receiveEvents() throws IOException, InterruptedException {
		//send data before subscriptio

		//send subscribe
		Gson gs = new Gson();
		client.sendMessage("{\"type\": \"subscribe\",\"data\": {\"events\":[\"d6\"]}}");
		Message msg=null;
		String strmsg=null;
		//skip subscribe and last events messages
		assertNotNull(strmsg=client.messages.poll(WSClient.TIMEOUT, TimeUnit.MILLISECONDS));
		assertNotNull(strmsg=client.messages.poll(WSClient.TIMEOUT, TimeUnit.MILLISECONDS));

		given().with().body("{\"auth_token\":\"token1243\",\"data1\":\"a1\"}").post("/data/d6")
		.then().assertThat().statusCode(204);
		given().with().body("{\"auth_token\":\"token1243\",\"data2\":\"a2\"}").post("/data/d6")
		.then().assertThat().statusCode(204);
		given().with().body("{\"auth_token\":\"token1243\",\"data1\":\"a3\"}").post("/data/d6")
		.then().assertThat().statusCode(204);
		//skip two messages
		assertNotNull(strmsg=client.messages.poll(WSClient.TIMEOUT, TimeUnit.MILLISECONDS));
		assertNotNull(strmsg=client.messages.poll(WSClient.TIMEOUT, TimeUnit.MILLISECONDS));
		//we need only last one
		assertNotNull(strmsg=client.messages.poll(WSClient.TIMEOUT, TimeUnit.MILLISECONDS));
		msg=gs.fromJson(strmsg, Message.class);
		assertThat(msg.data, allOf(hasKey("data1"),hasKey("data2"),hasEntry("data1","a3")));

	}
}
