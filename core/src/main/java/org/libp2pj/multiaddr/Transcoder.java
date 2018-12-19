package org.libp2pj.multiaddr;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.UnknownHostException;

/**
 * Created by Anton Nashatyrev on 14.12.2018.
 */
public abstract class Transcoder {

    public abstract byte[] parseString(String s);

    public abstract String bytesToString(byte[] bytes);

    public static Transcoder ip4() {
        return new Transcoder() {
            @Override
            public byte[] parseString(String s) {
                try {
                    return Inet4Address.getByName(s).getAddress();
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public String bytesToString(byte[] bytes) {
                try {
                    return Inet4Address.getByAddress(bytes).getHostAddress();
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public static Transcoder ip6() {
        return new Transcoder() {
            @Override
            public byte[] parseString(String s) {
                try {
                    return Inet6Address.getByName(s).getAddress();
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public String bytesToString(byte[] bytes) {
                try {
                    return Inet6Address.getByAddress(bytes).getHostAddress();
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public static Transcoder tcp() {
        return new Transcoder() {
            @Override
            public byte[] parseString(String s) {
                int port = Integer.parseInt(s);
                return new byte[] {(byte) (port >> 8), (byte) port};
            }

            @Override
            public String bytesToString(byte[] bytes) {
                return Integer.toString((bytes[0] & 0xFF) << 8 | (bytes[1] & 0xFF));
            }
        };
    }

    public static Transcoder p2p() {
        return new Transcoder() {
            @Override
            public byte[] parseString(String s) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String bytesToString(byte[] bytes) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
