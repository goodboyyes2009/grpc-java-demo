package com.sunshine.grpc.example.loadbalancing.clientsidelb;

import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;

import java.net.SocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: hj
 * @date: 21-8-16 下午2:55
 */
public class MultiAddressNameResolverFactory extends NameResolver.Factory {

    final List<EquivalentAddressGroup> addressGroups;

    public MultiAddressNameResolverFactory(SocketAddress... socketAddresses) {
        this.addressGroups = Arrays.stream(socketAddresses)
                                   .map(EquivalentAddressGroup::new)
                                   .collect(Collectors.toList());
    }

    @Override
    public NameResolver newNameResolver(URI targetUri, NameResolver.Args args) {
        return new NameResolver() {
            @Override
            public String getServiceAuthority() {
                return "fakeServiceAuthority";
            }

            @Override
            public void start(Listener2 listener) {
                listener.onResult(ResolutionResult.newBuilder()
                                                  .setAddresses(addressGroups)
                                                  .setAttributes(Attributes.EMPTY)
                                                  .build());
            }

            @Override
            public void shutdown() {

            }
        };
    }

    @Override
    public String getDefaultScheme() {
        return "multiAddress";
    }
}
