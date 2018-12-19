package org.libp2pj;

import org.apache.commons.codec.binary.Hex;
import org.libp2pj.exceptions.MalformedMultiaddressException;
import org.libp2pj.multiaddr.Component;
import org.libp2pj.multiaddr.TransportProtocol;
import org.libp2pj.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by Anton Nashatyrev on 10.12.2018.
 */
public class Multiaddress {

    public static Multiaddress fromBytes(byte[] bytes) {
        try {
            List<Component> components = new ArrayList<>();
            int off = 0;
            while (off < bytes.length) {
                Optional<TransportProtocol> protocol = TransportProtocol.fromVCode(bytes, off);
                if (!protocol.isPresent()) {
                    throw new MalformedMultiaddressException("Can't resolve protocol in address " +
                            Hex.encodeHexString(bytes) + " at offset " + off + " (decoded components: " + components + ")");
                }
                off += protocol.get().getVCode().length;
                if (!protocol.get().isFixedLengthData()) {
                    throw new UnsupportedOperationException("Not implemented yet");
                }
                if (protocol.get().hasData()) {
                    components.add(new Component(protocol.get(),
                            Arrays.copyOfRange(bytes, off, off + protocol.get().getSize())));
                    off += protocol.get().getSize();
                } else {
                    components.add(new Component(protocol.get()));
                }
            }

            return new Multiaddress(components);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing address from bytes: " + Hex.encodeHexString(bytes), e);
        }
    }

    public static Multiaddress fromString(String multiaddress) {
        String[] ss = multiaddress.split("/");
        if (ss.length <= 1) throw new MalformedMultiaddressException(multiaddress);
        if (!ss[0].isEmpty()) throw new MalformedMultiaddressException("Multiaddress should start with /: " + multiaddress);
        int i = 1;
        List<Component> components = new ArrayList<>();
        while (i < ss.length) {
            Optional<TransportProtocol> protocol = TransportProtocol.fromName(ss[i]);
            if (!protocol.isPresent()) {
                throw new MalformedMultiaddressException("Can't resolve protocol '" + ss[i] + "' in address: " + multiaddress);
            }
            if (protocol.get().hasData()) {
                i++;
                byte[] protoData = protocol.get().getTranscoder().parseString(ss[i]);
                components.add(new Component(protocol.get(), protoData));
            } else {
                components.add(new Component(protocol.get()));
            }
            i++;
        }
        return new Multiaddress(components);
    }

    private final List<Component> components;

    private Multiaddress(List<Component> components) {
        this.components = components;
    }

    public List<Component> getComponents() {
        return components;
    }

    public byte[] getEncoded() {
        return Util.concat(getComponents().stream()
                .map(Component::getEncoded).collect(Collectors.toList()));
    }

    public String getString() {
        return "/" + getComponents().stream()
                .map(Component::getString)
                .collect(Collectors.joining("/"));
    }

    @Override
    public String toString() {
        return getString();
    }
}
