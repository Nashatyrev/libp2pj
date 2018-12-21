package org.libp2pj.p2pd;

import com.google.protobuf.CodedInputStream;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import org.apache.commons.codec.binary.Hex;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by Anton Nashatyrev on 21.12.2018.
 */
public class ProtobufTest {

    @Test
    public void test1() throws Exception {
        byte[] msg = Hex.decodeHex("520800224e0a221220e5c1d006919ae0bf6fbd5249f4dfd1a9b143be0a1b20efb8761b0b13a7967ff6121429000000000000000000000000000000010696e21208047f00000106b2151208040a00020f06b215");
        ByteBuf bb = Unpooled.wrappedBuffer(msg);
        ByteBuf dup = bb.duplicate();
        ByteBufInputStream is = new ByteBufInputStream(dup);

        int msgLen = CodedInputStream.readRawVarint32(is.read(), is);
        Assert.assertEquals(msgLen, dup.readableBytes());
        Assert.assertEquals(msg.length, bb.readableBytes());
    }
}
