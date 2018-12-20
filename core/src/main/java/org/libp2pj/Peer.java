package org.libp2pj;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.libp2pj.util.Base58;

/**
 * Created by Anton Nashatyrev on 18.12.2018.
 */
public class Peer {
    private final byte[] id;

    public static Peer fromBase58(String base58Id) {
        return new Peer(Base58.decode(base58Id));
    }

    public static Peer fromHexString(String hexId) {
        try {
            if (hexId.startsWith("0x")) {
                hexId = hexId.substring(2);
            }
            return new Peer(Hex.decodeHex(hexId));
        } catch (DecoderException e) {
            throw new RuntimeException(e);
        }
    }

    public Peer(byte[] id) {
        this.id = id;
    }

    public byte[] getIdBytes() {
        return id;
    }

    public String getIdBase58() {
        return Base58.encode(getIdBytes());
    }

    public String getIdHexString() {
        return Hex.encodeHexString(getIdBytes());
    }

    @Override
    public String toString() {
        return "Peer{" + "id=" + getIdBase58() + "}";
    }
}
