package com.sunshine.grpc.example.productinfo;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: hj
 * @date: 21-8-10 上午10:50
 */
public class ProductInfoImpl extends ProductInfoGrpc.ProductInfoImplBase {
    private Map<String, Product> productCacheMap = new ConcurrentHashMap<>();

    @Override
    public void addProduct(Product product, StreamObserver<ProductId> responseObserver) {
        // 获取产品的id
        String id = product.getId();
        ProductId productId = ProductId.newBuilder().setValue(id).build();
        productCacheMap.put(id, product);
        responseObserver.onNext(productId);
        responseObserver.onCompleted();
    }

    @Override
    public void getProduct(ProductId productId, StreamObserver<Product> responseObserver) {
        String id = productId.getValue();
        if (productCacheMap.containsKey(id)) {
            responseObserver.onNext(productCacheMap.get(id));
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(Status.NOT_FOUND.asRuntimeException());
        }
    }
}
