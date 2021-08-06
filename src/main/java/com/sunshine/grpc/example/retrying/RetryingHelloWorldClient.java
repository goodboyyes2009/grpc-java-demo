package com.sunshine.grpc.example.retrying;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.sunshine.grpc.example.helloworld.GreeterGrpc;
import com.sunshine.grpc.example.helloworld.HelloReply;
import com.sunshine.grpc.example.helloworld.HelloRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Charsets.UTF_8;

/**
 * @author: hj
 * @date: 21-8-5 下午4:43
 * <p>
 * A client that requests a greeting from the {@link RetryingHelloWorldServer} with a retrying policy.
 */
public class RetryingHelloWorldClient {
    private static final Logger logger = LoggerFactory.getLogger(RetryingHelloWorldClient.class);
    static final String ENV_DISABLE_RETRYING = "DISABLE_RETRYING_IN_RETRYING_EXAMPLE";

    private final boolean enableRetries;
    private final ManagedChannel channel;
    private final GreeterGrpc.GreeterBlockingStub blockingStub;
    private final AtomicInteger totalRetries = new AtomicInteger();
    private final AtomicInteger failRetries = new AtomicInteger();

    protected Map<String, ?> getRetryingServiceConfig() {
        return new Gson().fromJson(
                new JsonReader(
                        new InputStreamReader(
                                RetryingHelloWorldClient.class.getClassLoader().getResourceAsStream("retrying_service_config.json"),
                                UTF_8)),
                Map.class);
    }

    /**
     * Construct
     *
     * @param host
     * @param port
     * @param enableRetries
     */
    public RetryingHelloWorldClient(String host, int port, boolean enableRetries) {
        // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
        // needing certificates.
        ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder.forAddress(host, port).usePlaintext();

        if (enableRetries) {
            Map<String, ?> serviceConfig = getRetryingServiceConfig();
            logger.info("Client started with retrying configuration: " + serviceConfig);
            channelBuilder.defaultServiceConfig(serviceConfig);
        }
        channel = channelBuilder.build();
        blockingStub = GreeterGrpc.newBlockingStub(channel);
        this.enableRetries = enableRetries;
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(60, TimeUnit.SECONDS);
    }

    /**
     * Say hello to server in a blocking unary call.
     *
     * @param name
     */
    public void greet(String name) {
        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        HelloReply response = null;

        StatusRuntimeException statusRuntimeException = null;

        try {
            response = blockingStub.sayHello(request);
        } catch (StatusRuntimeException e) {
            failRetries.incrementAndGet();
            statusRuntimeException = e;
        }
        totalRetries.incrementAndGet();

        if (statusRuntimeException == null) {
            logger.info("Greeting: {}", response.getMessage());
        } else {
            logger.info("RPC failed: {}", statusRuntimeException.getStatus());
        }

    }

    private void printSummary() {
        logger.info("\n\nTotal RPCs sent: {}. Total RPCs failed: {}\n", totalRetries.get(), failRetries.get());
        if (enableRetries) {
            logger.info("Retrying enabled. To disable retries, run the client with environment variable {}=true.",
                    ENV_DISABLE_RETRYING);
        } else {
            logger.info(
                    "Retrying disabled. To enable retries, unset environment variable {} and then run the client.",
                    ENV_DISABLE_RETRYING);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        boolean enableRetries = !Boolean.parseBoolean(System.getenv(ENV_DISABLE_RETRYING));
        RetryingHelloWorldClient client = new RetryingHelloWorldClient("localhost", 50051, enableRetries);

        ForkJoinPool executor = new ForkJoinPool();

        for (int i = 0; i < 50; i++) {
            final String userId = "user" + i;
            executor.execute(() -> client.greet(userId));
        }
        executor.awaitQuiescence(100, TimeUnit.SECONDS);
        executor.shutdown();
        client.printSummary();
        client.shutdown();
    }
}
