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


public class DashboardsTest {
	private static WebSocketContainer container = ContainerProvider.getWebSocketContainer();
	private static WSClient client;
	
	@BeforeClass
	public static void connectWs() throws DeploymentException, IOException, URISyntaxException, InterruptedException {
		client = new WSClient();
		container.connectToServer(client, new URI("ws://localhost:8080/websocket/connection"));
		String message=null;
		assertNotNull(message=client.messages.poll(WSClient.TIMEOUT, TimeUnit.MILLISECONDS));
		assertTrue(message.startsWith("onopen"));		
	}
	

	
	@Test
	public void allDashboards() throws IOException, InterruptedException {
		//send data before subscriptio

		//send subscribe
		Gson gs = new Gson();
		client.sendMessage("{\"type\": \"subscribe\",\"data\": {\"events\":[\"d7\"]}}");
		Message msg=null;
		String strmsg=null;
		//skip subscribe and last events messages
		assertNotNull(strmsg=client.messages.poll(WSClient.TIMEOUT, TimeUnit.MILLISECONDS));
		assertNotNull(strmsg=client.messages.poll(WSClient.TIMEOUT, TimeUnit.MILLISECONDS));

		given().with().body("{\"auth_token\":\""+WSClient.AUTH_TOKEN+"\",\"event\":\"reload\"}").post("/dashboards/*")
		.then().assertThat().statusCode(204);
		assertNotNull(strmsg=client.messages.poll(WSClient.TIMEOUT, TimeUnit.MILLISECONDS));
		msg=gs.fromJson(strmsg, Message.class);
		assertThat(msg.type, equalTo("dashboards"));
		assertThat(msg.data, allOf(hasKey("event"),hasKey("id"),hasEntry("id","*")));
	}
	@Test
	public void oneDashboard() throws IOException, InterruptedException {
		//send data before subscription

		//send subscribe
		Gson gs = new Gson();
		client.sendMessage("{\"type\": \"subscribe\",\"data\": {\"events\":[\"d7\"]}}");
		Message msg=null;
		String strmsg=null;
		//skip subscribe and last events messages
		assertNotNull(strmsg=client.messages.poll(WSClient.TIMEOUT, TimeUnit.MILLISECONDS));
		assertNotNull(strmsg=client.messages.poll(WSClient.TIMEOUT, TimeUnit.MILLISECONDS));

		given().with().body("{\"auth_token\":\""+WSClient.AUTH_TOKEN+"\",\"event\":\"reload\"}").post("/dashboards/db1")
		.then().assertThat().statusCode(204);
		assertNotNull(strmsg=client.messages.poll(WSClient.TIMEOUT, TimeUnit.MILLISECONDS));
		msg=gs.fromJson(strmsg, Message.class);
		assertThat(msg.type, equalTo("dashboards"));
		assertThat(msg.data, allOf(hasKey("event"),hasKey("id"),hasEntry("id","db1")));
	}
}
