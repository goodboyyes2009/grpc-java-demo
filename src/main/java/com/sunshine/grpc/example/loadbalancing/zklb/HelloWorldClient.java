package com.sunshine.grpc.example.loadbalancing.zklb;

import com.sunshine.grpc.example.helloworld.GreeterGrpc;
import com.sunshine.grpc.example.helloworld.HelloReply;
import com.sunshine.grpc.example.helloworld.HelloRequest;
import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author: hj
 * @date: 21-8-12 上午11:37
 */
public class HelloWorldClient {
    private static final Logger logger = LoggerFactory.getLogger(HelloWorldClient.class);

    private final ManagedChannel channel;
    private final GreeterGrpc.GreeterBlockingStub blockingStub;

    public HelloWorldClient(String zkAddr) {
        ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder.forTarget(zkAddr)
                                                                       .defaultLoadBalancingPolicy("round_robin")
                                                                       .nameResolverFactory(new ZkNameResolverProvider())
                                                                       .usePlaintext();
        channel = channelBuilder.build();
        blockingStub = GreeterGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    /* Say hello to sever */
    public void greet() {
        HelloRequest request = HelloRequest.newBuilder().setName("world").build();
        HelloReply helloReply;

        try {
            helloReply = blockingStub.sayHello(request);
            logger.info("Greeting:" + helloReply.getMessage());
        } catch (Exception e) {
            logger.warn("RPC failed: {}", e.getMessage(), e);
            return;
        }
    }


    public static void main(String[] args) throws InterruptedException {
        if (args.length != 1) {
            System.out.println("Usage: helloworld_client zk://ADDR:PORT");
            return;
        }
        HelloWorldClient client = new HelloWorldClient(args[0]);

        try {
            while (true) {
                client.greet();
                Thread.sleep(1000);
            }
        } finally {
            client.shutdown();
        }
    }

    class ZkNameResolver extends NameResolver implements Watcher {

        /**
         * Hard-coded path to the ZkNode that knows about servers.
         * Note this must match with the path used by HelloWorldServer
         */

        public static final String PATH = "/grpc_hello_world_service";

        /**
         * 2 seconds to indicate that client disconnected
         */
        public static final int TIMEOUT_MS = 2000;

        private URI zkUri;
        private ZooKeeper zoo;
        private Listener listener;

        private final Logger logger = LoggerFactory.getLogger("ZK");

        public ZkNameResolver(URI zkUri) {
            this.zkUri = zkUri;
        }

        @Override
        public String getServiceAuthority() {
            return zkUri.getAuthority();
        }

        @Override
        public void start(Listener listener) {
            this.listener = listener;
            final CountDownLatch connectedSignal = new CountDownLatch(1);

            // 1.先连接上zookeeper
            try {
                String zkAddr = zkUri.getHost() + ":" + Integer.toString(zkUri.getPort());
                logger.info("Connecting to zookeeper Address " + zkAddr);

                this.zoo = new ZooKeeper(zkAddr, TIMEOUT_MS, new Watcher() {
                    @Override
                    public void process(WatchedEvent watchedEvent) {
                        if (watchedEvent.getState() == Event.KeeperState.SyncConnected) {
                            connectedSignal.countDown();
                        }
                    }
                });
                connectedSignal.await();
                logger.info("Connected!");
            } catch (Exception e) {
                logger.info("Failed to connect");
                return;
            }

            // 2. 判断根目录是否存在
            try {
                Stat stat = zoo.exists(PATH, true);
                if (stat == null) {
                    logger.info("PATH does not exist.");
                } else {
                    logger.info("PATH exists");
                }
            } catch (Exception e) {
                logger.info("Failed to get stat");
                return;
            }
            // 3. 获取注册的服务列表
            try {
                // final CountDownLatch connectedSignal1 = new CountDownLatch(1);
                List<String> servers = zoo.getChildren(PATH, this);
                AddServersToListener(servers);
            } catch (Exception e) {
                logger.info(e.getMessage());
            }
        }

        @Override
        public void shutdown() {
        }

        /**
         * The callback from zookeeper when servers add/remove
         */
        @Override
        public void process(WatchedEvent watchedEvent) {
            if (watchedEvent.getType() == Event.EventType.None) {
                logger.info("Connection expired");
            } else {
                try {
                    List<String> servers = zoo.getChildren(PATH, false);
                    // 检测注册服务的情况，更新服务列表
                    AddServersToListener(servers);
                    zoo.getChildren(PATH, this);
                } catch (Exception e) {
                    logger.error("process fail, {}", e.getMessage(), e);
                }
            }
        }

        private void AddServersToListener(List<String> servers) {
            List<EquivalentAddressGroup> addrs = new ArrayList<>();
            logger.info("Updating server list");
            for (String child : servers) {

                logger.info("online: " + child);
                try {
                    URI uri = new URI("zookeeper://" + child);
                    // convert uri to host and port
                    String host = uri.getHost();
                    int port = uri.getPort();

                    List<SocketAddress> socketAddresses = new ArrayList<>();
                    socketAddresses.add(new InetSocketAddress(host, port));
                    addrs.add(new EquivalentAddressGroup(socketAddresses));
                } catch (URISyntaxException e) {
                    logger.error("Unparsable server address:{}", child, e.getMessage(), e);
                }
                if (addrs.size() > 0) {
                    listener.onAddresses(addrs, Attributes.EMPTY);
                } else {
                    logger.info("No servers online. Keep looking");
                }
            }
        }
    }

    class ZkNameResolverProvider extends NameResolverProvider {
        @Override
        protected boolean isAvailable() {
            return true;
        }

        @Override
        protected int priority() {
            return 5;
        }

        @Nullable
        @Override
        public NameResolver newNameResolver(URI targetUri, NameResolver.Args args) {
            return new ZkNameResolver(targetUri);
        }

        @Override
        public String getDefaultScheme() {
            return "zk";
        }
    }
}