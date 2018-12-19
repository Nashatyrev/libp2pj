package org.libp2pj.p2pd;

import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.libp2pj.Muxer;
import org.libp2pj.Stream;
import org.libp2pj.StreamHandler;
import p2pd.pb.P2Pd;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

/**
 * Created by Anton Nashatyrev on 14.12.2018.
 */
public class DaemonChannelHandler implements Closeable, AutoCloseable {

    private final Channel channel;
    private final boolean isInitiator;
    private Queue<ResponseBuilder> respBuildQueue = new ConcurrentLinkedQueue<>();
    private StreamHandler<Muxer.MuxerAdress> streamHandler;
    private Stream<Muxer.MuxerAdress> stream;

    public DaemonChannelHandler(Channel channel, boolean isInitiator) {
        this.channel = channel;
        this.isInitiator = isInitiator;
    }

    public void setStreamHandler(StreamHandler<Muxer.MuxerAdress> streamHandler) {
        this.streamHandler = streamHandler;
    }

    void onData(ByteBuf bytes) throws InvalidProtocolBufferException {
        if (!bytes.isReadable()) return;

        if (stream != null) {
            streamHandler.onRead(bytes.nioBuffer());
        } else {
            ResponseBuilder responseBuilder = respBuildQueue.peek();
            if (responseBuilder == null) {
                throw new RuntimeException("Unexpected response message from p2pDaemon");
            }

            // TODO currently we don't handle fragmented messages
            Action action = responseBuilder.parseNextMessage(bytes);
            if (action != Action.ContinueResponse) {
                respBuildQueue.poll();
            }

            if (action == Action.StartStream) {
                stream = new NettyStream(channel, isInitiator);
                streamHandler.onCreate(stream);
                channel.closeFuture().addListener((ChannelFutureListener) future -> streamHandler.onClose());
            }

            // in case if anything left (either another message or stream start)
            onData(bytes);
        }
    }

    void onError(Throwable t) {
        streamHandler.onError(t);
    }

    public <TResponse> CompletableFuture<TResponse> expectResponse(
            ResponseBuilder<TResponse> responseBuilder) {
        respBuildQueue.add(responseBuilder);
        return responseBuilder.getResponse();
    }

    public <TResponse> CompletableFuture<TResponse> call(P2Pd.Request request,
                                                         ResponseBuilder<TResponse> responseBuilder) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            request.writeDelimitedTo(baos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        byte[] msgBytes = baos.toByteArray();
        ByteBuf buffer = channel.alloc().buffer(msgBytes.length).writeBytes(msgBytes);
        CompletableFuture<TResponse> ret = expectResponse(responseBuilder);
        ChannelFuture channelFuture = channel.writeAndFlush(buffer);

        try {
            channelFuture.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        return ret;
    }

    public void close() {
        channel.close();
    }

    @FunctionalInterface
    public interface FunctionThrowable<A, B> {
        B apply(A arg) throws Exception;
    }

    private enum Action {
        EndResponse,
        ContinueResponse,
        StartStream
    }

    public static abstract class ResponseBuilder<TResponse> {
        protected CompletableFuture<TResponse> respFuture = new CompletableFuture<>();

        protected Action parseNextMessage(ByteBuf bytes) {
            try {
                return parseNextMessage(new ByteBufInputStream(bytes));
            } catch (Exception e) {
                respFuture.completeExceptionally(e);
                return Action.EndResponse;
            }
        }

        abstract Action parseNextMessage(InputStream is) throws Exception;

        CompletableFuture<TResponse> getResponse() {
            return respFuture;
        }
    }

    public static class SingleMsgResponseBuilder<TResponse> extends ResponseBuilder<TResponse>{
        FunctionThrowable<InputStream, TResponse> parser;

        public SingleMsgResponseBuilder(FunctionThrowable<InputStream, TResponse> parser) {
            this.parser = parser;
        }

        @Override
        Action parseNextMessage(InputStream is) {
            try {
                respFuture.complete(parser.apply(is));
            } catch (Exception e) {
                respFuture.completeExceptionally(e);
            }
            return Action.EndResponse;
        }

        CompletableFuture<TResponse> getResponse() {
            return respFuture;
        }
    }

    public static class SimpleResponseBuilder extends SingleMsgResponseBuilder<P2Pd.Response> {
        public SimpleResponseBuilder() {
            super(P2Pd.Response::parseDelimitedFrom);
        }
    }

    public static class ListenerStreamBuilder extends SingleMsgResponseBuilder<P2Pd.StreamInfo> {
        public ListenerStreamBuilder() {
            super(P2Pd.StreamInfo::parseDelimitedFrom);
        }
        @Override
        protected Action parseNextMessage(ByteBuf bytes) {
            super.parseNextMessage(bytes);
            return Action.StartStream;
        }
    }

    public static class SimpleResponseStreamBuilder extends SingleMsgResponseBuilder<P2Pd.Response> {
        public SimpleResponseStreamBuilder() {
            super(P2Pd.Response::parseDelimitedFrom);
        }

        @Override
        protected Action parseNextMessage(ByteBuf bytes) {
            super.parseNextMessage(bytes);
            try {
                if (getResponse().get().getType() == P2Pd.Response.Type.OK) {
                    return Action.StartStream;
                } else {
                    return Action.EndResponse;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class DHTListResponse extends ResponseBuilder<List<P2Pd.DHTResponse>> {
        private final List<P2Pd.DHTResponse> items = new ArrayList<>();
        private boolean started;
        @Override
        Action parseNextMessage(InputStream is) throws Exception {
            if (!started) {
                P2Pd.Response response = P2Pd.Response.parseDelimitedFrom(is);
                if (response.getType() == P2Pd.Response.Type.ERROR) {
                    throw new P2PDError("" + response.getError());
                } else {
                    if (!response.hasDht() || response.getDht().getType() != P2Pd.DHTResponse.Type.BEGIN) {
                        throw new RuntimeException("Invalid DHT list start message: " + response);
                    }
                    started = true;
                    return Action.ContinueResponse;
                }
            } else {
                P2Pd.DHTResponse response = P2Pd.DHTResponse.parseDelimitedFrom(is);
                if (response.getType() == P2Pd.DHTResponse.Type.END) {
                    respFuture.complete(items);
                    return Action.EndResponse;
                } else if (response.getType() == P2Pd.DHTResponse.Type.VALUE) {
                    items.add(response);
                    return Action.ContinueResponse;
                } else {
                    throw new RuntimeException("Invalid DHT list message: " + response);
                }
            }
        }
    }
}