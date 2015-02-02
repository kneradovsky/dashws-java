package ru.kn.dashws.ws;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Resource;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class WebSocketServer {
	private static WebSocketServer inst;
	static ReadWriteLock strwlock = new ReentrantReadWriteLock();
	ReadWriteLock rwlock = new ReentrantReadWriteLock();
	ReadWriteLock histrwlock = new ReentrantReadWriteLock();
	AtomicInteger connidSource=new AtomicInteger(-1);
	Map<Integer,ClientConnection> connections;
	Map<String,List<Integer>> subscriptions;
	Map<String,JsonObject> history;
	ExecutorService execsrv=Executors.newCachedThreadPool();
	Logger logger = LoggerFactory.getLogger(WebSocketServer.class);
	
	private WebSocketServer() {
		connections=new HashMap<>();
		subscriptions=new HashMap<>();
		history=new HashMap<>();
		logger.debug("constructing WebSocketServer");
	}
	public static WebSocketServer getInstance2() {
		try {
			//strwlock.writeLock().lock();
			if(inst==null) {
				inst=new WebSocketServer();

			}
			return inst;
		} finally {
			//strwlock.writeLock().unlock();
		}
	}
	
	
	public Integer add(ClientConnection conn) {
		Integer connid=connidSource.incrementAndGet();
		rwlock.writeLock().lock();
		connections.put(connid,conn);
		rwlock.writeLock().unlock();
		return connid;
	}
	
	public void remove(Integer connid) {
		rwlock.writeLock().lock();
		//remove from connections' map
		connections.remove(connid);
		//remove from each subscribers list
		for(Entry<String,List<Integer>> subs : subscriptions.entrySet()) {
			subs.getValue().remove(connid);
		}
		rwlock.writeLock().unlock();
	}
	
	public List<JsonObject> subscribe(Integer connid,List<String> subs_ids) {
		rwlock.writeLock().lock();
		//create history excerpt for subs_ids
		List<JsonObject> hist=new LinkedList<JsonObject>();
		//for each subs id find its subscribers and add connid to that list
		for(String subid : subs_ids) {
			List<Integer> sublist=null;
			if(!subscriptions.containsKey(subid)) {
				sublist=new LinkedList<Integer>();
				subscriptions.put(subid,sublist);
			} else {
				JsonObject stobj=getHistory(subid);
				if(stobj!=null) hist.add(stobj); 
				sublist=subscriptions.get(subid);
			}
			sublist.add(connid);
		}
		rwlock.writeLock().unlock();
		return hist;
	}
	
	public JsonObject getHistory(String id) {
		try {
			histrwlock.readLock().lock();
			return history.get(id);
		} finally {
			histrwlock.readLock().unlock();
		}
	}
	
	public JsonObject updateHistory(String id,JsonObject newdata) {
		try {
			histrwlock.writeLock().lock();
			if(history.containsKey(id)) {
				JsonObject olddata=history.get(id);
				for(Entry<String,JsonElement> field : olddata.entrySet()) {
					if(olddata.has(field.getKey())) newdata.add(field.getKey(), field.getValue());
				}
			} 
			history.put(id,newdata);
		} catch(Exception e) {
			logger.debug("updateHistory exception:", e);
		} finally {
			histrwlock.writeLock().unlock();
		}
		return newdata;
	}
	public void send(String id,JsonObject body,String target) {
		if(target.equals("dashboards")) {
			rwlock.readLock().lock();
			for(Entry<Integer,ClientConnection> connentry: connections.entrySet()) {
				final ClientConnection conn=connentry.getValue();
				final JsonObject msgBody=body;
				final String msgTarget=target;
				execsrv.submit(new Runnable() {
					@Override
					public void run() {
						String msg=conn.format_event(msgBody,msgTarget);
						conn.sendText(msg);
					}
				});
			}
			rwlock.readLock().unlock();
		} else {
			final JsonObject dataobj=updateHistory(id, body);
			rwlock.readLock().lock();
			if(subscriptions.containsKey(id)) {
				for(Integer connid: subscriptions.get(id)) {
					final ClientConnection conn=connections.get(connid);
					execsrv.submit(new Runnable() {
						@Override
						public void run() {
							String msg=conn.format_event(dataobj);
							conn.sendText(msg);
						}
					});
				}
			} else 
				logger.debug("send: widget:"+id+" has no subscriptions");
			rwlock.readLock().unlock();
		}
	}
}
