package com.sunshine.grpc.example.interceptor.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server端 Interceptor demo
 *
 * @author: hj
 * @date: 21-9-13 下午4:44
 */
public class OrderMgtServer {
    private static final Logger logger = LoggerFactory.getLogger(OrderMgtServer.class);

    private Server server;

    private void start() throws Exception {
        int port = 50051;
        server = ServerBuilder.forPort(port)
                     .addService(ServerInterceptors.intercept(new OrderMgtServiceImpl(), new OrderMgtServiceInterceptor()))
                     .build()
                     .start();
        logger.info("Server started, listening on: {}", port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            logger.info("*** shutting down gRPC server since JVM is shutting down");
            OrderMgtServer.this.stop();
            logger.info("*** server shut down");
        }));

    }

    private void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     * 后台启动，阻塞主进程
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public static void main(String[] args) throws Exception {
        final OrderMgtServer orderMgtServer = new OrderMgtServer();
        orderMgtServer.start();
        orderMgtServer.blockUntilShutdown();
    }
}
