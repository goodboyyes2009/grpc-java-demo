package com.sunshine.grpc.example.ordermanagement;

import com.google.protobuf.StringValue;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * @author: hj
 * @date: 21-8-10 下午5:32
 */
public class OrderManagementClient {
    private static final Logger logger = LoggerFactory.getLogger(OrderManagementClient.class);
    private String host;
    private int port;
    private final ManagedChannel channel;
    private final OrderManagementGrpc.OrderManagementBlockingStub blockingStub;
    private final OrderManagementGrpc.OrderManagementStub asyncStub;

    public OrderManagementClient(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        blockingStub = OrderManagementGrpc.newBlockingStub(channel);
        asyncStub = OrderManagementGrpc.newStub(channel);

        Order order = Order.newBuilder()
                           .setId("101")
                           .addItems("iPhone XS").addItems("Mac Book Pro")
                           .setDestination("San Jose, CA")
                           .setPrice(2300)
                           .build();

        // add order
        StringValue result = blockingStub.addOrder(order);
        logger.info("add order id: " + result.getValue());

        // get Order
        StringValue request = StringValue.newBuilder().setValue(order.getId()).build();
        Order resultOrder = blockingStub.getOrder(request);
        logger.info("get order: {}", resultOrder.toString());

        // Search orders
        StringValue stringValue = StringValue.newBuilder().setValue("Google").build();
        Iterator<Order> orderIterator = blockingStub.searchOrders(stringValue);
        while (orderIterator.hasNext()){
            Order matchingOrder = orderIterator.next();
            logger.info("Search Order Response -> Matching Order - " + matchingOrder.getId());
            logger.info(" Order : " + order.getId() + "\n "
                    + matchingOrder.toString());
        }
    }

    // 

}
