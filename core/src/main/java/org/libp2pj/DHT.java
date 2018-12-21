package org.libp2pj;

import io.ipfs.cid.Cid;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Created by Anton Nashatyrev on 21.12.2018.
 */
public interface DHT {

    CompletableFuture<PeerInfo> findPeer(Peer peerId);

    CompletableFuture<List<PeerInfo>> findPeersConnectedToPeer(Peer peerId);

    CompletableFuture<List<PeerInfo>> findProviders(Cid cid, int maxRetCount);

    CompletableFuture<List<PeerInfo>> getClosestPeers(byte[] key);

    CompletableFuture<byte[]> getPublicKey(Peer peerId);

    CompletableFuture<byte[]> getValue(byte[] key);

    CompletableFuture<List<byte[]>> searchValue(byte[] key);

    CompletableFuture<Void> putValue(byte[] key, byte[] value);

    CompletableFuture<Void> provide(Cid cid);
}
