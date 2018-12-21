package org.libp2pj.p2pd;

import com.google.protobuf.ByteString;
import io.ipfs.multiaddr.MultiAddress;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.libp2pj.*;
import p2pd.pb.P2Pd;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created by Anton Nashatyrev on 18.12.2018.
 */
public class P2PDHost implements Host {
    private AsyncDaemonExecutor daemonExecutor;

    private final int requestTimeoutSec = 5;

    public P2PDHost() {
        this("/tmp/p2pd.sock");
    }
    public P2PDHost(String domainSocketPath) {
        daemonExecutor = new AsyncDaemonExecutor(domainSocketPath);
    }

    @Override
    public DHT getDht() {
        return new P2PDDht(daemonExecutor);
    }

    @Override
    public Peer getMyId() {
        try {
            return new Peer(identify().get().getId().toByteArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<MultiAddress> getListenAddresses() {
        try {
            return identify().get().getAddrsList().stream()
                    .map(bs -> new MultiAddress(bs.toByteArray()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private CompletableFuture<P2Pd.IdentifyResponse> identify() {
        return daemonExecutor.executeWithDaemon(h ->
            h.call(P2Pd.Request.newBuilder()
                    .setType(P2Pd.Request.Type.IDENTIFY)
                    .build(), new DaemonChannelHandler.SimpleResponseBuilder())
                    .thenApply(P2Pd.Response::getIdentify)
        );
    }

    @Override
    public CompletableFuture<Void> connect(List<MultiAddress> peerAddresses, Peer peerId) {
        return daemonExecutor.executeWithDaemon(handler -> {
            CompletableFuture<P2Pd.Response> resp = handler.call(P2Pd.Request.newBuilder()
                            .setType(P2Pd.Request.Type.CONNECT)
                            .setConnect(P2Pd.ConnectRequest.newBuilder()
                                    .setPeer(ByteString.copyFrom(peerId.getIdBytes()))
                                    .addAllAddrs(peerAddresses.stream()
                                                    .map(addr -> ByteString.copyFrom(addr.getBytes()))
                                                    .collect(Collectors.toList()))
                                    .setTimeout(requestTimeoutSec)
                                    .build()
                            ).build(),
                    new DaemonChannelHandler.SimpleResponseBuilder());
            return resp.thenApply(r -> null);
        });
    }

    private final List<Closeable> activeChannels = new Vector<>();
    private final AtomicInteger counter = new AtomicInteger();

    @Override
    public CompletableFuture<Void> dial(MuxerAdress muxerAdress, StreamHandler<MuxerAdress> streamHandler) {
        try {
            return daemonExecutor.getDaemon().thenCompose(handler -> {
                try {
                    handler.setStreamHandler(new StreamHandlerWrapper<>(streamHandler)
                            .onCreate(s -> activeChannels.add(handler))
                            .onClose(() -> activeChannels.remove(handler))
                    );
                    CompletableFuture<P2Pd.Response> resp = handler.call(P2Pd.Request.newBuilder()
                                    .setType(P2Pd.Request.Type.STREAM_OPEN)
                                    .setStreamOpen(P2Pd.StreamOpenRequest.newBuilder()
                                            .setPeer(ByteString.copyFrom(muxerAdress.getPeer().getIdBytes()))
                                            .addAllProto(muxerAdress.getProtocols().stream()
                                                    .map(Protocol::getName).collect(Collectors.toList()))
                                            .setTimeout(requestTimeoutSec)
                                            .build()
                                    ).build(),
                            new DaemonChannelHandler.SimpleResponseStreamBuilder());
                    return resp.whenComplete((r, t) -> {
                        if (t != null) {
                            streamHandler.onError(t);
                            handler.close();
                        }
                    }).thenApply(r -> null);
                } catch (Exception e) {
                    handler.close();
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletableFuture<Closeable> listen(MuxerAdress muxerAdress, Supplier<StreamHandler<MuxerAdress>> handlerFactory) {
        ControlConnector ccListen = new ControlConnector();
        String listenPath = "/tmp/p2pd.client." + counter.incrementAndGet();
        ChannelFuture channelFuture = ccListen.listen(listenPath, h -> {
            StreamHandler<MuxerAdress> streamHandler = handlerFactory.get();
            h.setStreamHandler(streamHandler);
            CompletableFuture<P2Pd.StreamInfo> response = h.expectResponse(new DaemonChannelHandler.ListenerStreamBuilder());
            response.whenComplete((r, t) -> {
                if (t != null) {
                    streamHandler.onError(t);
                }
            });
        });

        channelFuture.addListener((ChannelFutureListener)
                future -> activeChannels.add(() -> future.channel().close()));

        Closeable ret = () -> channelFuture.channel().close();
        return Util.channelFutureToJava(channelFuture)
                .thenCompose(channel ->
                        daemonExecutor.executeWithDaemon(handler ->
                            handler.call(P2Pd.Request.newBuilder()
                                        .setType(P2Pd.Request.Type.STREAM_HANDLER)
                                        .setStreamHandler(P2Pd.StreamHandlerRequest.newBuilder()
                                            .setPath(listenPath)
                                            .addAllProto(muxerAdress.getProtocols().stream()
                                                .map(Protocol::getName).collect(Collectors.toList()))
                                            .build()
                                        ).build(),
                                    new DaemonChannelHandler.SimpleResponseBuilder())))
                .thenApply(resp1 -> ret);
    }

    @Override
    public void close() {
        activeChannels.forEach(ch -> {
            try {
                ch.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
