package org.libp2pj.multiaddr;

import org.libp2pj.util.Util;

import java.util.Arrays;
import java.util.Optional;

/**
 * Created by Anton Nashatyrev on 14.12.2018.
 */
public enum TransportProtocol {
    IP4(0x04, "ip4", 4, Transcoder.ip4()),
    IP6(0x29, "ip6", 16, Transcoder.ip6()),
    TCP(0x06, "tcp", 2, Transcoder.tcp()),
    P2P_CIRCUIT(0x0122, "p2p-circuit"),
    P2P(0x01a5, "p2p", ProtocolDataType.VAR_SIZE, Transcoder.p2p());

    public enum ProtocolDataType {
        NONE,
        FIXED_SIZE,
        VAR_SIZE,
        PATH
    }

    public static Optional<TransportProtocol> fromName(String protocolName) {
        for (TransportProtocol protocol : TransportProtocol.values()) {
            if (protocolName.toLowerCase().equals(protocol.getName().toLowerCase())) {
                return Optional.of(protocol);
            }
        }
        return Optional.empty();
    }

    public static Optional<TransportProtocol> fromVCode(byte[] arr, int off) {
        for (TransportProtocol protocol : TransportProtocol.values()) {
            byte[] vCode = protocol.getVCode();
            if (Arrays.equals(vCode, Arrays.copyOfRange(arr, off, off +  vCode.length))) {
                return Optional.of(protocol);
            }
        }
        return Optional.empty();
    }

    private final int code;
    private final String name;
    private final int size;
    private final ProtocolDataType dataType;
    private final Transcoder transcoder;

    TransportProtocol(int code, String name, ProtocolDataType dataType, Transcoder transcoder) {
        this(code, name, 0, dataType, transcoder);
    }

    TransportProtocol(int code, String name) {
        this(code, name, 0, ProtocolDataType.NONE, null);
    }

    TransportProtocol(int code, String name, int size, Transcoder transcoder) {
        this(code, name, size, ProtocolDataType.FIXED_SIZE, transcoder);
    }

    TransportProtocol(int code, String name, int size, ProtocolDataType dataType, Transcoder transcoder) {
        this.code = code;
        this.name = name;
        this.size = size;
        this.dataType = dataType;
        this.transcoder = transcoder;
    }

    public byte[] getVCode() {
        return Util.encodeUVariant(getCode());
    }

    public String getName() {
        return name;
    }

    public int getCode() {
        return code;
    }

    public int getSize() {
        return size;
    }

    public boolean hasData() {
        return getDataType() != ProtocolDataType.NONE;
    }

    public boolean isFixedLengthData() {
        return getDataType() == ProtocolDataType.NONE || getDataType() == ProtocolDataType.FIXED_SIZE;
    }

    public ProtocolDataType getDataType() {
        return dataType;
    }

    public Transcoder getTranscoder() {
        return transcoder;
    }
}
