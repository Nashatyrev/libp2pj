package org.libp2pj.p2pd;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerDomainSocketChannel;
import io.netty.channel.unix.DomainSocketAddress;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Created by Anton Nashatyrev on 13.12.2018.
 */
public class ControlConnector {
    private static final EventLoopGroup group = new EpollEventLoopGroup();
    private final int connectTimeoutSec = 5;

    public CompletableFuture<DaemonChannelHandler> connect() {
        return connect("/tmp/p2pd.sock");
    }

    public CompletableFuture<DaemonChannelHandler> connect(String socketPath) {
        CompletableFuture<DaemonChannelHandler> ret = new CompletableFuture<>();

        ChannelFuture channelFuture = new Bootstrap()
                .group(group)
                .channel(EpollDomainSocketChannel.class)
                .handler(new ChannelInit(ret::complete, true))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutSec * 1000)
                .connect(new DomainSocketAddress(socketPath));

        channelFuture.addListener((ChannelFutureListener) future -> {
            try {
                future.get();
            } catch (Exception e) {
                ret.completeExceptionally(e);
            }
        });
        return ret;
    }

    public ChannelFuture listen(String socketPath, Consumer<DaemonChannelHandler> handlersConsumer) {

        return new ServerBootstrap()
                .group(group)
                .channel(EpollServerDomainSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutSec * 1000)
                .childHandler(new ChannelInit(handlersConsumer, false))
                .bind(new DomainSocketAddress(socketPath));
    }

    private static class ChannelInit extends ChannelInitializer<EpollDomainSocketChannel> {
        private final Consumer<DaemonChannelHandler> handlersConsumer;
        private final boolean initiator;

        public ChannelInit(Consumer<DaemonChannelHandler> handlersConsumer, boolean initiator) {
            this.handlersConsumer = handlersConsumer;
            this.initiator = initiator;
        }

        @Override
        protected void initChannel(EpollDomainSocketChannel ch) throws Exception {
            DaemonChannelHandler handler = new DaemonChannelHandler(ch, initiator);
            handlersConsumer.accept(handler);
            ch.pipeline().addFirst(new SimpleChannelInboundHandler() {
                @Override
                protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
                    handler.onData((ByteBuf) msg);
                }

                @Override
                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                    handler.onError(cause);
                    super.exceptionCaught(ctx, cause);
                }
            });
        }
    }
}
