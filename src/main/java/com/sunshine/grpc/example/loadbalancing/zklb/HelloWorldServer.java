package com.sunshine.grpc.example.loadbalancing.zklb;

import com.sunshine.grpc.example.helloworld.GreeterGrpc;
import com.sunshine.grpc.example.helloworld.HelloReply;
import com.sunshine.grpc.example.helloworld.HelloRequest;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author: hj
 * @date: 21-8-12 上午11:37
 */
public class HelloWorldServer {
    static String portStr;
    private static final Logger logger = LoggerFactory.getLogger(HelloWorldServer.class);

    private Server server;

    private void start(String port) throws IOException {
        server = ServerBuilder.forPort(Integer.parseInt(port))
                              .addService(new GreeterImpl())
                              .build().start();
        logger.info("HelloSever started listening on :{}", port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("Server shut down since JVM is shutting down");
            try {
                HelloWorldServer.this.stop();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.err.println("**** Server shutdown ***");
        }));
    }


    private void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }


    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }


    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length != 2) {
            System.out.println("Usage: hello_server PORT zk://ADDR:PORT");
            return;
        }

        String zkAddr;

        try {
            portStr = args[0];
            zkAddr = args[1];
        } catch (Exception e) {
            System.out.println("Usage: hello_server PORT zk://ADDR:PORT");
            return;
        }

        //注册服务到zookeeper
        ZookeeperConnection zkConnection = new ZookeeperConnection();
        if (!zkConnection.connect(zkAddr, "localhost", portStr)){
            return;
        }

        final HelloWorldServer server = new HelloWorldServer();
        server.start(portStr);
        server.blockUntilShutdown();
    }

    static class GreeterImpl extends GreeterGrpc.GreeterImplBase {
        @Override
        public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
            HelloReply helloReply = HelloReply.newBuilder()
                                              .setMessage("Hello " + request.getName() + " from " + portStr)
                                              .build();
            responseObserver.onNext(helloReply);
            responseObserver.onCompleted();
        }
    }

    static class ZookeeperConnection {
        private final Logger logger = LoggerFactory.getLogger(ZookeeperConnection.class);
        private ZooKeeper zooKeeper;

        /**
         * Connect to a zookeeper ensemble in zkUriStr.
         *
         * @return
         */
        public boolean connect(String zkUriStr, String serverIp, String portStr) throws IOException {
            final CountDownLatch connectSignal = new CountDownLatch(1);

            String zkHostPort;
            try {
                URI uri = new URI(zkUriStr);
                zkHostPort = uri.getHost() + ":" + Integer.toString(uri.getPort());
            } catch (Exception e) {
                logger.error("Cloud not parse zk URI:{}", zkUriStr, e);
                return false;
            }

            zooKeeper = new ZooKeeper(zkHostPort, 5000, new Watcher() {
                @Override
                public void process(WatchedEvent watchedEvent) {
                    if (watchedEvent.getState() == Event.KeeperState.SyncConnected) {
                        connectSignal.countDown();
                    }
                }
            });
            /** Wait for zookeeper connect */
            String serverPath = "/grpc_hello_world_service";
            Stat stat;

            String currTime = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());

            try {
                stat = zooKeeper.exists(serverPath, true);
                if (stat == null) {
                    zooKeeper.create(serverPath, currTime.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                }
            } catch (Exception e) {
                logger.error("fail to create path:{}", serverPath, e);
                return false;
            }

            String serverAddr = serverPath + "/" + serverIp + ":" + portStr;

            try {
                stat = zooKeeper.exists(serverAddr, true);
                if (stat == null) {
                    // 不存在创建
                    try {
                        zooKeeper.create(serverAddr, currTime.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
                    } catch (KeeperException e) {
                        logger.error("fail to create path:{}", serverAddr, e);
                        return false;
                    }
                } else {
                    // 存在更新数据
                    try {
                        zooKeeper.setData(serverAddr, currTime.getBytes(), stat.getVersion());
                    } catch (Exception e) {
                        logger.error("Failed to update server data");
                        return false;
                    }
                }
            } catch (Exception e) {
                logger.error("fail to create path:{}, or update date", serverAddr, e);
                return false;
            }
            return true;
        }

        private void close() throws InterruptedException {
            zooKeeper.close();
        }
    }
}
