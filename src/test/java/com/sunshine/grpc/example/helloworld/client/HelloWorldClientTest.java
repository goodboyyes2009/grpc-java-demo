package com.sunshine.grpc.example.helloworld.client;

import com.sunshine.grpc.example.helloworld.GreeterGrpc;
import com.sunshine.grpc.example.helloworld.HelloReply;
import com.sunshine.grpc.example.helloworld.HelloRequest;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import static org.junit.Assert.assertEquals;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 测试HelloWorldClient
 *
 * @author: hj
 * @date: 21-8-5 下午2:48
 * <p>
 * * Unit tests for {@link HelloWorldClient}.
 * * For demonstrating how to write gRPC unit test only.
 * * Not intended to provide a high code coverage or to test every major usecase.
 * *
 * * directExecutor() makes it easier to have deterministic tests.
 * * However, if your implementation uses another thread and uses streaming it is better to use
 * * the default executor, to avoid hitting bug #3084.
 * <p>
 * * <p>For more unit test examples see {@link io.grpc.examples.routeguide.RouteGuideClientTest} and
 * * {@link io.grpc.examples.routeguide.RouteGuideServerTest}.
 */

@RunWith(JUnit4.class)
public class HelloWorldClientTest {

    /**
     * This rule manages automatic graceful shutdown for the registered servers and channels at the end of test.
     */
    @Rule
    public final GrpcCleanupRule grpcCleanRule = new GrpcCleanupRule();

    private final GreeterGrpc.GreeterImplBase serviceImpl = mock(GreeterGrpc.GreeterImplBase.class, delegatesTo(new GreeterGrpc.GreeterImplBase() {
        @Override
        public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
            String name = request.getName();
            HelloReply helloReply = HelloReply.newBuilder().setMessage(name).build();
            responseObserver.onNext(helloReply);
            responseObserver.onCompleted();
        }
    }));

    private HelloWorldClient client;

    @Before
    public void setUp() throws Exception {
        // Generate a unique in-process server name.
        String serverName = InProcessServerBuilder.generateName();

        // Create a server. and services. start, and register for automatic graceful shutdown.
        grpcCleanRule.register(InProcessServerBuilder.forName(serverName)
                                                     .directExecutor()
                                                     .addService(serviceImpl)
                                                     .build()
                                                     .start());

        // Create a client channel and register for automatic graceful shutdown.
        ManagedChannel channel = grpcCleanRule.register(InProcessChannelBuilder.forName(serverName)
                                                                               .directExecutor()
                                                                               .build());

        // Create a HelloWorldClient using the in-process channel.
        client = new HelloWorldClient(channel);
    }


    /**
     * To test the client, call from the client against the fake server, and verify behaviors or state
     * changes from the server side.
     */
    @Test
    public void greet_messageDeliveredToServer() {
        ArgumentCaptor<HelloRequest> requestCaptor = ArgumentCaptor.forClass(HelloRequest.class);

        client.greet("test name");

        verify(serviceImpl).sayHello(requestCaptor.capture(), ArgumentMatchers.any());
        assertEquals("test name", requestCaptor.getValue().getName());
    }
}