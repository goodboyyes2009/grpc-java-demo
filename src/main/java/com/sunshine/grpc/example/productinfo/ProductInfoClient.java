package com.sunshine.grpc.example.productinfo;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: hj
 * @date: 21-8-10 上午11:19
 */
public class ProductInfoClient {
    private static final Logger logger = LoggerFactory.getLogger(ProductInfoClient.class);

    private final ManagedChannel channel;
    private final ProductInfoGrpc.ProductInfoBlockingStub stub;


    public ProductInfoClient(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        stub = ProductInfoGrpc.newBlockingStub(channel);
    }

    // 获取Product
    private Product getProduct(String id) {
        ProductId productId = ProductId.newBuilder().setValue(id).build();
        Product product = stub.getProduct(productId);
        return product;
    }

    // 添加Product
    private String addProduct(String id, String name, String desc, float price) {
        logger.info("add product id={}", id);
        Product product = Product.newBuilder().setId(id).setName(name).setDesc(desc).setPrice(price).build();
        ProductId productId = stub.addProduct(product);
        return productId.getValue();
    }

    public static void main(String[] args) {
        String host = "localhost";
        int port = 50051;
        ProductInfoClient client = new ProductInfoClient(host, port);
        String name = "name-";
        List<String> productIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            String id = String.valueOf(i);
            String productId = client.addProduct(id, name + id, "", 0.2F);
            productIds.add(productId);
        }

        productIds.stream().forEach(id -> {
            Product product = client.getProduct(id);
            logger.info("get Product: {}", product.toString());
        });
    }
}
