package org.libp2pj.p2pd;

import org.libp2pj.Host;
import org.libp2pj.Muxer;
import org.libp2pj.Stream;
import org.libp2pj.StreamHandler;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Anton Nashatyrev on 13.12.2018.
 */
public class ConnectorTestListen {

    public static void main(String[] args) throws Exception {
        startListener();
//        if ("dial".equals(args[0])) {
//            startDialer(args[1], args[2]);
//        } else if ("listen".equals(args[0])) {
//            startListener(args[1], args[2]);
//        }
    }

    public static void startListener() throws Exception {
        Host host = new P2PDHost("/tmp/p2pd.sock.2");

        System.out.println("My peer id: " + host.getMyId());
        System.out.println("My listen addresses: " + host.getListenAddresses());

        Muxer.MuxerAdress listenAddress = Muxer.MuxerAdress.listenAddress("testProtocol");

        CountDownLatch sessionLatch = new CountDownLatch(1);

        CompletableFuture<Closeable> connect = host.listen(listenAddress, () ->
                new StreamHandler<Muxer.MuxerAdress>() {
                    @Override
                    public void onCreate(Stream<Muxer.MuxerAdress> stream) {
                        System.out.println("Stream created: " + stream);
                        System.out.println("Writing messages...");

                        AtomicInteger counter = new AtomicInteger();
                        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
                            stream.write(ByteBuffer.wrap(("Hello from server " + counter.incrementAndGet()).getBytes()));
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

        connect.whenComplete((c, t) -> {
            if (t != null) {
                System.out.println("Error binding to address");
                sessionLatch.countDown();
            } else {
                System.out.println("Listening for connections...");
            }
        });

        System.out.println("Waiting for session end...");
        sessionLatch.await();
        connect.get().close();
        System.out.println("Done.");
    }
 }
