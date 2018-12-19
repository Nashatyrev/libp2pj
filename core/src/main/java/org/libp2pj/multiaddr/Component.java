package org.libp2pj.multiaddr;

import org.apache.commons.lang3.ArrayUtils;

/**
 * Created by Anton Nashatyrev on 17.12.2018.
 */
public class Component {
    private final TransportProtocol transportProtocol;
    private final byte[] data;

    public Component(TransportProtocol transportProtocol) {
        this(transportProtocol, null);
    }

    public Component(TransportProtocol transportProtocol, byte[] data) {
        this.transportProtocol = transportProtocol;
        this.data = data;
    }

    public TransportProtocol getTransportProtocol() {
        return transportProtocol;
    }

    public byte[] getData() {
        return data;
    }

    private byte[] getDataEncoded() {
        if (!getTransportProtocol().isFixedLengthData()) {
            throw new RuntimeException("Not supported yet");
        }
        return getData() == null ? new byte[0] : getData();
    }

    public String getDataAsString() {
        return getTransportProtocol().getTranscoder().bytesToString(getData());
    }

    public byte[] getEncoded() {
        return ArrayUtils.addAll(getTransportProtocol().getVCode(), getDataEncoded());
    }

    public String getString() {
        return getTransportProtocol().getName() + (getTransportProtocol().hasData() ?
                "/" + getDataAsString() : "");
    }

    @Override
    public String toString() {
        return getString();
    }
}
