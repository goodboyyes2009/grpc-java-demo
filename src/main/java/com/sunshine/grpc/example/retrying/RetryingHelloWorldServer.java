package com.sunshine.grpc.example.retrying;

import com.sunshine.grpc.example.helloworld.GreeterGrpc;
import com.sunshine.grpc.example.helloworld.HelloReply;
import com.sunshine.grpc.example.helloworld.HelloRequest;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: hj
 * @date: 21-8-5 下午3:56
 * <p>
 * A HelloWorld server that responds to requests with UNAVAILABLE with a given percentage.
 */
public class RetryingHelloWorldServer {
    private static final Logger logger = LoggerFactory.getLogger(RetryingHelloWorldServer.class);

    private static final float UNAVAILABLE_PERCENTAGE = 0.5F;
    private static final Random random = new Random();


    private Server server;

    private void start() throws IOException {
        int port = 50051;
        server = ServerBuilder.forPort(port)
                              .addService(new GreeterImpl())
                              .build()
                              .start();

        logger.info("Server started, listening on " + port);
        DecimalFormat df = new DecimalFormat("#%");
        logger.info("Responding as UNAVAILABLE to " + df.format(UNAVAILABLE_PERCENTAGE) + " requests");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            try {
                RetryingHelloWorldServer.this.stop();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.err.println("Server shutting down");
        }));


    }

    private void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }


    public static void main(String[] args) throws InterruptedException, IOException {
        final RetryingHelloWorldServer server = new RetryingHelloWorldServer();
        server.start();
        server.blockUntilShutdown();
    }


    static class GreeterImpl extends GreeterGrpc.GreeterImplBase {
        AtomicInteger retryCounter = new AtomicInteger(0);

        @Override
        public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {

            int count = retryCounter.incrementAndGet();
            if (random.nextFloat() < UNAVAILABLE_PERCENTAGE) {
                logger.info("Returning subbed UNAVAILABLE error, count " + count);
                responseObserver.onError(Status.UNAVAILABLE.withDescription("Greeter temporarily unavailable......")
                                                           .asRuntimeException());
            } else {
                logger.info("Returning successful Hello response, count " + count);
                HelloReply helloReply = HelloReply.newBuilder().setMessage("Hello " + request.getName()).build();
                responseObserver.onNext(helloReply);
                responseObserver.onCompleted();
            }
        }
    }
}
