package com.sunshine.grpc.example.productinfo;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author: hj
 * @date: 21-8-10 上午10:32
 */
public class ProductInfoServer {
    private final static Logger logger = LoggerFactory.getLogger(ProductInfoServer.class);
    private final Server server;
    private int port;

    public ProductInfoServer(int port) {
        this.port = port;
        server = ServerBuilder.forPort(this.port)
                              .addService(new ProductInfoImpl())
                              .build();
        logger.info("Server started, listening on " + port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // use stderr here since the logger may have been reset by its JVM shutdown hook.
            System.err.println("shutting down gRpc server since JVM is shutting down");
            try {
                ProductInfoServer.this.stop();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.err.println("** server shut down  ***");
        }));
    }

    private void stop() throws InterruptedException {
        if (server != null){
            server.shutdown().awaitTermination(10, TimeUnit.SECONDS);
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

    public static void main(String[] args) throws IOException, InterruptedException {
        int port = 50051;
        ProductInfoServer productInfoServer = new ProductInfoServer(port);
        productInfoServer.server.start();
        productInfoServer.blockUntilShutdown();
    }
}
