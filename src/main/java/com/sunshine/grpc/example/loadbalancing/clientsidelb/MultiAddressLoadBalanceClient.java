package com.sunshine.grpc.example.loadbalancing.clientsidelb;

import com.google.common.util.concurrent.ListenableFuture;
import com.sunshine.grpc.example.helloworld.GreeterGrpc;
import com.sunshine.grpc.example.helloworld.HelloReply;
import com.sunshine.grpc.example.helloworld.HelloRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.NameResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author: hj
 * @date: 21-8-16 下午3:04
 */
public class MultiAddressLoadBalanceClient {
    private static final Logger logger = LoggerFactory.getLogger(MultiAddressLoadBalanceClient.class);

    private ManagedChannel channel;
    private GreeterGrpc.GreeterFutureStub futureStub;

    static NameResolver.Factory nameResolverFactory = new MultiAddressNameResolverFactory(
            new InetSocketAddress("localhost", 50051),
            new InetSocketAddress("localhost", 50052),
            new InetSocketAddress("localhost", 50053)
    );

    public MultiAddressLoadBalanceClient() {
        channel = ManagedChannelBuilder.forTarget("server")
                                       .nameResolverFactory(new MultiAddressNameResolverFactory())
                                       .defaultLoadBalancingPolicy("round_robin")
                                       .usePlaintext()
                                       .build();
        futureStub = GreeterGrpc.newFutureStub(channel);
    }


    public void greet() {
        HelloRequest request = HelloRequest.newBuilder().setName("Hello ").build();
        ListenableFuture<HelloReply> helloReplyListenableFuture = futureStub.sayHello(request);
        helloReplyListenableFuture.addListener(() -> {
            // 完成的时候调用
            logger.info("Done:{}", helloReplyListenableFuture.isDone());
            logger.info("完成Hello调用，接收到Reply");
        }, Executors.newSingleThreadExecutor());
    }

    public static void main(String[] args) throws InterruptedException {
        MultiAddressLoadBalanceClient client = new MultiAddressLoadBalanceClient();
        while (true){
            client.greet();
            TimeUnit.SECONDS.sleep(3);
        }
    }
}
