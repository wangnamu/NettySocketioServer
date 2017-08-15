package com.ufo.NettySocketioServer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

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
import com.ufo.NettySocketioServer.Dao.UserInfoDao;
import com.ufo.NettySocketioServer.Models.DeviceTypeEnum;
import com.ufo.NettySocketioServer.Models.Message;
import com.ufo.NettySocketioServer.Models.MessageTypeEnum;
import com.ufo.NettySocketioServer.Models.Notify;
import com.ufo.NettySocketioServer.Models.Response;
import com.ufo.NettySocketioServer.Models.UserInfo;
import com.ufo.NettySocketioServer.Models.UserInfoBean;

public class MySocketIOServer extends SocketIOServer {

    private RedissonClient mRedisson;
    private ApnsService mApnsService;
    private String mServerToken;

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

        mServerToken = UUID.randomUUID().toString();

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

                UserInfoDao userInfoDao = new UserInfoDao(mRedisson, userInfo.getSID());
                List<UserInfoBean> userInfoBeans = userInfoDao.getUserInfoBeanList();

                // 检查是否存在其它设备登录
                if (userInfo.getCheckStatus()) {

                    UserInfoBean loginedUserInfoBean = getLoginedUserInfoBean(userInfoBeans, userInfo.getDeviceType(), userInfo.getDeviceToken());
                    if (loginedUserInfoBean != null) {

                        if (ackSender.isAckRequested()) {
                            Response response = new Response();
                            response.setIsSuccess(false);
                            String msg = String.format("您的账号于%s在一台%s设备上登录",
                                    dateStringFromLong(loginedUserInfoBean.getLoginTime()),
                                    loginedUserInfoBean.getDeviceType());

                            response.setMessage(msg);
                            ackSender.sendAckData(gson.toJson(response));

                            System.out.println(String.format("username:%s with %s has login!", userInfo.getUserName(),
                                    userInfo.getDeviceType()));

                            return;
                        }
                    }
                }

                UserInfoBean userInfoBean = UserInfoBean.translateFromUserInfo(userInfo,
                        client.getSessionId().toString(), mServerToken);
                userInfoDao.save(userInfoBean);

                // 踢掉其它人
                kickOffOthers(userInfoBeans, userInfo);

                client.set("SID", userInfo.getSID());
                client.set("DeviceToken", userInfo.getDeviceToken());
                client.set("DeviceType", userInfo.getDeviceType());

                if (ackSender.isAckRequested()) {
                    Response response = new Response();
                    response.setIsSuccess(true);
                    response.setMessage("登录成功");
                    ackSender.sendAckData(gson.toJson(response));
                    System.out.println(String.format("username:%s with %s login success!", userInfo.getUserName(),
                            userInfo.getDeviceType()));
                }

            }

        });

        // 登出
        this.addEventListener(EventLisenter.LOGOFF, String.class, new DataListener<String>() {

            @Override
            public void onData(SocketIOClient client, String data, AckRequest ackSender) throws Exception {

                // SDown 主动下线
                client.set("Down", "SDown");

                Gson gson = new Gson();
                UserInfo userInfo = gson.fromJson(data, UserInfo.class);

                if (ackSender.isAckRequested()) {
                    ackSender.sendAckData(gson.toJson(userInfo));
                    System.out.println(String.format("username:%s with %s logoff success!", userInfo.getUserName(),
                            userInfo.getDeviceType()));
                }
                client.disconnect();
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
                String deviceToken = client.get("DeviceToken");
                String down = client.get("Down");

                System.out
                        .println(String.format("userID %s deviceToken %s disconnect down %s", sid, deviceToken, down));

                UserInfoDao userInfoDao = new UserInfoDao(mRedisson, sid);

                UserInfoBean userInfoBean = userInfoDao.getUserInfoBean(deviceToken);

                if (userInfoBean != null) {
                    // 用户下线，清空用户数据
                    if (down != null) {
                        userInfoDao.del(deviceToken);
                    }
                    // 用户切换到后台，断开socket连接
                    else {
                        userInfoBean.setIsSocketConnected(false);
                        userInfoDao.save(userInfoBean);
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

                    String sid = kickoffClient.get("SID");
                    String deviceType = kickoffClient.get("DeviceType");
                    String deviceToken = kickoffClient.get("DeviceToken");


                    UserInfoDao userInfoDao = new UserInfoDao(mRedisson, sid);

                    UserInfoBean loginedUserInfoBean = getLoginedUserInfoBean(userInfoDao.getUserInfoBeanList(),
                            deviceType, deviceToken);

                    Response response = new Response();
                    response.setIsSuccess(true);
                    if (loginedUserInfoBean != null) {
                        String msg = String.format("您的账号于%s在一台%s设备上登录",
                                dateStringFromLong(loginedUserInfoBean.getLoginTime()),
                                loginedUserInfoBean.getDeviceType());
                        response.setMessage(msg);
                    }

                    Gson gson = new Gson();
                    String json = gson.toJson(response);

                    kickoffClient.sendEvent(Event.KICKOFF, json);
                    // KDown 被踢下线
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

                for (String receiverID : message.getReceiverIDs()) {
                    sendNews(receiverID, messageForSend);
                }

                // 发送给自己的其它平台
                UserInfoDao userInfoDao = new UserInfoDao(mRedisson, message.getSenderID());
                List<UserInfoBean> userInfoBeans = userInfoDao.getUserInfoBeanList();
                for (UserInfoBean userInfoBean : userInfoBeans) {
                    if (!userInfoBean.getDeviceToken().equals(message.getSenderDeviceToken())) {

                        SocketIOClient client = getClient(UUID.fromString(userInfoBean.getSessionID()));
                        if (client != null) {
                            client.sendEvent(Event.NEWS, new AckCallback<String>(String.class) {
                                @Override
                                public void onSuccess(String result) {
                                    System.out.println(String.format("UserID %s on %s reply %s!",
                                            userInfoBean.getUserID(), userInfoBean.getDeviceType(), result));
                                }
                            }, data);
                        }
                    }
                }

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

        // 通知当前用户的不同设备
        RTopic<String> notifyTopic = mRedisson.getTopic(Topic.NOTIFYOTHERPLATFORMS);
        notifyTopic.addListener(new MessageListener<String>() {
            @Override
            public void onMessage(String channel, String data) {
                Gson gson = new Gson();
                Notify notify = gson.fromJson(data, Notify.class);

                UserInfoDao userInfoDao = new UserInfoDao(mRedisson, notify.getUserID());

                List<UserInfoBean> userInfoBeans = userInfoDao.getUserInfoBeanList();

                for (UserInfoBean userInfoBean : userInfoBeans) {
                    if (!notify.getSourceDeviceType().equals(userInfoBean.getDeviceType())) {
                        SocketIOClient client = getClient(UUID.fromString(userInfoBean.getSessionID()));
                        if (client != null) {
                            client.sendEvent(Event.NOTIFYOTHERPLATFORMS, data);
                            System.out.println(String.format("userID:%s notify from %s to %s", notify.getUserID(),
                                    notify.getSourceDeviceType(), userInfoBean.getDeviceType()));
                        }
                    }
                }

            }
        });
    }

    /**
     * 检查其它设备登录情况
     *
     * @param userInfoBeans
     * @param deviceType
     * @param deviceToken
     * @return
     */
    private UserInfoBean getLoginedUserInfoBean(List<UserInfoBean> userInfoBeans, String deviceType, String deviceToken) {

        if (deviceType.equals(DeviceTypeEnum.PC)) {
            for (UserInfoBean userInfoBean : userInfoBeans) {
                if (userInfoBean.getDeviceType().equals(DeviceTypeEnum.PC)
                        && !userInfoBean.getDeviceToken().equals(deviceToken)) {
                    return userInfoBean;
                }
            }
        } else {
            for (UserInfoBean userInfoBean : userInfoBeans) {
                if ((userInfoBean.getDeviceType().equals(DeviceTypeEnum.ANDROID)
                        || userInfoBean.getDeviceType().equals(DeviceTypeEnum.IOS))
                        && !userInfoBean.getDeviceToken().equals(deviceToken)) {
                    return userInfoBean;
                }
            }
        }

        return null;
    }

    /**
     * 检查并踢掉相同平台下已登陆用户
     *
     * @param userInfoBeans
     * @param loginUser
     * @return
     */
    private boolean kickOffOthers(List<UserInfoBean> userInfoBeans, UserInfo loginUser) {

        UserInfoDao userInfoDao = new UserInfoDao(mRedisson, loginUser.getSID());

        for (UserInfoBean userInfoBean : userInfoBeans) {
            //PC踢PC
            if (loginUser.getDeviceType().equals(DeviceTypeEnum.PC)
                    && userInfoBean.getDeviceType().equals(DeviceTypeEnum.PC)
                    && !userInfoBean.getDeviceToken().equals(loginUser.getDeviceToken())) {

                if (userInfoBean.getIsSocketConnected()) {
                    mRedisson.getTopic(Topic.KICKOFF).publish(userInfoBean.getSessionID());
                } else {
                    userInfoDao.del(userInfoBean.getDeviceToken());
                }

                return true;
            }
            //移动踢移动
            if (!loginUser.getDeviceType().equals(DeviceTypeEnum.PC)
                    && !userInfoBean.getDeviceType().equals(DeviceTypeEnum.PC)
                    && !userInfoBean.getDeviceToken().equals(loginUser.getDeviceToken())) {

                if (userInfoBean.getIsSocketConnected()) {
                    mRedisson.getTopic(Topic.KICKOFF).publish(userInfoBean.getSessionID());
                } else {
                    userInfoDao.del(userInfoBean.getDeviceToken());
                }

                return true;
            }
        }

        return false;
    }

    /**
     * 发送消息
     *
     * @param receiverID
     * @param msg
     */
    private void sendNews(String receiverID, Message msg) {

        UserInfoDao userInfoDao = new UserInfoDao(mRedisson, receiverID);

        List<UserInfoBean> userInfoBeans = userInfoDao.getUserInfoBeanList();

        for (UserInfoBean userInfoBean : userInfoBeans) {

            // 非IOS或IOS前台在线
            if (!userInfoBean.getDeviceType().equals(DeviceTypeEnum.IOS)
                    || (userInfoBean.getDeviceType().equals(DeviceTypeEnum.IOS)
                    && userInfoBean.getIsSocketConnected())) {
                SocketIOClient client = getClient(UUID.fromString(userInfoBean.getSessionID()));
                if (client == null)
                    continue;

                System.out.println(
                        String.format("UserID %s send %s to %s", msg.getSenderID(), msg.getBody(), receiverID));

                HashSet<String> hashSet = new HashSet<>();
                hashSet.add(receiverID);
                msg.setReceiverIDs(hashSet);

                Gson gson = new Gson();
                String data = gson.toJson(msg);

                client.sendEvent(Event.NEWS, new AckCallback<String>(String.class) {
                    @Override
                    public void onSuccess(String result) {
                        System.out.println(String.format("UserID %s on %s reply %s!", receiverID,
                                userInfoBean.getDeviceType(), result));
                    }
                }, data);
            }

            // if (userInfoBean.getDeviceType().equals(DeviceTypeEnum.ANDROID))
            // {
            // SocketIOClient client =
            // getClient(UUID.fromString(userInfoBean.getSessionID()));
            // if (client == null)
            // continue;
            //
            // System.out.println(
            // String.format("UserID %s send %s to %s", msg.getSenderID(),
            // msg.getBody(), receiverID));
            //
            // HashSet<String> hashSet = new HashSet<>();
            // hashSet.add(receiverID);
            // msg.setReceiverIDs(hashSet);
            //
            // Gson gson = new Gson();
            // String data = gson.toJson(msg);
            //
            // client.sendEvent(Event.NEWS, new
            // AckCallback<String>(String.class) {
            // @Override
            // public void onSuccess(String result) {
            // System.out.println(String.format("UserID %s on Android reply
            // %s!", receiverID, result));
            // }
            // }, data);
            // } else if
            // (userInfoBean.getDeviceType().equals(DeviceTypeEnum.IOS)) {
            // SocketIOClient client =
            // getClient(UUID.fromString(userInfoBean.getSessionID()));
            //
            // if (client == null)
            // continue;
            //
            // System.out.println(
            // String.format("UserID %s send %s to %s", msg.getSenderID(),
            // msg.getBody(), receiverID));
            //
            // HashSet<String> hashSet = new HashSet<>();
            // hashSet.add(receiverID);
            // msg.setReceiverIDs(hashSet);
            //
            // Gson gson = new Gson();
            // String data = gson.toJson(msg);
            //
            // client.sendEvent(Event.NEWS, new
            // AckCallback<String>(String.class) {
            // @Override
            // public void onSuccess(String result) {
            // System.out.println(String.format("UserID %s on IOS reply %s!",
            // receiverID, result));
            // }
            // }, data);
            //
            // } else {
            // SocketIOClient client =
            // getClient(UUID.fromString(userInfoBean.getSessionID()));
            // if (client == null)
            // continue;
            //
            // System.out.println(
            // String.format("UserID %s send %s to %s", msg.getSenderID(),
            // msg.getBody(), receiverID));
            //
            // HashSet<String> hashSet = new HashSet<>();
            // hashSet.add(receiverID);
            // msg.setReceiverIDs(hashSet);
            //
            // Gson gson = new Gson();
            // String data = gson.toJson(msg);
            //
            // client.sendEvent(Event.NEWS, new
            // AckCallback<String>(String.class) {
            // @Override
            // public void onSuccess(String result) {
            // System.out.println(String.format("UserID %s on PC reply %s!",
            // receiverID, result));
            // }
            // }, data);
            // }

        }

        // IOS后台在线且最后一次登录是在该服务器上
        UserInfoBean iosUserInfoBean = IOSInbackground(receiverID);
        if (iosUserInfoBean != null && iosUserInfoBean.getServerToken().equals(mServerToken)) {
            int badge = iosUserInfoBean.getUnReadCount();
            badge++;
            iosUserInfoBean.setUnReadCount(badge);
            userInfoDao.save(iosUserInfoBean);
            sendToAPN(msg, iosUserInfoBean.getDeviceToken(), badge);
        }

    }

    /**
     * APNS推送
     *
     * @param msg
     * @param deviceToken
     */
    private void sendToAPN(Message msg, String deviceToken, int badge) {
        if (!msg.getIsAlert())
            return;
        PayloadBuilder builder = APNS.newPayload().badge(badge).alertTitle(msg.getTitle())
                .customField("othersType", msg.getOthersType()).customField("others", msg.getOthers())
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
     * ios后台在线
     *
     * @param userID
     * @return
     */
    private UserInfoBean IOSInbackground(String userID) {
        UserInfoDao userInfoDao = new UserInfoDao(mRedisson, userID);
        List<UserInfoBean> userInfoBeans = userInfoDao.getUserInfoBeanList();
        for (UserInfoBean userInfoBean : userInfoBeans) {
            if (userInfoBean.getDeviceType().equals(DeviceTypeEnum.IOS) && !userInfoBean.getIsSocketConnected()) {
                return userInfoBean;
            }
        }
        return null;
    }

    private String dateStringFromLong(long date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(new Date(date));
    }

    /**
     * 守护线程
     */
    // private class DaemonThreadFactory implements ThreadFactory {
    // public Thread newThread(Runnable r) {
    // Thread t = new Thread(r);
    // t.setDaemon(true);
    // return t;
    // }
    // }

}
