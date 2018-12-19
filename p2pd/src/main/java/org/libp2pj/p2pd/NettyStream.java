package org.libp2pj.p2pd;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.libp2pj.Muxer;
import org.libp2pj.Stream;

import java.nio.ByteBuffer;

/**
 * Created by Anton Nashatyrev on 14.12.2018.
 */
public class NettyStream implements Stream<Muxer.MuxerAdress> {

    private final Channel channel;
    private final boolean initiator;
    private final Muxer.MuxerAdress localAddress;
    private final Muxer.MuxerAdress remoteAddress;

    public NettyStream(Channel channel, boolean initiator,
                       Muxer.MuxerAdress localAddress,
                       Muxer.MuxerAdress remoteAddress) {
        this.channel = channel;
        this.initiator = initiator;
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
    }

    public NettyStream(Channel channel, boolean initiator) {
        this(channel, initiator, null, null);
    }

    @Override
    public void write(ByteBuffer data) {
        channel.write(Unpooled.wrappedBuffer(data));
    }

    @Override
    public void flush() {
        channel.flush();
    }

    @Override
    public boolean isInitiator() {
        return initiator;
    }

    @Override
    public void close() {
        channel.close();
    }

    @Override
    public Muxer.MuxerAdress getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    public Muxer.MuxerAdress getLocalAddress() {
        return localAddress;
    }
}
