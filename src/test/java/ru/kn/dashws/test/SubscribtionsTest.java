package ru.kn.dashws.test;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.collection.IsIn.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.WebSocketContainer;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.Gson;


public class SubscribtionsTest {
	private static WebSocketContainer container = ContainerProvider.getWebSocketContainer();
	private static WSClient client;
	
	@BeforeClass
	public static void connectWs() throws DeploymentException, IOException, URISyntaxException, InterruptedException {
		client = new WSClient();
		container.connectToServer(client, new URI("ws://localhost:8080/websocket/connection"));
		String message=null;
		assertNotNull(message=client.messages.poll(WSClient.TIMEOUT, TimeUnit.MILLISECONDS));
		assertTrue(message.startsWith("onopen"));		
                //get ack
                assertNotNull(message=client.messages.poll(WSClient.TIMEOUT, TimeUnit.MILLISECONDS));
	}
	
	@Test
	public void invalidSubscription() throws IOException, InterruptedException {
		client.sendMessage("{\"type\":\"subscribe\"}");
		String message=null;
		assertNotNull(message=client.messages.poll(WSClient.TIMEOUT, TimeUnit.MILLISECONDS));
		Gson gs = new Gson();
		Message msg = gs.fromJson(message, Message.class);
		assertThat(msg.type, equalTo("error"));
	}
	
	@Test
	public void receiveLastEvents() throws IOException, InterruptedException {
		//send data before subscriptio
		given().with().body("{\"auth_token\":\""+WSClient.AUTH_TOKEN+"\",\"data\":\"a1\"}").post("/data/d1")
		.then().assertThat().statusCode(204);
		given().with().body("{\"auth_token\":\""+WSClient.AUTH_TOKEN+"\",\"data\":\"a2\"}").post("/data/d2")
		.then().assertThat().statusCode(204);
		//send subscribe
		Gson gs = new Gson();
		client.sendMessage("{\"type\": \"subscribe\",\"data\": {\"events\":[\"d2\",\"d1\"]}}");
		LastEventsMessage submsg=null;
		String strmsg=null;
		assertNotNull(strmsg=client.messages.poll(WSClient.TIMEOUT, TimeUnit.MILLISECONDS));
		submsg = gs.fromJson(strmsg, LastEventsMessage.class);
		assertTrue(submsg.data.size()>=2);
	}
	
	@Test
	public void receiveEvents() throws IOException, InterruptedException {
		//send data before subscriptio

		//send subscribe
		Gson gs = new Gson();
		client.sendMessage("{\"type\": \"subscribe\",\"data\": {\"events\":[\"d3\",\"d4\"]}}");
		Message msg=null;
		String strmsg=null;
		//skip events message
		assertNotNull(strmsg=client.messages.poll(WSClient.TIMEOUT, TimeUnit.MILLISECONDS));

		given().with().body("{\"auth_token\":\""+WSClient.AUTH_TOKEN+"\",\"data\":\"a1\"}").post("/data/d3")
		.then().assertThat().statusCode(204);
		given().with().body("{\"auth_token\":\""+WSClient.AUTH_TOKEN+"\",\"data\":\"a2\"}").post("/data/d4")
		.then().assertThat().statusCode(204);
		
		Message[] msgs=new Message[2];
		for(int i=0;i<2;i++) {
			assertNotNull(strmsg=client.messages.poll(WSClient.TIMEOUT, TimeUnit.MILLISECONDS));
			msg=gs.fromJson(strmsg, Message.class);
			msgs[i]=msg;
			assertThat(msg.data.get("id"),isIn(Arrays.asList("d3","d4")));
			assertThat(msg.data.get("id"),not(isIn(Arrays.asList("d1","d2"))));
		}
	}
}
