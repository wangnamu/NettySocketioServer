package com.ufo.NettySocketioServer;

import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.redisson.api.RMap;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;

import com.corundumstudio.socketio.AckCallback;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.google.gson.Gson;
import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsService;
import com.notnoop.apns.ApnsServiceBuilder;
import com.notnoop.apns.PayloadBuilder;
import com.ufo.NettySocketioServer.Models.DeviceTypeEnum;
import com.ufo.NettySocketioServer.Models.Message;
import com.ufo.NettySocketioServer.Models.MessageTypeEnum;
import com.ufo.NettySocketioServer.Models.Notify;
import com.ufo.NettySocketioServer.Models.UserInfo;
import com.ufo.NettySocketioServer.Models.UserInfoBean;

public class MySocketIOServer extends SocketIOServer {

	//发送消息最大并发线程数
	private final static int MAXTHREADCOUNT = 100;

	private RedissonClient mRedisson;
	private ApnsService mApnsService;

	public MySocketIOServer(Configuration configuration) {
		super(configuration);
	}

	public MySocketIOServer(Configuration configuration, RedissonClient redisson) {
		super(configuration);
		mRedisson = redisson;
	}

	public void setUp(String path, String password, Boolean isproduction) {

		ApnsServiceBuilder builder = APNS.newService().withCert(path, password);
		if (isproduction) {
			mApnsService = builder.withProductionDestination().build();
		} else {
			mApnsService = builder.withSandboxDestination().build();
		}

		setUpLisenter();
		setUpTopic();
	}

	private void setUpLisenter() {
		// 登录
		this.addEventListener(EventLisenter.LOGIN, String.class, new DataListener<String>() {
			@Override
			public void onData(SocketIOClient client, String data, AckRequest ackSender) {

				Gson gson = new Gson();
				UserInfo userInfo = gson.fromJson(data, UserInfo.class);

				RMap<String, String> currentUser = mRedisson.getMap(userInfo.getSID());

				checkLogin(currentUser, userInfo);

				UserInfoBean userInfoBean = UserInfoBean.translateFromUserInfo(userInfo,
						client.getSessionId().toString(), currentUser.get("SessionIDIOS"),
						currentUser.get("SessionIDANDROID"), currentUser.get("SessionIDPC"));

				currentUser.putAll(userInfoBean.toHashMap());

				client.set("SID", userInfo.getSID());
				client.set("DeviceType", userInfo.getDeviceType());

				if (ackSender.isAckRequested()) {
					ackSender.sendAckData(gson.toJson(userInfo));
					System.out.println(String.format("username:%s with %s login success!", userInfo.getUserName(),
							userInfo.getDeviceType()));
				}
			}

		});

		// 登出
		this.addEventListener(EventLisenter.LOGOFF, String.class, new DataListener<String>() {

			@Override
			public void onData(SocketIOClient client, String data, AckRequest ackSender) throws Exception {

				client.set("Down", "SDown");

				Gson gson = new Gson();
				UserInfo userInfo = gson.fromJson(data, UserInfo.class);

				RMap<String, String> userMap = mRedisson.getMap(userInfo.getSID());

				if (userMap != null) {
					if (userInfo.getDeviceType().equals(DeviceTypeEnum.ANDROID)) {
						userMap.fastPut("SessionIDANDROID", null);
					} else if (userInfo.getDeviceType().equals(DeviceTypeEnum.IOS)) {
						userMap.fastPut("SessionIDIOS", null);
					} else {
						userMap.fastPut("SessionIDPC", null);
					}

					if (!isOnline(userMap)) {
						userMap.delete();
					}
				}

				if (ackSender.isAckRequested()) {
					ackSender.sendAckData(gson.toJson(userInfo));
					System.out.println(String.format("username:%s with %s logoff success!", userInfo.getUserName(),
							userInfo.getDeviceType()));
				}
			}
		});

		// 来自同一用户在不同平台下发来的通知
		this.addEventListener(EventLisenter.NOTIFYOTHERPLATFORMS, String.class, new DataListener<String>() {
			@Override
			public void onData(SocketIOClient client, String data, AckRequest ackSender) throws Exception {
				mRedisson.getTopic(Topic.NOTIFYOTHERPLATFORMS).publish(data);
			}
		});

		// 消息
		this.addEventListener(EventLisenter.NEWS, String.class, new DataListener<String>() {
			@Override
			public void onData(SocketIOClient client, String data, AckRequest ackSender) throws Exception {
				mRedisson.getTopic(Topic.NEWS).publish(data);
			}
		});

		// 断线
		this.addDisconnectListener(new DisconnectListener() {

			@Override
			public void onDisconnect(SocketIOClient client) {

				String sid = client.get("SID");
				String deviceType = client.get("DeviceType");

				System.out.println(String.format("SID:%s with %s disconnect", sid, deviceType));

				String down = client.get("Down");
				if (down != null && !down.equals("KDown")) {
					RMap<String, String> userMap = mRedisson.getMap(sid);
					// IOS不存在断线之说
					if (userMap != null) {
						if (deviceType.equals(DeviceTypeEnum.ANDROID)) {
							userMap.fastPut("SessionIDANDROID", null);
						} else if (deviceType.equals(DeviceTypeEnum.PC)) {
							userMap.fastPut("SessionIDPC", null);
						}

						if (!isOnline(userMap)) {
							userMap.delete();
						}
					}
				}

				mRedisson.getMap(client.getSessionId().toString()).delete();
				client.disconnect();
			}

		});
	}

	private void setUpTopic() {
		// 被踢下线
		RTopic<String> kickoffTopic = mRedisson.getTopic(Topic.KICKOFF);
		kickoffTopic.addListener(new MessageListener<String>() {
			@Override
			public void onMessage(String channel, String sessionID) {

				SocketIOClient kickoffClient = getClient(UUID.fromString(sessionID));
				if (kickoffClient != null) {
					kickoffClient.sendEvent(Event.KICKOFF, "kickoff");
					kickoffClient.set("Down", "KDown");
					kickoffClient.disconnect();

					System.out.println(String.format("SessionID:%s was kicked off", sessionID));
				}
			}
		});

		// 消息
		RTopic<String> newsTopic = mRedisson.getTopic(Topic.NEWS);
		newsTopic.addListener(new MessageListener<String>() {
			@Override
			public void onMessage(String channel, String data) {

				Gson gson = new Gson();
				Message message = gson.fromJson(data, Message.class);
				Message messageForSend = gson.fromJson(data, Message.class);

				ExecutorService executorService = Executors.newFixedThreadPool(MAXTHREADCOUNT,
						new DaemonThreadFactory());

				for (String receiverID : message.getReceiverIDs()) {
					executorService.execute(new Runnable() {
						@Override
						public void run() {
							sendNews(receiverID, messageForSend);
						}
					});
				}

				executorService.shutdown();
			}
		});

		// 广播
		RTopic<String> broadcastTopic = mRedisson.getTopic(Topic.BROADCAST);
		broadcastTopic.addListener(new MessageListener<String>() {
			@Override
			public void onMessage(String channel, String data) {
				getBroadcastOperations().sendEvent(Event.BROADCAST, data);
			}
		});

		// 通知其它平台
		RTopic<String> notifyTopic = mRedisson.getTopic(Topic.NOTIFYOTHERPLATFORMS);
		notifyTopic.addListener(new MessageListener<String>() {
			@Override
			public void onMessage(String channel, String data) {
				Gson gson = new Gson();
				Notify notify = gson.fromJson(data, Notify.class);

				RMap<String, String> userMap = mRedisson.getMap(notify.getUserID());
				if (userMap == null)
					return;
				if (userMap.get("SID") == null)
					return;

				if (isAndroidOnline(userMap) && isPCOnline(userMap)) {
					if (notify.getSourceDeviceType().equals(DeviceTypeEnum.ANDROID)) {
						SocketIOClient client = getClient(UUID.fromString(userMap.get("SessionIDPC")));
						if (client != null) {
							client.sendEvent(Event.NOTIFYOTHERPLATFORMS, data);
							System.out
									.println(String.format("userID:%s notify from Android to PC", notify.getUserID()));
						}
					} else {
						SocketIOClient client = getClient(UUID.fromString(userMap.get("SessionIDANDROID")));
						if (client != null) {
							client.sendEvent(Event.NOTIFYOTHERPLATFORMS, data);
							System.out
									.println(String.format("userID:%s notify from PC to Android", notify.getUserID()));
						}
					}
				} else if (isIOSOnline(userMap) && isPCOnline(userMap)) {
					if (notify.getSourceDeviceType().equals(DeviceTypeEnum.IOS)) {
						SocketIOClient client = getClient(UUID.fromString(userMap.get("SessionIDPC")));
						if (client != null) {
							client.sendEvent(Event.NOTIFYOTHERPLATFORMS, data);
							System.out.println(String.format("userID:%s notify from IOS to PC", notify.getUserID()));
						}
					} else {
						SocketIOClient client = getClient(UUID.fromString(userMap.get("SessionIDIOS")));
						if (client != null) {
							client.sendEvent(Event.NOTIFYOTHERPLATFORMS, data);
							System.out.println(String.format("userID:%s notify from PC to IOS", notify.getUserID()));
						}
					}
				}
			}
		});
	}

	/**
	 * IOS在线
	 * 
	 * @param currentUser
	 * @return
	 */
	private boolean isIOSOnline(RMap<String, String> currentUser) {
		if (currentUser.get("SessionIDIOS") != null) {
			return true;
		}
		return false;
	}

	/**
	 * Android在线
	 * 
	 * @param currentUser
	 * @return
	 */
	private boolean isAndroidOnline(RMap<String, String> currentUser) {
		if (currentUser.get("SessionIDAndroid") != null) {
			return true;
		}
		return false;
	}

	/**
	 * PC在线
	 * 
	 * @param currentUser
	 * @return
	 */
	private boolean isPCOnline(RMap<String, String> currentUser) {
		if (currentUser.get("SessionIDPC") != null) {
			return true;
		}
		return false;
	}

	/**
	 * APP在线
	 * 
	 * @param currentUser
	 * @return
	 */
	private boolean isAPPOnline(RMap<String, String> currentUser) {
		return isAndroidOnline(currentUser) | isIOSOnline(currentUser);
	}

	/**
	 * 在线
	 * 
	 * @param currentUser
	 * @return
	 */
	private boolean isOnline(RMap<String, String> currentUser) {
		return isAPPOnline(currentUser) | isPCOnline(currentUser);
	}

	/**
	 * 检查并踢掉相同平台下已登陆用户
	 * 
	 * @param currentUser
	 * @param loginUser
	 */
	private void checkLogin(RMap<String, String> currentUser, UserInfo loginUser) {

		if (currentUser != null && currentUser.get("SID") != null) {

			if (isOnline(currentUser) && !(currentUser.get("DeviceToken").equals(loginUser.getDeviceToken()))) {

				// PC踢PC
				if (isPCOnline(currentUser) && loginUser.getDeviceType().equals(DeviceTypeEnum.PC)) {
					mRedisson.getTopic(Topic.KICKOFF).publish(currentUser.get("SessionIDPC"));
					System.out.println(String.format("SID:%s Token %s with %s will kicked off on the PC silde !!!",
							currentUser.get("SID"), currentUser.get("DeviceToken"), loginUser.getDeviceType()));
				}
				// 移动踢移动
				else if (isAPPOnline(currentUser) && !loginUser.getDeviceType().equals(DeviceTypeEnum.PC)) {
					String sessionID = isAndroidOnline(currentUser) ? currentUser.get("SessionIDANDROID")
							: currentUser.get("SessionIDIOS");
					mRedisson.getTopic(Topic.KICKOFF).publish(sessionID);
					System.out.println(String.format("SID:%s Token %s with %s will kicked off on the APP silde!!!",
							currentUser.get("SID"), currentUser.get("DeviceToken"), loginUser.getDeviceType()));
				}

			}

		}

	}

	/**
	 * 发送消息
	 * 
	 * @param receiverID
	 * @param msg
	 */
	private void sendNews(String receiverID, Message msg) {
		RMap<String, String> userMap = mRedisson.getMap(receiverID);

		if (!isOnline(userMap))
			return;

		if (isAndroidOnline(userMap)) {
			String sessionID = userMap.get("SessionIDANDROID");

			if (sessionID == null)
				return;

			SocketIOClient client = getClient(UUID.fromString(sessionID));
			if (client == null)
				return;

			System.out.println(String.format("UserID %s send %s to %s", msg.getSenderID(), msg.getBody(), receiverID));

			HashSet<String> hashSet = new HashSet<>();
			hashSet.add(receiverID);
			msg.setReceiverIDs(hashSet);

			Gson gson = new Gson();
			String data = gson.toJson(msg);

			client.sendEvent(Event.NEWS, new AckCallback<String>(String.class) {
				@Override
				public void onSuccess(String result) {
					System.out.println(String.format("UserID %s on Android reply %s!", receiverID, result));
				}
			}, data);

		}

		if (isIOSOnline(userMap)) {
			String sessionID = userMap.get("SessionIDIOS");

			if (sessionID == null)
				return;

			System.out.println(String.format("UserID %s send %s to %s", msg.getSenderID(), msg.getBody(), receiverID));

			HashSet<String> hashSet = new HashSet<>();
			hashSet.add(receiverID);
			msg.setReceiverIDs(hashSet);

			Gson gson = new Gson();
			String data = gson.toJson(msg);

			SocketIOClient client = getClient(UUID.fromString(sessionID));

			if (client == null) {
				// 在后台
				System.out.println("in background");
				sendToAPN(msg, userMap.get("DeviceToken"));
			} else {
				// 在前台
				System.out.println("in foreground");
				client.sendEvent(Event.NEWS, new AckCallback<String>(String.class) {
					@Override
					public void onSuccess(String result) {
						System.out.println(String.format("UserID %s on IOS reply %s!", receiverID, result));
					}
				}, data);
			}
		}

		if (isPCOnline(userMap)) {
			String sessionID = userMap.get("SessionIDPC");

			if (sessionID == null)
				return;

			SocketIOClient client = getClient(UUID.fromString(sessionID));
			if (client == null)
				return;

			System.out.println(String.format("UserID %s send %s to %s", msg.getSenderID(), msg.getBody(), receiverID));

			HashSet<String> hashSet = new HashSet<>();
			hashSet.add(receiverID);
			msg.setReceiverIDs(hashSet);

			Gson gson = new Gson();
			String data = gson.toJson(msg);

			client.sendEvent(Event.NEWS, new AckCallback<String>(String.class) {
				@Override
				public void onSuccess(String result) {
					System.out.println(String.format("UserID %s on PC reply %s!", receiverID, result));
				}
			}, data);

		}
	}

	/**
	 * APNS推送
	 * 
	 * @param msg
	 * @param deviceToken
	 */
	private void sendToAPN(Message msg, String deviceToken) {
		if (!msg.getIsAlert())
			return;
		PayloadBuilder builder = APNS.newPayload().badge(1).alertTitle(msg.getTitle())
				.customField("othersType", msg.getOthersType())
				.customField("others", msg.getOthers())
				.category(msg.getCategory()).mutableContent();

		switch (msg.getMessageType()) {
		case MessageTypeEnum.TEXT:
			builder.alertBody(msg.getBody());
			break;
		case MessageTypeEnum.IMAGE:
			builder.alertBody("[图片]");
			break;
		case MessageTypeEnum.FILE:
			builder.alertBody("[文件]");
			break;
		case MessageTypeEnum.URL:
			builder.alertBody("[链接]");
			break;
		case MessageTypeEnum.SOUND:
			builder.alertBody("[音频]");
			break;
		case MessageTypeEnum.MOVIE:
			builder.alertBody("[视频]");
			break;
		case MessageTypeEnum.EMOJI:
			builder.alertBody("[表情]");
			break;

		default:
			break;
		}

		String payload = builder.build();
		mApnsService.push(deviceToken, payload);

	}

	/**
	 * 守护线程
	 */
	private class DaemonThreadFactory implements ThreadFactory {
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setDaemon(true);
			return t;
		}
	}

}
