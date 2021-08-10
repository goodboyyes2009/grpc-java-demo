package com.sunshine.grpc.example.ordermanagement;

import com.google.protobuf.StringValue;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author: hj
 * @date: 21-8-10 下午3:32
 */
public class OrderManagementImpl extends OrderManagementGrpc.OrderManagementImplBase {
    private static final Logger logger = LoggerFactory.getLogger(OrderManagementImpl.class);

    private Order ord1 = Order.newBuilder()
                              .setId("102")
                              .addItems("Google Pixel 3A").addItems("Mac Book Pro")
                              .setDestination("Mountain View, CA")
                              .setPrice(1800)
                              .build();
    private Order ord2 = Order.newBuilder()
                              .setId("103")
                              .addItems("Apple Watch S4")
                              .setDestination("San Jose, CA")
                              .setPrice(400)
                              .build();
    private Order ord3 = Order.newBuilder()
                              .setId("104")
                              .addItems("Google Home Mini")
                              .addItems("Google Nest Hub")
                              .setDestination("Mountain View, CA")
                              .setPrice(400)
                              .build();
    private Order ord4 = Order.newBuilder()
                              .setId("105")
                              .addItems("Amazon Echo")
                              .setDestination("San Jose, CA")
                              .setPrice(30)
                              .build();
    private Order ord5 = Order.newBuilder()
                              .setId("106")
                              .addItems("Amazon Echo")
                              .addItems("Apple iPhone XS")
                              .setDestination("Mountain View, CA")
                              .setPrice(300)
                              .build();

    // 生成一些静态的订单的信息
    private Map<String, Order> orderCaches = Stream.of(new AbstractMap.SimpleEntry<>(ord1.getId(), ord1),
            new AbstractMap.SimpleEntry<>(ord2.getId(), ord2),
            new AbstractMap.SimpleEntry<>(ord3.getId(), ord3),
            new AbstractMap.SimpleEntry<>(ord4.getId(), ord4),
            new AbstractMap.SimpleEntry<>(ord5.getId(), ord5))
                                                   .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


    private Map<String, CombinedShipment> combinedShipmentMap = new HashMap<>();


    // unary
    @Override
    public void addOrder(Order request, StreamObserver<StringValue> responseObserver) {
        logger.info("Order Added  ID: {}, , Destination : {}", request.getId(), request.getDestination());
        orderCaches.put(request.getId(), request);
        StringValue value = StringValue.newBuilder().setValue("1000500").build();
        responseObserver.onNext(value);
        responseObserver.onCompleted();
    }

    // unary
    @Override
    public void getOrder(StringValue request, StreamObserver<Order> responseObserver) {
        String idValue = request.getValue();
        Order order = orderCaches.get(idValue);
        if (order != null) {
            logger.info("Order retrieved: ID=[{}]", order.getId());
            responseObserver.onNext(order);
            responseObserver.onCompleted();
        } else {
            logger.info("Order, id={}, not found.", idValue);
            responseObserver.onError(Status.NOT_FOUND.asRuntimeException());
        }
    }

    // Server Streaming
    @Override
    public void searchOrders(StringValue request, StreamObserver<Order> responseObserver) {
        for (Map.Entry<String, Order> orderEntry : orderCaches.entrySet()) {
            Order order = orderEntry.getValue();
            int itemsCount = order.getItemsCount();
            for (int idx = 0; idx < itemsCount; idx++) {
                String item = order.getItems(idx);
                if (item.contains(request.getValue())) {
                    logger.info("item found , {}", item);
                    responseObserver.onNext(order);
                    break;
                }
            }
        }
        responseObserver.onCompleted();
    }

    // Client Streaming
    @Override
    public StreamObserver<Order> updateOrders(StreamObserver<StringValue> responseObserver) {
        return new StreamObserver<Order>() {
            StringBuilder updateOrderIdBuilder = new StringBuilder().append("Update Order Ids: ");

            @Override
            public void onNext(Order value) {
                if (value != null) {
                    orderCaches.put(value.getId(), value);
                    updateOrderIdBuilder.append(value.getId()).append(",");
                    logger.info("Order Id :{} has updated", value.getId());
                }
            }

            @Override
            public void onError(Throwable t) {
                logger.info("Order ID update error " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                logger.info("update order completed");
                StringValue reply = StringValue.newBuilder().setValue(updateOrderIdBuilder.toString()).build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            }
        };

    }

    public static final int BATCH_SIZE = 3;

    // Bi-di Streaming
    @Override
    public StreamObserver<StringValue> processOrders(StreamObserver<CombinedShipment> responseObserver) {
        return new StreamObserver<StringValue>() {
            int batchMarker = 0;

            @Override
            public void onNext(StringValue value) {
                String orderId = value.getValue();
                logger.info("Order Proc: Id {}", orderId);
                Order currentOrder = orderCaches.get(orderId);
                if (currentOrder == null) {
                    logger.warn("No order found, id :{}", orderId);
                    return;
                }
                // process an order and increment batch marker to
                batchMarker++;
                String destination = currentOrder.getDestination();
                CombinedShipment existCombinedShipment = combinedShipmentMap.get(destination);
                if (existCombinedShipment == null) {
                    existCombinedShipment = CombinedShipment.newBuilder(existCombinedShipment)
                                                            .addOrderList(currentOrder)
                                                            .build();
                    combinedShipmentMap.put(destination, existCombinedShipment);
                } else {
                    CombinedShipment combinedShipment = CombinedShipment.newBuilder().build();
                    combinedShipment.newBuilderForType()
                                    .addOrderList(currentOrder)
                                    .setId("CMB-" + new Random().nextInt(1000) + ":" + currentOrder.getDestination())
                                    .setStatus("Processed")
                                    .build();
                    combinedShipmentMap.put(currentOrder.getDestination(), combinedShipment);
                }
                if (batchMarker == BATCH_SIZE) {
                    // Order batch completed. Flush all existing shipments.
                    for (Map.Entry<String, CombinedShipment> entry : combinedShipmentMap.entrySet()) {
                        responseObserver.onNext(entry.getValue());
                    }
                    // Reset batch marker
                    batchMarker = 0;
                    combinedShipmentMap.clear();
                }

            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {
                for (Map.Entry<String, CombinedShipment> entry : combinedShipmentMap.entrySet()) {
                    responseObserver.onNext(entry.getValue());
                }
                responseObserver.onCompleted();
            }
        };
    }
}
