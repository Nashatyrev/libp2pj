package org.libp2pj.p2pd;

import org.libp2pj.Peer;
import org.libp2pj.PeerInfo;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Created by Anton Nashatyrev on 13.12.2018.
 */
public class ConnectorTestDht {

    public static void main(String[] args) throws Exception {
        startDht();
    }

    public static void startDht() throws Exception {
        P2PDHost host = new P2PDHost();

        Peer myId = host.getMyId();
        System.out.println("My peer id: " + myId);

        CompletableFuture<List<PeerInfo>> peers = host.getDht().findPeersConnectedToPeer(myId);
        System.out.println("My connected peers: ");
        peers.get().forEach(p -> System.out.println("    " + p));

        System.out.println("Done.");
    }
 }
