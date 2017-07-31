NettySocketioServer
===========================

该程序由java语言编写，基于[netty-socketio]开源项目和[Redis]，旨在实现WebSocket的服务端功能。

****
#### Author:wangnamu
#### E-mail:wangstin@sina.com
****

## Requirements
* JDK 1.8 or later

## Getting Started
### 适用范围

Web系统与手机APP之间的通讯，每个账户支持一台PC设备和一台手机设备同时在线

### 登录账户工作原理（详见代码）

1. 生成唯一的设备标识DeviceToken
2. socket connect 成功回调中进行登录
3. 传入userInfo信息
4. `检查是否存在其它设备登录（判定标识为每个账户支持一台PC设备和一台手机设备同时在线），如存在则返回登录失败信息`
5. `服务端存入用户信息`
6. `服务端踢掉与当前登录设备类型相同的已连接的其它设备`
7. 被踢掉的客户端收到通知

### 分布式

本程序支持分布式，client与server通过redis的发布订阅进行通信，结构如下：

| # | # | # |
|:----------:|:-----------:|:-----------:|
| client1 | client2 | client3 | 
| >>>>>>>>>> |>>>>>redis<<<<<| <<<<<<<<<< |
| socketioServer1 | socketioServer2 | socketioServer3 | 

## How To Use

* 去[Redis]官网下载并安装Redis

* 运行redis server

* 编译程序并导出可执行jar包

* 编写配置文件 config.properties

* 在导出的jar包目录下打开控制台

* 运行命令： java -jar socket.jar 'ip' 'port' 'config.properties 绝对路径'

```
   config.properties文件：
   
   redisConfigPath=xxx          #redis-config.json文件绝对路径
   p12FilePath=xxx              #.p12文件绝对路径（供IOS推送使用）
   p12Password=xxx              #.p12密码（供IOS推送使用）
   p12ISProduction=true/false   #.p12测试／正式通道（供IOS推送使用）
   
```

## Example

### Javascript

```javascript
<script src="https://cdnjs.cloudflare.com/ajax/libs/socket.io/1.7.3/socket.io.min.js"></script>
```

```javascript
var socket = io.connect('http://xxx.xxx.xxx.xxx:xxxx');

//连接
socket.on('connect',function () {
    login();
 });

//登录
function login() {
    var userInfo = {};  //详见UserInfo类
    socket.emit('login', jsonObject, function (data) {
        console.log(data);  //data详见Response类
    });
}
```

```javascript
//被踢下线
socket.on('kickoff', function (data) {
    console.log(data);  //data详见Response类
});
```

```javascript
//收到消息
socket.on('news', function (data, ackServerCallback) {
     console.log(data);  //data详见Message类
     if (ackServerCallback) {
        ackServerCallback('success');
     }
});
```

```javascript
//断线
socket.on('disconnect', function () {
});
```

### Java Model

* Message

```Java
// 主键
private String SID;
// 发送人ID
private String SenderID;
// 发送人设备编号
private String SenderDeviceToken;
// 接收人ID
private HashSet<String> ReceiverIDs;
// 标题
private String Title;
// 内容
private String Body;
// 时间
private long Time;
// 消息类型(文字、图片、文件、链接、音频、视频、表情等)
private String MessageType;
// 提醒
private Boolean IsAlert;
// 针对ios10和androidN的快捷回复功能，Category字段表示快捷方式分类
private String Category = "custom";
// 自定义内容类型
private String OthersType;
// 自定义内容
private Object Others;
```

* UserInfo 

```Java
// 主键
private String SID;
// 用户名
private String UserName;
// 真实姓名
private String NickName;
// 最近一次登录时间
private long LoginTime;
// 设备证书
private String DeviceToken;
// 设备类型
private String DeviceType;
// 检查登录状态
private Boolean CheckStatus = false;
```

* Response

```Java
//是否成功
private Boolean IsSuccess;
//信息
private String Message;
```

## Dependencies

| Name | Address |
| :---------- | :-----------|
| netty-socketio   | https://github.com/mrniko/netty-socketio |
| redisson   | https://github.com/redisson/redisson   |
| gson   | https://github.com/google/gson   |
| slf4j   | https://www.slf4j.org/   |




***************************
[redis]:https://redis.io/
[netty-socketio]:https://github.com/mrniko/netty-socketio
