package ru.kn.dashws.test;

import org.junit.Test;
import static com.jayway.restassured.RestAssured.*;

public class WebServerTest {

	@Test
	public void inexistentRoute() {
		post("/").then().assertThat().statusCode(404);
	}
	
	@Test
	public void withoutToken() {
		given().with().body("{\"data\":\"a\"}").post("/data/d1")
		.then().assertThat().statusCode(401);
	}

	@Test
	public void invalidToken() {
		given().with().body("{\"auth_token\":\""+WSClient.AUTH_TOKEN+"\",\"data\":\"a\"}").post("/data/d1")
		.then().assertThat().statusCode(401);
	}
	
	@Test
	public void validToken() {
		given().with().body("{\"auth_token\":\""+WSClient.AUTH_TOKEN+"\",\"data\":\"a\"}").post("/data/d1")
		.then().assertThat().statusCode(204);
	}
	@Test
	public void invalidData() {
		given().with().body("{\"auth_token\":\""+WSClient.AUTH_TOKEN+"\",\"data\":").post("/data/d1")
		.then().assertThat().statusCode(401);
	}	
}
