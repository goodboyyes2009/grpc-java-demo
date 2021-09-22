package com.sunshine.grpc.example.interceptor.server;

import io.grpc.ForwardingServerCall;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author: hj
 * @date: 21-9-14 上午9:47
 */
public class OrderMgtServerCall<ReqT, RespT> extends ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT> {
    private static final Logger logger = LoggerFactory.getLogger(OrderMgtServerCall.class);

    protected OrderMgtServerCall(ServerCall<ReqT, RespT> delegate) {
        super(delegate);
    }

    @Override
    protected ServerCall<ReqT, RespT> delegate() {
        return super.delegate();
    }

    @Override
    public MethodDescriptor<ReqT, RespT> getMethodDescriptor() {
        return super.getMethodDescriptor();
    }

    @Override
    public void sendMessage(RespT message) {
        logger.info("OrderMgtServerCall Message from Server -> Client :" + message);
        super.sendMessage(message);
    }
}
