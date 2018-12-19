package org.libp2pj.p2pd;

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
public class ConnectorTest {

    public static void main(String[] args) throws Exception {
        startDialer("/ip4/127.0.0.1/tcp/43180", "QmWaWjD7Sfs7Lw7ZgMgbRN47e2iakSMuZHqPRkctHyhFzf");
//        if ("dial".equals(args[0])) {
//            startDialer(args[1], args[2]);
//        } else if ("listen".equals(args[0])) {
//            startListener(args[1], args[2]);
//        }
    }

    private static void startListener(String arg, String arg1) {

    }


    public static void startDialer(String addr, String id) throws Exception {
        Host host = new P2PDHost("/tmp/p2pd.sock");

        System.out.println("My peer id: " + host.getMyId());
        Thread.sleep(1000);
        System.out.println("My listen addresses: " + host.getListenAddresses());

        Multiaddress peerAddr = Multiaddress.fromString(addr);
        Peer peerId = Peer.fromBase58(id);

        System.out.println("Connecting to other peer: " + peerAddr + ", " + peerId);
        CompletableFuture<Void> connect = host.connect(singletonList(peerAddr), peerId);
        connect.get(5, TimeUnit.SECONDS);
        System.out.println("Connection successful!");

        CountDownLatch sessionLatch = new CountDownLatch(1);

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
//    public static void main_(String[] args) throws Exception {
//
//        ControlConnector cc = new ControlConnector();
//        System.out.println("Connecting...");
//        DaemonChannelHandler handler = cc.connect().get();
//        System.out.println("Connected.");
//
//        CompletableFuture<P2Pd.Response> resp1 = handler.call(P2Pd.Request.newBuilder()
//                .setType(P2Pd.Request.Type.IDENTIFY)
//                .build(), new DaemonChannelHandler.SimpleResponseBuilder());
//        System.out.println("Request 1 sent.");
//        CompletableFuture<P2Pd.Response> resp2 = handler.call(P2Pd.Request.newBuilder()
//                .setType(P2Pd.Request.Type.IDENTIFY)
//                .build(), new DaemonChannelHandler.SimpleResponseBuilder());
//        System.out.println("Request 2 sent.");
//        P2Pd.Response response1 = resp1.get();
//        System.out.println("Response 1 : " + response1);
//        P2Pd.Response response2 = resp2.get();
//        System.out.println("Response 2 : " + response2);
//
//        System.out.println("    Id: " + response1.getIdentify().getId() + ", " +
//                bytesToHex(response1.getIdentify().getId().toByteArray()));
//        for (ByteString addr : response1.getIdentify().getAddrsList()) {
//            System.out.println("    addr: " + addr + ", " + bytesToHex(addr.toByteArray()));
//        }
//
////        CompletableFuture<List<P2Pd.DHTResponse>> resp = handler.call(P2Pd.Request.newBuilder()
////                .setType(P2Pd.Request.Type.DHT)
////                .setDht(P2Pd.DHTRequest.newBuilder()
////                        .setType(P2Pd.DHTRequest.Type.FIND_PEERS_CONNECTED_TO_PEER)
////                        .setPeer(ByteString.copyFrom(Base58.decode("QmWaWjD7Sfs7Lw7ZgMgbRN47e2iakSMuZHqPRkctHyhFzf")))
////                        .build())
////                .build(), new DaemonChannelHandler.DHTListResponse()
////        );
////        System.out.println("DHT request sent.");
////        System.out.println("DHT resp: " + resp.get());
//
//
//        CompletableFuture<P2Pd.Response> resp = handler.call(P2Pd.Request.newBuilder()
//                        .setType(P2Pd.Request.Type.CONNECT)
//                        .setConnect(P2Pd.ConnectRequest.newBuilder()
//                                .setPeer(ByteString.copyFrom(Base58.decode("Qmf7TGDYDgNFrRYEYHrWG6RKey9K1P7rjzS4VFJFBSLmUL")))
//                                .addAddrs(ByteString.copyFrom(Multiaddress.fromString("/ip4/127.0.0.1/tcp/43180").getEncoded()))
//                                .setTimeout(5)
//                                .build()
//                        ).build(),
//                new DaemonChannelHandler.SimpleResponseBuilder());
//        System.out.println("Connect request sent.");
//        System.out.println("Connect resp: " + resp.get());
//
//        CompletableFuture<P2Pd.Response> response3 = handler.call(P2Pd.Request.newBuilder()
//                        .setType(P2Pd.Request.Type.STREAM_OPEN)
//                        .setStreamOpen(P2Pd.StreamOpenRequest.newBuilder()
//                                .setPeer(ByteString.copyFrom(Base58.decode("Qmf7TGDYDgNFrRYEYHrWG6RKey9K1P7rjzS4VFJFBSLmUL")))
//                                .addProto("test")
//                                .setTimeout(2)
//                                .build()
//                        ).build(),
//                new DaemonChannelHandler.SimpleResponseStreamBuilder());
//        System.out.println("Response 3 : " + response3.get());
//
//        DuplexStream stream = handler.getStream().get();
//        System.out.println("Writing to stream...");
//
//        new Thread(() -> {
//            try {
//                for (int i = 0; i < 1000000; i++) {
//                    stream.write(ByteBuffer.wrap((i  + ": Hello from client\n").getBytes()));
//                    Thread.sleep(1000);
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }).start();
//        CountDownLatch session = new CountDownLatch(1);
//        stream.attachHandler(new DuplexStream.Handler() {
//            @Override
//            public void onRead(ByteBuffer data) {
//                byte[] msg = new byte[data.remaining()];
//                data.get(msg);
//                System.out.println("Handler stream read: " + new String(msg));
//            }
//
//            @Override
//            public void onClose() {
//                System.out.println("Handler stream closed.");
//                session.countDown();
//            }
//
//            @Override
//            public void onError(Throwable throwable) {
//                System.out.println("Handler stream error: " + throwable);
//                session.countDown();
//            }
//        });
//
//        System.out.println("Waiting...");
//        session.await();
//    }
//
//    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
//    public static String bytesToHex(byte[] bytes) {
//        char[] hexChars = new char[bytes.length * 2];
//        for ( int j = 0; j < bytes.length; j++ ) {
//            int v = bytes[j] & 0xFF;
//            hexChars[j * 2] = hexArray[v >>> 4];
//            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
//        }
//        return new String(hexChars);
//    }
 }
