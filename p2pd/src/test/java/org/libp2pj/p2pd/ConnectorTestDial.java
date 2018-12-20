package org.libp2pj.p2pd;

import io.ipfs.multiaddr.MultiAddress;
import org.libp2pj.*;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.singletonList;

/**
 * Created by Anton Nashatyrev on 13.12.2018.
 */
public class ConnectorTestDial {

    public static void main(String[] args) throws Exception {
        startDialer("/ip4/127.0.0.1/tcp/43180", "Qmf7TGDYDgNFrRYEYHrWG6RKey9K1P7rjzS4VFJFBSLmUL");
//        if ("dial".equals(args[0])) {
//            startDialer(args[1], args[2]);
//        } else if ("listen".equals(args[0])) {
//            startListener(args[1], args[2]);
//        }
    }

    public static void startDialer(String addr, String id) throws Exception {
        Host host = new P2PDHost("/tmp/p2pd.sock");

        System.out.println("My peer id: " + host.getMyId());
        Thread.sleep(1000);
        System.out.println("My listen addresses: " + host.getListenAddresses());

        MultiAddress peerAddr = new MultiAddress(addr);
        Peer peerId = Peer.fromBase58(id);

        System.out.println("Connecting to other peer: " + peerAddr + ", " + peerId);
        CompletableFuture<Void> connect = host.connect(singletonList(peerAddr), peerId);
        connect.get(5, TimeUnit.SECONDS);
        System.out.println("Connection successful!");

        CountDownLatch sessionLatch = new CountDownLatch(1);

        System.out.println("Dialing peer " + peerId + " ...");
        host.dial(new Muxer.MuxerAdress(peerId, "testProtocol"),
                new StreamHandler<Muxer.MuxerAdress>() {
            @Override
            public void onCreate(Stream<Muxer.MuxerAdress> stream) {
                System.out.println("Stream created. Writing messages...");

                AtomicInteger counter = new AtomicInteger();
                Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
                    stream.write(ByteBuffer.wrap(("Hello from client " + counter.incrementAndGet()).getBytes()));
                    stream.flush();
                }, 0, 1, TimeUnit.SECONDS);
            }

            @Override
            public void onRead(ByteBuffer data) {
                System.out.println("Message received: " + new String(Util.byteBufferToArray(data)));
            }

            @Override
            public void onClose() {
                System.out.println("Stream closed");
                sessionLatch.countDown();
            }

            @Override
            public void onError(Throwable error) {
                System.out.println("Stream error: " + error);
                sessionLatch.countDown();
            }
        });

        System.out.println("Waiting for session end...");
        sessionLatch.await();
        System.out.println("Done.");
    }
 }
