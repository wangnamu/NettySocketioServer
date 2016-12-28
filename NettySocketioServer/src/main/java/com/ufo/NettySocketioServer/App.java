package com.ufo.NettySocketioServer;

import com.corundumstudio.socketio.listener.*;
import com.corundumstudio.socketio.store.RedissonStoreFactory;
import com.google.gson.Gson;
import com.ufo.NettySocketioServer.Models.UserInfo;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import org.redisson.Redisson;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import com.corundumstudio.socketio.*;

public class App {

	public static void main(String[] args) throws InterruptedException, UnknownHostException {

		// JedisPool jedisPool = new JedisPool(new JedisPoolConfig(),
		// "localhost", 6379);

		Config redisConfig = new Config();
		redisConfig.useSingleServer().setAddress("192.168.19.79:6379");
		RedissonClient redisson = Redisson.create(redisConfig);

		InetAddress addr = InetAddress.getLocalHost();
		String ip = addr.getHostAddress();

		int port = Integer.valueOf(args[0]);

		System.out.println(String.format("server is running at ip %s port %d!!!", ip, port));

		Configuration config = new Configuration();
		config.setHostname(ip);
		config.setPort(port);
		config.setStoreFactory(new RedissonStoreFactory(redisson));

		final SocketIOServer server = new SocketIOServer(config);

		// 登录
		server.addEventListener("login", String.class, new DataListener<String>() {
			@Override
			public void onData(SocketIOClient client, String data, AckRequest ackSender) {

				client.joinRoom("aaa");
				server.getRoomOperations("aaa").sendEvent("kickoff", "kickoff");
				
				
				Gson gson = new Gson();
				UserInfo userInfo = gson.fromJson(data, UserInfo.class);
//
//				RMap<String, String> currentUser = redisson.getMap(userInfo.getSID());
//				if (currentUser != null) {
//					if (Boolean.valueOf(currentUser.get("IsOnline"))
//							&& (currentUser.get("DeviceToken") != userInfo.getDeviceToken())) {
//						// 踢掉旧用户
//						//server.getClient(UUID.fromString(currentUser.get("SessionID"))).sendEvent("kickoff", "kickoff");
//
//						server.getRoomOperations(currentUser.get("SID")).sendEvent("kickoff", "kickoff");
//						
//						System.out.println("sid:" + currentUser.get("SID") + " token:" + currentUser.get("DeviceToken")
//								+ " was kicked off!!!");
//
//						server.getClient(UUID.fromString(currentUser.get("SessionID"))).set("Down", "KDown");
//						server.getClient(UUID.fromString(currentUser.get("SessionID"))).disconnect();
//					}
//				}
//
//				userInfo.setIsOnline(true);
//				userInfo.setSessionID(client.getSessionId().toString());
//
//				currentUser.putAll(userInfo.toHashMap());
//
//				client.set("SID", userInfo.getSID());
//				
//				client.joinRoom(userInfo.getSID());
//
				if (ackSender.isAckRequested()) {
					ackSender.sendAckData(gson.toJson(userInfo));
					System.out.println("userName:" + userInfo.getUserName() + " sessionID:" + userInfo.getSessionID()
							+ " login success!!!!!");
				}
			}

		});

		// 登出
		server.addEventListener("logoff", String.class, new DataListener<String>() {

			@Override
			public void onData(SocketIOClient client, String data, AckRequest ackSender) throws Exception {

				client.set("Down", "SDown");

				Gson gson = new Gson();
				UserInfo userInfo = gson.fromJson(data, UserInfo.class);

				userInfo.setIsOnline(false);

				RMap<String, String> userMap = redisson.getMap(userInfo.getSID());
				userMap.fastPut("IsOnline", String.valueOf(false));

				if (ackSender.isAckRequested()) {
					ackSender.sendAckData(gson.toJson(userInfo));
					System.out.println("userName:" + userInfo.getUserName() + " sessionID:" + userInfo.getSessionID()
							+ " logoff success!!!!!");
				}
			}
		});

		// 断线
		server.addDisconnectListener(new DisconnectListener() {

			@Override
			public void onDisconnect(SocketIOClient client) {
				// TODO Auto-generated method stub
				String sid = client.get("SID");
				System.out.println(sid + " disconnect");

				client.leaveRoom(sid);
				
				String down = client.get("Down");
				if (down != "KDown") {

					RMap<String, String> userMap = redisson.getMap(sid);

					userMap.fastPut("IsOnline", String.valueOf(false));
					
					client.disconnect();
				}

			}

		});

		server.start();

		Thread.sleep(Integer.MAX_VALUE);

		server.stop();
		// jedisPool.close();
		// jedisPool.destroy();

	}

}
