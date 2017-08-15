package com.ufo.NettySocketioServer;

import com.corundumstudio.socketio.store.RedissonStoreFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import com.corundumstudio.socketio.*;

public class App {

    // arg[0]ip地址,arg[1]端口,arg[2]配置文件路径
    public static void main(String[] args) throws InterruptedException, IOException {

        String ip = args[0];
        int port = Integer.valueOf(args[1]);
        String propPath = args[2];

        Properties properties = new Properties();
        InputStream in = new FileInputStream(propPath);
        properties.load(in);

        String redisConfigPath = properties.getProperty("redisConfigPath").trim();
        String p12FilePath = properties.getProperty("p12FilePath").trim();
        String p12Password = properties.getProperty("p12Password").trim();
        Boolean p12ISProduction = Boolean.valueOf(properties.getProperty("p12ISProduction").trim());

        Config redisConfig = Config.fromJSON(new File(redisConfigPath));

        RedissonClient redisson = Redisson.create(redisConfig);

        Configuration config = new Configuration();
        config.setHostname(ip);
        config.setPort(port);
        //config.setTransports(Transport.WEBSOCKET);

        config.setStoreFactory(new RedissonStoreFactory(redisson));

        MySocketIOServer server = new MySocketIOServer(config, redisson);
        server.setUp(p12FilePath, p12Password, p12ISProduction);
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
