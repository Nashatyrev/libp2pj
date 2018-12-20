package org.libp2pj;

import io.ipfs.multiaddr.MultiAddress;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Created by Anton Nashatyrev on 18.12.2018.
 */
public interface Host extends Muxer {

    Peer getMyId();

    List<MultiAddress> getListenAddresses();

    CompletableFuture<Void> connect(List<MultiAddress> peerAddresses, Peer peerId);

    @Override
    void dial(MuxerAdress muxerAdress, StreamHandler<MuxerAdress> handler);

    @Override
    CompletableFuture<Closeable> listen(MuxerAdress muxerAdress,
                                        Supplier<StreamHandler<MuxerAdress>> handlerFactory);

    void close();

//    Peerstore getPeerStore();

//    Network getNetwork();

//    ConnectionManager getConnectionManager();
}
