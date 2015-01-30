package ru.kn.dashws.web;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;


/**
 * Servlet implementation class HttpEndpoint
 */
@WebServlet(
		asyncSupported = true, 
		urlPatterns = { 
				"/data/*", 
				"/dashboards/*"
		}, 
		initParams = { 
				@WebInitParam(name = "auth_token", value = "token", description = "authentication token for accepting data")
		})
public class HttpEndpoint extends HttpServlet {
	private static final long serialVersionUID = 1L;
    private String auth_token;
    Logger logger = LoggerFactory.getLogger(HttpEndpoint.class);
    ExecutorService executor;
    /**
     * @see HttpServlet#HttpServlet()
     */
    public HttpEndpoint() {
        super();
    }

	/**
	 * @see Servlet#init(ServletConfig)
	 */
	public void init(ServletConfig config) throws ServletException {
		auth_token=config.getInitParameter("auth_token");
		String numexecutors=config.getInitParameter("web_executors");
		Integer numexecs=10;
		try {
			numexecs = Integer.parseInt(numexecutors);
		} catch(NumberFormatException e) {
			logger.debug("web_executors parameter is invalid, using default value");
		};
		executor=Executors.newFixedThreadPool(numexecs); 
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String path=request.getRequestURI();
		if(path.endsWith("/")) path=path.substring(0, path.length()-1);
		String[] parts=path.split("/");
		final String target = parts[parts.length-2];
		final String id=parts[parts.length-1];
		StringBuilder builder=new StringBuilder();
		String content=null;
		while((content=request.getReader().readLine())!=null) builder.append(content);
		JsonParser parser = new JsonParser();
		try {
			JsonElement elem=parser.parse(content);
			final JsonObject obj = elem.getAsJsonObject();
			if(obj.has("auth_token")) {
				String req_auth_token=obj.get("auth_token").getAsString();
				if(req_auth_token.equals(auth_token)) {
					response.setStatus(204);
					executor.submit(new Runnable()  {
						@Override
						public void run() {
							WebSocketServer.send(id,obj,target);
						}
					});
				} else {
					response.sendError(401, "Invalid auth_token");
				}
			} else response.sendError(401, "No auth_token");
		} catch(JsonParseException e1) {
			response.sendError(400, "body is not a valid json");
		}
	}

}
