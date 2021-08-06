package com.sunshine.grpc.example.helloworld.client;

import com.sunshine.grpc.example.helloworld.GreeterGrpc;
import com.sunshine.grpc.example.helloworld.HelloReply;
import com.sunshine.grpc.example.helloworld.HelloRequest;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * A simple client that requests a greeting from the {@link com.sunshine.grpc.example.helloworld.server.HelloWorldServer}.
 *
 * @author: hj
 * @date: 21-7-30 下午3:30
 */
public class HelloWorldClient {
    private final Logger logger = LoggerFactory.getLogger(HelloWorldClient.class);

    private final GreeterGrpc.GreeterBlockingStub blockingStub;

    /**
     * Construct client for accessing HelloWorld server using the exist channel
     */
    public HelloWorldClient(Channel channel) {
        // 'channel' here is a Channel, not a ManagedChannel, so it is not this code's responsibility to
        // shut it down.
        blockingStub = GreeterGrpc.newBlockingStub(channel);
    }

    /**
     * say hello to the server
     */
    public void greet(String name) {
        logger.info("Will try to greet " + name + "......");
        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        HelloReply response;
        try {
            response = blockingStub.sayHello(request);
        } catch (Exception e) {
            logger.error("RPC failed:{}", e.getMessage(), e);
            return;
        }
        logger.info("Greeting " + response.getMessage());
    }

    /**
     * Greet server. If provided, the first element of {@code args} is the name to use in the greeting.
     * The second argument is the target server
     *
     * @param args
     */
    public static void main(String[] args) throws InterruptedException {

        String user = "world";

        String target = "localhost:50051";

        if (args.length > 0) {
            if ("--help".equals(args[0])) {
                System.err.println("Usage:[name [target]]");
                System.err.println();
                System.err.println(" name The name you wish to be greeted by. Defaults to" + user);
                System.err.println(" target The server to connect to. Defaults to " + target);
                System.exit(1);
            }
            user = args[0];
        }

        if (args.length > 1) {
            target = args[1];
        }

        // Create a communication channel to the server, known as a Channel. Channels are thread-safe
        // and reusable. It is common to create channels at the beginning of your application and reuse
        // them until the application shuts down.
        ManagedChannel channel = ManagedChannelBuilder.forTarget(target)
                                                      // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                                                      // needing certificates.
                                                      .usePlaintext()
                                                      .build();
        try {
            HelloWorldClient client = new HelloWorldClient(channel);
            client.greet(user);
        } finally {
            // ManagedChannels use resources like threads and TCP connections. To prevent leaking these
            // resources the channel should be shut down when it will no longer be used. If it may be used
            // again leave it running.
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }


}
