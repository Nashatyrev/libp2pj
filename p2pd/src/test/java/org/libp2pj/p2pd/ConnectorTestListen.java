package org.libp2pj.p2pd;

/**
 * Created by Anton Nashatyrev on 13.12.2018.
 */
public class ConnectorTestListen {

//    public static void main_(String[] args) throws Exception {
//        ControlConnector cc = new ControlConnector();
//        System.out.println("Connecting...");
//        DaemonChannelHandler handler = cc.connect("/tmp/p2pd.sock.2").get();
//        System.out.println("Connected.");
//
//        CompletableFuture<P2Pd.Response> resp1 = handler.call(P2Pd.Request.newBuilder()
//                .setType(P2Pd.Request.Type.IDENTIFY)
//                .build(), new DaemonChannelHandler.SimpleResponseBuilder());
//        System.out.println("Request 1 sent.");
//        P2Pd.Response response1 = resp1.get();
//        System.out.println("Response 1 : " + response1);
//
//        System.out.println("    Id: " + response1.getIdentify().getId() + ", " +
//                bytesToHex(response1.getIdentify().getId().toByteArray()));
//        for (ByteString addr : response1.getIdentify().getAddrsList()) {
//            System.out.println("    addr: " + addr + ", " + bytesToHex(addr.toByteArray()));
//        }
//
//        CountDownLatch session = new CountDownLatch(1);
//
//        ControlConnector ccListen = new ControlConnector();
//        System.out.println("Start listening...");
//        String listenPath = "/tmp/p2pd.test.listen";
//        ChannelFuture channelFuture = ccListen.listen(listenPath, (DaemonChannelHandler h) -> {
//            System.out.println("Listen handler created.");
//            CompletableFuture<P2Pd.StreamInfo> response = h.expectResponse(new DaemonChannelHandler.ListenerStreamBuilder());
//            CompletableFuture<DuplexStream> stream = h.getStream();
//            response.handle((r, t) -> {
//                System.out.println("Handler: p2pd response: " + r + ", " + t);
//                if (t!= null) t.printStackTrace();
//                return null;
//            });
//            stream.handle((s, t) -> {
//                System.out.println("Handler: stream: " + s + ", " + t);
//                if (s != null) {
//                    new Thread(() -> {
//                        try {
//                            System.out.println("Writing messages");
//                            for (int i = 0; i < 1000000; i++) {
//                                s.write(ByteBuffer.wrap((i  + ": Hello from server\n").getBytes()));
//                                Thread.sleep(1000);
//                            }
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }).start();
//
//                    s.attachHandler(new DuplexStream.Handler() {
//                        @Override
//                        public void onRead(ByteBuffer data) {
//                            byte[] msg = new byte[data.remaining()];
//                            data.get(msg);
//                            System.out.println("Handler stream read: " + new String(msg));
//                        }
//
//                        @Override
//                        public void onClose() {
//                            System.out.println("Handler stream closed.");
//                            session.countDown();
//                        }
//
//                        @Override
//                        public void onError(Throwable throwable) {
//                            System.out.println("Handler stream error: " + throwable);
//                            session.countDown();
//                        }
//                    });
//                }
//                return null;
//            });
//        });
//        channelFuture.await();
//        System.out.println("Listening.");
//
//        CompletableFuture<P2Pd.Response> resp = handler.call(P2Pd.Request.newBuilder()
//                        .setType(P2Pd.Request.Type.STREAM_HANDLER)
//                        .setStreamHandler(P2Pd.StreamHandlerRequest.newBuilder()
//                                .setPath(listenPath)
//                                .addProto("test")
//                                .build()
//                        ).build(),
//                new DaemonChannelHandler.SimpleResponseBuilder());
//        System.out.println("Listen request sent.");
//        System.out.println("Listen resp: " + resp.get());
//
//
//        System.out.println("Waiting for session complete");
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
