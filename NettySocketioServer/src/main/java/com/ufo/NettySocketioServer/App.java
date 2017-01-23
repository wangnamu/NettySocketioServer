package com.ufo.NettySocketioServer;

import com.corundumstudio.socketio.store.RedissonStoreFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import com.corundumstudio.socketio.*;

public class App {

	// arg[0]端口,arg[1]是否处于debug模式
	public static void main(String[] args) throws InterruptedException, UnknownHostException, IOException {

		InetAddress addr = InetAddress.getLocalHost();
		String ip = addr.getHostAddress();

		int port = Integer.valueOf(args[0]);

		Config redisConfig;

		if (args.length > 1) {
			InputStream stream = App.class.getResourceAsStream("/redis-config.json");
			redisConfig = Config.fromJSON(stream);
		} else {
			String path = App.class.getProtectionDomain().getCodeSource().getLocation().getPath();
			path = java.net.URLDecoder.decode(path, "utf-8");
			if (path.endsWith(".jar")) {
				path = path.substring(0, path.lastIndexOf("/") + 1);
			}
			System.out.println(path);
			redisConfig = Config.fromJSON(path);
		}

		RedissonClient redisson = Redisson.create(redisConfig);

		Configuration config = new Configuration();
		config.setHostname(ip);
		config.setPort(port);

		config.setStoreFactory(new RedissonStoreFactory(redisson));

		MySocketIOServer server = new MySocketIOServer(config, redisson);
		server.setUp();
		server.start();

		while (true) {

			System.out.println(String.format("server is running at ip %s port %d!!!", ip, port));
			System.out.println(String.format("input exit to exit", ip, port));

			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String input = br.readLine();
			if ("exit".equals(input)) {
				System.out.println(String.format("please waitting to exit", ip, port));
				redisson.shutdown();
				server.stop();
				System.out.println("bye bye!");
				System.exit(0);
			}

		}

	}

}
