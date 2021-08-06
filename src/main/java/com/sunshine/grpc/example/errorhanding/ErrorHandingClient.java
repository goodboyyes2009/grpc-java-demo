package com.sunshine.grpc.example.errorhanding;

import com.google.common.base.Verify;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import com.sunshine.grpc.example.helloworld.GreeterGrpc;
import com.sunshine.grpc.example.helloworld.HelloReply;
import com.sunshine.grpc.example.helloworld.HelloRequest;
import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

/**
 * Shows how to extract error information from a server response.
 *
 * @author: hj
 * @date: 21-8-5 下午5:51
 */
public class ErrorHandingClient {

    private ManagedChannel channel;

    public static void main(String[] args) throws IOException, InterruptedException {
        new ErrorHandingClient().run();
    }

    private void run() throws IOException, InterruptedException {
        // Port 0 means that the operating system will pick an available port to use.
        Server server = ServerBuilder.forPort(0).addService(new GreeterGrpc.GreeterImplBase() {
            @Override
            public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
                responseObserver.onError(Status.INTERNAL.withDescription("Eggplant Xerxes Crybaby Overbite Narwhal")
                                                        .asRuntimeException());
            }
        }).build().start();

        channel = ManagedChannelBuilder.forAddress("localhost", server.getPort()).usePlaintext().build();

        blockingCall();
        futureCallDirect();
        futureCallback();

        asyncCall();
        advancedAsyncCall();

        channel.shutdown();
        server.shutdown();
        channel.awaitTermination(1, TimeUnit.SECONDS);
        server.awaitTermination();
    }

    private void blockingCall() {
        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(channel);
        try {
            stub.sayHello(HelloRequest.newBuilder().setName("Bart").build());
        } catch (Exception e) {
            Status status = Status.fromThrowable(e);
            Verify.verify(status.getCode() == Status.Code.INTERNAL);
            Verify.verify(status.getDescription().contains("Eggplant"));
            // Cause is not transmitted over the wire.
        }
    }

    // Future 同步方式
    private void futureCallDirect() {
        GreeterGrpc.GreeterFutureStub stub = GreeterGrpc.newFutureStub(channel);
        ListenableFuture<HelloReply> response = stub.sayHello(HelloRequest.newBuilder().setName("Lisa").build());
        final CountDownLatch latch = new CountDownLatch(1);
        try {
            response.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            Status status = Status.fromThrowable(e);
            Verify.verify(status.getCode() == Status.Code.INTERNAL);
            Verify.verify(status.getDescription().contains("Xerxes"));
            // Cause is not transmitted over the wire.
            latch.countDown();
        }
        if (!Uninterruptibles.awaitUninterruptibly(latch, 1, TimeUnit.SECONDS)) {
            throw new RuntimeException("timeout!");
        }
    }

    // Future 异步方式
    private void futureCallback() {
        GreeterGrpc.GreeterFutureStub stub = GreeterGrpc.newFutureStub(channel);
        ListenableFuture<HelloReply> response = stub.sayHello(HelloRequest.newBuilder().setName("lucy").build());

        final CountDownLatch latch = new CountDownLatch(1);

        Futures.addCallback(response, new FutureCallback<HelloReply>() {
            @Override
            public void onSuccess(@NullableDecl HelloReply result) {
                // won't be called, since the server in this example always fails.
            }

            @Override
            public void onFailure(Throwable t) {
                Status status = Status.fromThrowable(t);
                Verify.verify(status.getCode() == Status.Code.INTERNAL);
                Verify.verify(status.getDescription().contains("Crybaby"));
                // Cause is not transmitted over the wire..
                latch.countDown();
            }
        }, directExecutor());

        if (!Uninterruptibles.awaitUninterruptibly(latch, 1, TimeUnit.SECONDS)) {
            throw new RuntimeException("timeout!");
        }
    }

    private void asyncCall() {
        GreeterGrpc.GreeterStub stub = GreeterGrpc.newStub(channel);
        HelloRequest request = HelloRequest.newBuilder().setName("jack").build();
        CountDownLatch latch = new CountDownLatch(1);
        StreamObserver<HelloReply> streamObserver = new StreamObserver<HelloReply>() {
            @Override
            public void onNext(HelloReply helloReply) {
                // won't be called.
            }

            @Override
            public void onError(Throwable t) {
                Status status = Status.fromThrowable(t);
                Verify.verify(status.getCode() == Status.Code.INTERNAL);
                Verify.verify(status.getDescription().contains("Overbite"));
                // Cause is not transmitted over the wire..
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                // Won't be called, since the server in this example always fails.
            }
        };

        stub.sayHello(request, streamObserver);
        if (!Uninterruptibles.awaitUninterruptibly(latch, 1, TimeUnit.SECONDS)) {
            throw new RuntimeException("timeout");
        }
    }

    private void advancedAsyncCall() {
        ClientCall<HelloRequest, HelloReply> call = channel.newCall(GreeterGrpc.getSayHelloMethod(), CallOptions.DEFAULT);
        final CountDownLatch latch = new CountDownLatch(1);
        call.start(new ClientCall.Listener<HelloReply>() {
            @Override
            public void onClose(Status status, Metadata trailers) {
                Verify.verify(status.getCode() == Status.Code.INTERNAL);
                Verify.verify(status.getDescription().contains("Narwhal"));
                // Cause is not transmitted over the wire.
                latch.countDown();
            }
        }, new Metadata());

        call.sendMessage(HelloRequest.newBuilder().setName("Marge").build());
        call.halfClose();

        if (!Uninterruptibles.awaitUninterruptibly(latch, 1, TimeUnit.SECONDS)) {
            throw new RuntimeException("timeout!");
        }
    }
}