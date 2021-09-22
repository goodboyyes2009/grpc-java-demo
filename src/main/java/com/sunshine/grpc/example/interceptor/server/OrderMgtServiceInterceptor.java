package com.sunshine.grpc.example.interceptor.server;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author: hj
 * @date: 21-9-13 下午4:52
 */
public class OrderMgtServiceInterceptor implements ServerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(OrderMgtServiceInterceptor.class);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        logger.info("======= [Server Interceptor] : Remote method invoked - " + call.getMethodDescriptor()
                                                                                    .getFullMethodName());

        ServerCall<ReqT, RespT> serverCall = new OrderMgtServerCall<>(call);
        return new OrderMgtServerCallListener<>(next.startCall(serverCall, headers));
    }
}
