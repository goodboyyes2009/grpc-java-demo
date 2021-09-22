package com.sunshine.grpc.example.interceptor.server;

import io.grpc.ForwardingServerCallListener;
import io.grpc.ServerCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author: hj
 * @date: 21-9-14 上午9:47
 */
public class OrderMgtServerCallListener<R> extends ForwardingServerCallListener<R> {
    private static final Logger logger = LoggerFactory.getLogger(OrderMgtServerCallListener.class);

    private final ServerCall.Listener<R> delegate;

    OrderMgtServerCallListener(ServerCall.Listener<R> delegate) {
        this.delegate = delegate;
    }

    @Override
    protected ServerCall.Listener<R> delegate() {
        return delegate;
    }

    @Override
    public void onMessage(R message) {
        // logger.info("OrderMgtServerCallListener Message Received from Client -> Service " + message);
        super.onMessage(message);
    }
}
