package org.libp2pj.p2pd;

import com.google.protobuf.ByteString;
import io.ipfs.multiaddr.MultiAddress;
import io.netty.channel.ChannelFuture;
import org.libp2pj.*;
import p2pd.pb.P2Pd;

import java.io.Closeable;
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

    private final String domainSocketPath;
    private final int requestTimeoutSec = 5;

    public P2PDHost() {
        this("/tmp/p2pd.sock.2");
    }
    public P2PDHost(String domainSocketPath) {
        this.domainSocketPath = domainSocketPath;
    }

    @Override
    public Peer getMyId() {
        return new Peer(identify().getId().toByteArray());
    }

    @Override
    public List<MultiAddress> getListenAddresses() {
        return identify().getAddrsList().stream()
                .map(bs -> new MultiAddress(bs.toByteArray()))
                .collect(Collectors.toList());
    }

    private P2Pd.IdentifyResponse identify() {
        try (DaemonChannelHandler h = new ControlConnector().connect(domainSocketPath).get()) {
            CompletableFuture<P2Pd.Response> resp1 = h.call(P2Pd.Request.newBuilder()
                    .setType(P2Pd.Request.Type.IDENTIFY)
                    .build(), new DaemonChannelHandler.SimpleResponseBuilder());
            if (resp1.get().getType() == P2Pd.Response.Type.ERROR) {
                throw new P2PDError(resp1.get().getError().toString());
            } else {
                return resp1.get().getIdentify();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletableFuture<Void> connect(List<MultiAddress> peerAddresses, Peer peerId) {
        try (DaemonChannelHandler handler = new ControlConnector().connect(domainSocketPath).get()) {
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
            return resp.thenApply(resp1 -> {
                if (resp1.getType() == P2Pd.Response.Type.ERROR) {
                    throw new P2PDError(resp1.getError().toString());
                } else {
                    return null;
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final List<Closeable> activeChannels = new Vector<>();

    @Override
    public void dial(MuxerAdress muxerAdress, StreamHandler<MuxerAdress> streamHandler) {
        try {
            DaemonChannelHandler handler = new ControlConnector().connect(domainSocketPath).get();
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
                resp.whenComplete((r, t) -> {
                    Throwable anyError = t;

                    if (r != null && r.getType() == P2Pd.Response.Type.ERROR) {
                        anyError = new P2PDError(r.getError().toString());
                    }

                    if (anyError != null) {
                        streamHandler.onError(anyError);
                        handler.close();
                    }
                });
            } catch (Exception e) {
                handler.close();
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    AtomicInteger counter = new AtomicInteger();

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

        return Util.channelFutureToJava(channelFuture).thenCompose(channel -> {
            try (DaemonChannelHandler handler = new ControlConnector().connect(domainSocketPath).get()) {
                CompletableFuture<P2Pd.Response> resp = handler.call(P2Pd.Request.newBuilder()
                                .setType(P2Pd.Request.Type.STREAM_HANDLER)
                                .setStreamHandler(P2Pd.StreamHandlerRequest.newBuilder()
                                        .setPath(listenPath)
                                        .addAllProto(muxerAdress.getProtocols().stream()
                                                .map(Protocol::getName).collect(Collectors.toList()))
                                        .build()
                                ).build(),
                        new DaemonChannelHandler.SimpleResponseBuilder());

                return resp.thenApply(resp1 -> {
                    if (resp1.getType() == P2Pd.Response.Type.ERROR) {
                        channel.close();
                        throw new P2PDError(resp1.getError().toString());
                    } else {
                        return () -> channelFuture.channel().close();
                    }
                });
            } catch (Exception e) {
                channel.close();
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void close() {

    }
}
