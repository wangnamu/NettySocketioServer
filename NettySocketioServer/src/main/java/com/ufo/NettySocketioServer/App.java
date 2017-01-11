package com.ufo.NettySocketioServer;

import com.corundumstudio.socketio.listener.*;
import com.corundumstudio.socketio.store.RedissonStoreFactory;
import com.google.gson.Gson;
import com.ufo.NettySocketioServer.Models.Message;
import com.ufo.NettySocketioServer.Models.UserInfo;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.UUID;

import org.redisson.Redisson;
import org.redisson.api.RMap;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;
import org.redisson.config.Config;

import com.corundumstudio.socketio.*;

public class App {

	public static void main(String[] args) throws InterruptedException, UnknownHostException {

		InetAddress addr = InetAddress.getLocalHost();
		String ip = addr.getHostAddress();

		int port = Integer.valueOf(args[0]);
		
		Config redisConfig = new Config();
		redisConfig.useSingleServer().setAddress(String.format("%s:6379", ip));
		RedissonClient redisson = Redisson.create(redisConfig);

		System.out.println(String.format("server is running at ip %s port %d!!!", ip, port));

		Configuration config = new Configuration();
		config.setHostname(ip);
		config.setPort(port);
		config.setStoreFactory(new RedissonStoreFactory(redisson));

		final SocketIOServer server = new SocketIOServer(config);

		// 被踢下线
		RTopic<String> kickoffTopic = redisson.getTopic(Topic.KICKOFF);
		kickoffTopic.addListener(new MessageListener<String>() {
			@Override
			public void onMessage(String channel, String sessionID) {
				System.out.println("kickoff method");

				SocketIOClient kickoffClient = server.getClient(UUID.fromString(sessionID));
				if (kickoffClient != null) {
					kickoffClient.sendEvent(Event.KICKOFF, "kickoff");
					kickoffClient.set("Down", "KDown");
					kickoffClient.disconnect();
				}
			}
		});

		// 消息
		RTopic<String> sendTopic = redisson.getTopic(Topic.SEND);
		sendTopic.addListener(new MessageListener<String>() {
			@Override
			public void onMessage(String channel, String data) {

				Gson gson = new Gson();
				Message message = gson.fromJson(data, Message.class);

				for (String receiverID : message.getReceiverIDs()) {

					RMap<String, String> userMap = redisson.getMap(receiverID);
					String sessionID = userMap.get("SessionID");

					if (sessionID != null) {

						SocketIOClient client = server.getClient(UUID.fromString(sessionID));
						if (client != null) {

							Message msg = gson.fromJson(data, Message.class);
							HashSet<String> hashSet = new HashSet<>();
							hashSet.add(receiverID);
							msg.setReceiverIDs(hashSet);

							client.sendEvent(Event.SEND, new AckCallback<String>(String.class) {

								@Override
								public void onSuccess(String result) {
									// TODO Auto-generated method stub
									
									System.out.println("send success:" + result);
								}

							}, gson.toJson(msg));
						}

					}
				}

			}
		});

		// 广播
		RTopic<String> broadcastTopic = redisson.getTopic(Topic.BROADCAST);
		broadcastTopic.addListener(new MessageListener<String>() {
			@Override
			public void onMessage(String channel, String data) {
				 server.getBroadcastOperations().sendEvent(Event.BROADCAST, data);
			}
		});

		// 登录
		server.addEventListener(EventLisenter.LOGIN, String.class, new DataListener<String>() {
			@Override
			public void onData(SocketIOClient client, String data, AckRequest ackSender) {

				Gson gson = new Gson();
				UserInfo userInfo = gson.fromJson(data, UserInfo.class);

				RMap<String, String> currentUser = redisson.getMap(userInfo.getSID());
				if (currentUser != null) {

					if (Boolean.valueOf(currentUser.get("IsOnline"))
							&& (currentUser.get("DeviceToken") != userInfo.getDeviceToken())) {

						redisson.getTopic(Topic.KICKOFF).publish(currentUser.get("SessionID"));

						System.out.println("sid:" + currentUser.get("SID") + " token:" + currentUser.get("DeviceToken")
								+ " was kicked off!!!");

					}
				}

				userInfo.setIsOnline(true);
				userInfo.setSessionID(client.getSessionId().toString());

				currentUser.putAll(userInfo.toHashMap());

				client.set("SID", userInfo.getSID());

				if (ackSender.isAckRequested()) {
					ackSender.sendAckData(gson.toJson(userInfo));
					System.out.println("userName:" + userInfo.getUserName() + " sessionID:" + userInfo.getSessionID()
							+ " login success!!!!!");
				}
			}

		});

		// 登出
		server.addEventListener(EventLisenter.LOGOFF, String.class, new DataListener<String>() {

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

				String down = client.get("Down");
				if (down != "KDown") {
					RMap<String, String> userMap = redisson.getMap(sid);
					userMap.fastPut("IsOnline", String.valueOf(false));
				}

				client.del(sid);
				client.disconnect();

			}

		});

		server.start();

		Thread.sleep(Integer.MAX_VALUE);

		server.stop();

	}

}
