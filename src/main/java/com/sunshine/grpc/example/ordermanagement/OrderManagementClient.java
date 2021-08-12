package com.sunshine.grpc.example.ordermanagement;

import com.google.protobuf.StringValue;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author: hj
 * @date: 21-8-10 下午5:32
 */
public class OrderManagementClient {
    private static final Logger logger = LoggerFactory.getLogger(OrderManagementClient.class);

    private final ManagedChannel channel;
    private final OrderManagementGrpc.OrderManagementBlockingStub blockingStub;
    private final OrderManagementGrpc.OrderManagementStub asyncStub;

    public OrderManagementClient(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        blockingStub = OrderManagementGrpc.newBlockingStub(channel);
        asyncStub = OrderManagementGrpc.newStub(channel);
    }

    private static void invokerOrderUpdate(OrderManagementGrpc.OrderManagementStub asyncStub) {
        Order updOrder1 = Order.newBuilder()
                               .setId("102")
                               .addItems("Google Pixel 3A").addItems("Google Pixel Book")
                               .setDestination("Mountain View, CA")
                               .setPrice(1100)
                               .build();
        Order updOrder2 = Order.newBuilder()
                               .setId("103")
                               .addItems("Apple Watch S4").addItems("Mac Book Pro").addItems("iPad Pro")
                               .setDestination("San Jose, CA")
                               .setPrice(2800)
                               .build();
        Order updOrder3 = Order.newBuilder()
                               .setId("104")
                               .addItems("Google Home Mini").addItems("Google Nest Hub").addItems("iPad Mini")
                               .setDestination("Mountain View, CA")
                               .setPrice(2200)
                               .build();

        final CountDownLatch finishLatch = new CountDownLatch(1);

        StreamObserver<StringValue> updateOrderResponse = new StreamObserver<StringValue>() {
            @Override
            public void onNext(StringValue value) {
                logger.info("Update orders Res: " + value.getValue());
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {
                logger.info("update orders response completed!");
                finishLatch.countDown();
            }
        };

        StreamObserver<Order> orderRequestStreamObserver = asyncStub.updateOrders(updateOrderResponse);
        orderRequestStreamObserver.onNext(updOrder1);
        orderRequestStreamObserver.onNext(updOrder2);
        orderRequestStreamObserver.onNext(updOrder3);

        if (finishLatch.getCount() == 0) {
            logger.warn("RPC completed or errored before we finished sending");
            return;
        }

        orderRequestStreamObserver.onCompleted();

        // Receiving happens asynchronously
        try {
            if (finishLatch.await(10, TimeUnit.SECONDS)) {
                logger.warn("FAILED : Process orders cannot finish within 10 seconds");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private static void invokeOrderProcess(OrderManagementGrpc.OrderManagementStub asyncStub) {
        final CountDownLatch finalLatch = new CountDownLatch(1);

        StreamObserver<CombinedShipment> streamObserver = new StreamObserver<CombinedShipment>() {
            @Override
            public void onNext(CombinedShipment value) {
                logger.info("Combined Shipment : " + value.getId() + " : " + value.getOrderListList());
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {
                logger.info("Order Processing completed");
                finalLatch.countDown();
            }
        };

        StreamObserver<StringValue> requestStreamObserver = asyncStub.processOrders(streamObserver);
        requestStreamObserver.onNext(StringValue.newBuilder().setValue("102").build());
        requestStreamObserver.onNext(StringValue.newBuilder().setValue("103").build());
        requestStreamObserver.onNext(StringValue.newBuilder().setValue("104").build());
        requestStreamObserver.onNext(StringValue.newBuilder().setValue("101").build());

        if (finalLatch.getCount() == 0) {
            logger.warn("RPC completed or errored before we finished sending.");
            return;
        }

        requestStreamObserver.onCompleted();

        try {
            if (finalLatch.await(120, TimeUnit.SECONDS)) {
                logger.warn("FAILED: Process orders cannot finish within 120 seconds");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        String host = "localhost";
        int port = 50051;
        OrderManagementClient client = new OrderManagementClient(host, port);

        Order order = Order.newBuilder()
                           .setId("101")
                           .addItems("iPhone XS").addItems("Mac Book Pro")
                           .setDestination("San Jose, CA")
                           .setPrice(2300)
                           .build();

        // add order
        StringValue result = client.blockingStub.addOrder(order);
        logger.info("add order id: " + result.getValue());

        // get Order
        StringValue request = StringValue.newBuilder().setValue(order.getId()).build();
        Order resultOrder = client.blockingStub.getOrder(request);
        logger.info("get order: {}", resultOrder.toString());

        // Search orders
        StringValue stringValue = StringValue.newBuilder().setValue("Google").build();
        Iterator<Order> orderIterator = client.blockingStub.searchOrders(stringValue);
        while (orderIterator.hasNext()) {
            Order matchingOrder = orderIterator.next();
            logger.info("Search Order Response -> Matching Order - " + matchingOrder.getId());
            logger.info(" Order : " + order.getId() + "\n "
                    + matchingOrder.toString());
        }

        invokerOrderUpdate(client.asyncStub);
        invokeOrderProcess(client.asyncStub);
    }
}
