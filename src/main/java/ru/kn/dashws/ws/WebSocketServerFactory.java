package ru.kn.dashws.ws;

import java.lang.reflect.Field;

public class WebSocketServerFactory {
	public static Object getServer() {
		try {
		Class<?> single = ClassLoader.getSystemClassLoader().loadClass("ru.kn.dashws.ws.WebSocketServer");
		Field instfield=single.getDeclaredField("inst");
		synchronized (single) {
			Object instance = instfield.get(null);
			if(instance==null) {
				instance = single.newInstance();
				instfield.set(null,instance);
			}
			return instance;
		}
		}catch(Exception e) {
			e.printStackTrace();
			return null;
		}
		
	}
}
