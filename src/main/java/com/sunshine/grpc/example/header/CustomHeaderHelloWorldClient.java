package com.sunshine.grpc.example.header;

import com.sunshine.grpc.example.helloworld.GreeterGrpc;
import com.sunshine.grpc.example.helloworld.HelloReply;
import com.sunshine.grpc.example.helloworld.HelloRequest;
import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * A example to create a Client with Header info.
 *
 * @author: hj
 * @date: 21-8-6 下午5:11
 */
public class CustomHeaderHelloWorldClient {
    private static final Logger logger = LoggerFactory.getLogger(CustomHeaderHelloWorldClient.class);
    private final ManagedChannel rawChannel;
    private final GreeterGrpc.GreeterBlockingStub stub;

    /**
     * Construct CustomHeaderHelloWorldClient
     *
     * @param host
     * @param port
     */
    public CustomHeaderHelloWorldClient(String host, int port) {
        // create a rawChannel
        rawChannel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        ClientInterceptor clientInterceptor = new HeaderClientInterceptor();
        Channel channelWithInterceptors = ClientInterceptors.intercept(rawChannel, clientInterceptor);
        stub = GreeterGrpc.newBlockingStub(channelWithInterceptors);
    }

    private void shutdown() throws InterruptedException {
        rawChannel.shutdown().awaitTermination(30, TimeUnit.SECONDS);
    }

    /**
     * 定义打招呼的方法
     *
     * @param name
     */
    private void greet(String name) {
        logger.info("Will try to greet " + name);
        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        HelloReply response;

        try {
            response = stub.sayHello(request);
        } catch (Exception e) {
            logger.warn("RPC failed {}", e.getMessage());
            return;
        }
        logger.info("Greeting: " + response.getMessage());
    }


    public static void main(String[] args) throws InterruptedException {
        String host = "localhost";
        int port = 50051;
        String greetUser = "张三";
        CustomHeaderHelloWorldClient client = new CustomHeaderHelloWorldClient(host, port);
        try {
            client.greet(greetUser);
        } finally {
            client.shutdown();
        }
    }
}
