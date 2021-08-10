package com.sunshine.grpc.example.ordermanagement;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author: hj
 * @date: 21-8-10 下午3:22
 */
public class OrderManagementServer {
    private static final Logger logger = LoggerFactory.getLogger(OrderManagementServer.class);

    private final Server server;
    private final int port;

    public OrderManagementServer(int port) {
        this.port = port;
        server = ServerBuilder.forPort(this.port)
                              .addService(new OrderManagementImpl())
                              .build();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("Shutting down OrderManagement server since  JVM is shutting down");
            try {
                OrderManagementServer.this.stop();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.err.println("** OrderManagement server shut down **");
        }));

    }

    private void start() {
        if (server != null) {
            try {
                server.start();
                logger.info("OrderManagement Server started, listening on : {}", port);
            } catch (IOException e) {
                logger.error("启动OrderManagement Sever 失败, {}", e.getMessage(), e);
            }
        }
    }

    private void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }


    public static void main(String[] args) throws InterruptedException {
        int port = 50051;
        OrderManagementServer orderManagementServer = new OrderManagementServer(port);
        orderManagementServer.start();
        // 阻塞主进程
        orderManagementServer.blockUntilShutdown();
    }
}
