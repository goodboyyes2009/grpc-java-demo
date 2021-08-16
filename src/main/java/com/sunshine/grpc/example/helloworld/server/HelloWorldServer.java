package com.sunshine.grpc.example.helloworld.server;

import com.sunshine.grpc.example.helloworld.GreeterGrpc;
import com.sunshine.grpc.example.helloworld.HelloReply;
import com.sunshine.grpc.example.helloworld.HelloRequest;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * HelloWorld Sever
 *
 * @author: hj
 * @date: 21-7-29 下午3:32
 */
public class HelloWorldServer {
    private final Logger logger = LoggerFactory.getLogger(HelloWorldServer.class);

    private Server server;

    private void start(int port, String name) throws Exception {

        server = ServerBuilder.forPort(port)
                              .addService(new GreeterImpl(name))
                              .build().start();
        logger.info("Server started, listening on " + port);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("shutting down gRpc server since JVM is shutting down");
                try {
                    HelloWorldServer.this.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.err.println("** server shut down  ***");
            }
        });
    }

    private void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     *
     * @throws InterruptedException
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }


    // 定义一个GreeterImpl
    public static class GreeterImpl extends GreeterGrpc.GreeterImplBase {
        private final String name;

        public GreeterImpl(String name) {
            this.name = name;
        }

        @Override
        public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
            HelloReply helloReply = HelloReply.newBuilder()
                                              .setMessage("Hello " + request.getName() + ", process by " + this.name)
                                              .build();
            responseObserver.onNext(helloReply);
            responseObserver.onCompleted();
        }
    }


    public static void main(String[] args) throws Exception {
        final HelloWorldServer server = new HelloWorldServer();
        final int port = 50051;
        String name = "server";
        server.start(port, name);
        server.blockUntilShutdown();
    }
}
