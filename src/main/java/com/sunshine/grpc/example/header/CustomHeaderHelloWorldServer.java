package com.sunshine.grpc.example.header;

import com.sunshine.grpc.example.helloworld.GreeterGrpc;
import com.sunshine.grpc.example.helloworld.HelloReply;
import com.sunshine.grpc.example.helloworld.HelloRequest;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author hj
 * @date 21-8-7 下午6:38
 */
public class CustomHeaderHelloWorldServer {

    private static final Logger logger = LoggerFactory.getLogger(CustomHeaderHelloWorldServer.class);

    private static final int PORT = 50051;
    private Server server;

    private void start() throws IOException {

        server = ServerBuilder.forPort(PORT)
                .addService(ServerInterceptors.intercept(new GreeterImpl(), new HeaderServerInterceptor()))
                .build()
                .start();

        logger.info("Server started, listening on " + PORT);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            try {
                CustomHeaderHelloWorldServer.this.stop();
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }
            System.err.println("*** server shut down");
        }));
    }


    private void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(20, TimeUnit.SECONDS);
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

    // 一种GreeterImpl
    private static class GreeterImpl extends GreeterGrpc.GreeterImplBase {

        @Override
        public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
            HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + request.getName()).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
    }


    public static void main(String[] args) throws IOException, InterruptedException {
        CustomHeaderHelloWorldServer customHeaderServer = new CustomHeaderHelloWorldServer();
        customHeaderServer.start();
        customHeaderServer.blockUntilShutdown();
    }

}
