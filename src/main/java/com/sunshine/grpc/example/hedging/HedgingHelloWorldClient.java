package com.sunshine.grpc.example.hedging;

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
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Charsets.UTF_8;

/**
 * A client that requests a greeting from the {@link HedgingHelloWorldServer} with a hedging policy.
 *
 * @author hj
 * @date 21-8-8 下午9:59
 */
public class HedgingHelloWorldClient {

    private static final Logger logger = LoggerFactory.getLogger(HedgingHelloWorldClient.class);

    private static final String ENV_DISABLE_HEDGING = "DISABLE_HEDGING_IN_HEDGING_EXAMPLE";

    private final boolean hedging;

    private final ManagedChannel channel;
    private final GreeterGrpc.GreeterBlockingStub stub;
    private final PriorityBlockingQueue<Long> latencies = new PriorityBlockingQueue<>();
    private final AtomicInteger failedRpcs = new AtomicInteger();


    /**
     * Construct client connecting to HelloWorld server at {@code host:port}.
     */
    public HedgingHelloWorldClient(String host, int port, boolean hedging) {

        ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext();
        if (hedging) {
            Map<String, ?> hedgingServiceConfig =
                    new Gson()
                            .fromJson(new JsonReader(
                                            new InputStreamReader(HedgingHelloWorldClient.class.getClassLoader().getResourceAsStream("hedging_service_config.json"), UTF_8))
                                    , Map.class);
            channelBuilder.defaultServiceConfig(hedgingServiceConfig).enableRetry();
        }
        channel = channelBuilder.build();
        stub = GreeterGrpc.newBlockingStub(channel);
        this.hedging = hedging;
    }


    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    /**
     * Say Hello to Server.
     */
    public void greet(String name) {
        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        HelloReply response = null;

        StatusRuntimeException statusRuntimeException = null;
        long startTime = System.nanoTime();

        try {
            response = stub.sayHello(request);
        } catch (StatusRuntimeException e) {
            failedRpcs.incrementAndGet();
            statusRuntimeException = e;
        }

        long latencyMills = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        latencies.offer(latencyMills);

        if (statusRuntimeException == null) {
            logger.info("Greeting {}, Latency: {}ms", response.getMessage(), latencyMills);
        } else {
            logger.info("RPC failed:{}, Latency: {}ms", statusRuntimeException.getStatus(), latencyMills);
        }
    }


    private void printSummary() {
        int rpcCount = latencies.size();

        long latency50 = 0L;
        long latency90 = 0L;
        long latency95 = 0L;
        long latency99 = 0L;
        long latency999 = 0L;
        long latencyMax = 0L;


        for (int i = 0; i < rpcCount; i++) {
            long latency = latencies.poll();
            if (i == rpcCount * 50 / 100 - 1) {
                latency50 = latency;
            }
            if (i == rpcCount * 90 / 100 - 1) {
                latency90 = latency;
            }
            if (i == rpcCount * 95 / 100 - 1) {
                latency95 = latency;
            }
            if (i == rpcCount * 99 / 100 - 1) {
                latency99 = latency;
            }
            if (i == rpcCount * 999 / 1000 - 1) {
                latency999 = latency;
            }
            if (i == rpcCount - 1) {
                latencyMax = latency;
            }
        }

        logger.info("Total RPCs sent: {}. Total RPCs failed:{}\n"
                        + (hedging ? "[Hedging enabled]\n" : "[Hedging disabled]\n")
                        + "========================\n"
                        + "50% latency: {}ms\n"
                        + "90% latency: {}ms\n"
                        + "95% latency: {}ms\n"
                        + "99% latency: {}ms\n"
                        + "99.9% latency: {}ms\n"
                        + "Max latency: {}ms\n"
                        + "========================\n",
                rpcCount, failedRpcs.get(),
                latency50, latency90, latency95, latency99, latency999, latencyMax);

        if (hedging) {
            logger.info("To disable hedging, run the client with environment variable {} = true.", ENV_DISABLE_HEDGING);
        } else {
            logger.info("To disable hedging, unset environment variable {} and then run the client.", ENV_DISABLE_HEDGING);
        }
    }


    public static void main(String[] args) throws Exception {
        boolean hedging = !Boolean.parseBoolean(System.getenv(ENV_DISABLE_HEDGING));
        final HedgingHelloWorldClient client = new HedgingHelloWorldClient("localhost", 50051, hedging);
        ForkJoinPool executor = new ForkJoinPool();

        for (int i = 0; i < 2000; i++) {
            final String userId = "user" + i;
            executor.execute(
                    () -> client.greet(userId));
        }

        executor.awaitQuiescence(100, TimeUnit.SECONDS);
        executor.shutdown();
        client.printSummary();
        client.shutdown();
    }
}
