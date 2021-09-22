package com.sunshine.grpc.example.interceptor.server;

import com.google.protobuf.StringValue;
import com.sunshine.grpc.example.ordermanagement.CombinedShipment;
import com.sunshine.grpc.example.ordermanagement.Order;
import com.sunshine.grpc.example.ordermanagement.OrderManagementGrpc;
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
 * @date: 21-9-13 下午4:48
 */
public class OrderMgtServiceImpl extends OrderManagementGrpc.OrderManagementImplBase {
    private static final Logger logger = LoggerFactory.getLogger(OrderMgtServiceImpl.class.getName());

    // 初始化一些订单数据
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
                              .addItems("Google Home Mini").addItems("Google Nest Hub")
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
                              .addItems("Amazon Echo").addItems("Apple iPhone XS")
                              .setDestination("Mountain View, CA")
                              .setPrice(300)
                              .build();

    private Map<String, Order> orderMap = Stream.of(
            new AbstractMap.SimpleEntry<>(ord1.getId(), ord1),
            new AbstractMap.SimpleEntry<>(ord2.getId(), ord2),
            new AbstractMap.SimpleEntry<>(ord3.getId(), ord3),
            new AbstractMap.SimpleEntry<>(ord4.getId(), ord4),
            new AbstractMap.SimpleEntry<>(ord5.getId(), ord5))
                                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    private Map<String, CombinedShipment> combinedShipmentMap = new HashMap<>();

    private static final int BATCH_SIZE = 3;


    /**
     * 添加订单数据, Unary模式
     *
     * @param request
     * @param responseObserver
     */
    @Override
    public void addOrder(Order request, StreamObserver<StringValue> responseObserver) {
        logger.info("Order Added -ID :" + request.getId() + ", Destination:" + request.getDestination());
        orderMap.put(request.getId(), request);
        StringValue id = StringValue.newBuilder().setValue("100500").build();
        responseObserver.onNext(id);
        responseObserver.onCompleted();
    }

    // Unary
    @Override
    public void getOrder(StringValue request, StreamObserver<Order> responseObserver) {
        Order order = orderMap.get(request.getValue());
        if (order == null) {
            logger.warn("Order retrieved id:{} not found", request.getValue());
            responseObserver.onError(new RuntimeException("没找到"));
            responseObserver.onCompleted();
        } else {
            logger.info("Order retrieved id: {} successfully ", request.getValue());
            responseObserver.onNext(order);
            responseObserver.onCompleted();
        }
    }

    /**
     * 通过匹配订单中的Items 获取订单
     * Server Stream 模式
     *
     * @param requestItem    待查找的item
     * @param responseOrders 响应的订单列表
     */
    @Override
    public void searchOrders(StringValue requestItem, StreamObserver<Order> responseOrders) {
        for (Map.Entry<String, Order> entry : orderMap.entrySet()) {
            Order currentOrder = entry.getValue();
            if (currentOrder.getItemsList().contains(requestItem.getValue())) {
                logger.info("Item :{} is found！", requestItem.getValue());
                responseOrders.onNext(currentOrder);
            }
        }
        // 添加所有搜索到的订单信息
        responseOrders.onCompleted();
    }

    /**
     * 更新订单信息
     * Client Stream 模式
     *
     * @param responseUpdate 返回更新订单的id,多个使用英文逗号连接
     * @return
     */
    @Override
    public StreamObserver<Order> updateOrders(StreamObserver<StringValue> responseUpdate) {
        return new StreamObserver<Order>() {
            // 初始化返回的id信息字符串
            StringBuilder updateIds = new StringBuilder().append("updated Order IDs: ");

            @Override
            public void onNext(Order value) {
                if (value != null) {
                    updateIds.append(value.getId()).append(",");
                    logger.info("Order ID:{} update！");
                }
            }

            @Override
            public void onError(Throwable t) {
                // TODO
            }

            @Override
            public void onCompleted() {
                logger.info("update Order completed!");
                updateIds.deleteCharAt(updateIds.length() - 1);
                StringValue updateResult = StringValue.newBuilder().setValue(updateIds.toString()).build();
                responseUpdate.onNext(updateResult);
                responseUpdate.onCompleted();
            }
        };
    }

    /**
     * 对订单进行合并处理
     * <p>
     * Bi-di Streaming
     *
     * @param responseObserver
     * @return
     */
    @Override
    public StreamObserver<StringValue> processOrders(StreamObserver<CombinedShipment> responseObserver) {
        return new StreamObserver<StringValue>() {
            int batchMarker = 0;

            @Override
            public void onNext(StringValue value) {
                logger.info("Order proc :ID - " + value.getValue());
                Order currentOrder = orderMap.get(value.getValue());
                if (currentOrder == null) {
                    // 没找到
                    logger.info("No Order found, ID - " + value.getValue());
                    return;
                }
                // Processing an order and increment batch marker to
                batchMarker++;
                String destination = currentOrder.getDestination();
                CombinedShipment existedCombinedShipment = combinedShipmentMap.get(destination);
                if (existedCombinedShipment != null) {
                    existedCombinedShipment = CombinedShipment.newBuilder(existedCombinedShipment)
                                                              .addOrderList(currentOrder)
                                                              .build();
                    combinedShipmentMap.put(destination, existedCombinedShipment);
                } else {
                    CombinedShipment combinedShipment = CombinedShipment.newBuilder().build();
                    CombinedShipment shipment = combinedShipment.newBuilderForType()
                                                                .addOrderList(currentOrder)
                                                                .setId("CMB-" + new Random().nextInt(1000) + ":" + currentOrder
                                                                        .getDestination())
                                                                .setStatus("Processed!")
                                                                .build();
                    combinedShipmentMap.put(currentOrder.getDestination(), shipment);
                }

                if (batchMarker == BATCH_SIZE) {
                    // Order batch completed. Flush all exist shipment.
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
