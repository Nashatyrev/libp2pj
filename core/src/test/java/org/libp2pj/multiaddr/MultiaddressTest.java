package org.libp2pj.multiaddr;

import org.apache.commons.codec.binary.Hex;
import org.junit.Assert;
import org.junit.Test;
import org.libp2pj.Multiaddress;

/**
 * Created by Anton Nashatyrev on 17.12.2018.
 */
public class MultiaddressTest {

    @Test
    public void test1() throws Exception {
        {
            String addr1S = "/ip4/127.0.0.1/tcp/12345";
            Multiaddress addr1 = Multiaddress.fromString(addr1S);
            System.out.println(Hex.encodeHex(addr1.getEncoded()));
            Assert.assertEquals("047f000001063039", Hex.encodeHexString(addr1.getEncoded()));
            Assert.assertEquals(addr1S, addr1.getString());
            Assert.assertEquals(addr1S, Multiaddress.fromBytes(addr1.getEncoded()).getString());
        }

        {
            String addr1S = "/p2p-circuit";
            Multiaddress addr1 = Multiaddress.fromString(addr1S);
            System.out.println(Hex.encodeHex(addr1.getEncoded()));
            Assert.assertEquals("a202", Hex.encodeHexString(addr1.getEncoded()));
            Assert.assertEquals(addr1S, addr1.getString());
            Assert.assertEquals(addr1S, Multiaddress.fromBytes(addr1.getEncoded()).getString());
        }

        {
            String addr1S = "/ip6/::1/tcp/38539";
            Multiaddress addr1 = Multiaddress.fromString(addr1S);
            System.out.println(Hex.encodeHex(addr1.getEncoded()));
            Assert.assertEquals("290000000000000000000000000000000106968b", Hex.encodeHexString(addr1.getEncoded()));
            Assert.assertEquals("/ip6/0:0:0:0:0:0:0:1/tcp/38539", addr1.getString());
            Assert.assertEquals("/ip6/0:0:0:0:0:0:0:1/tcp/38539", Multiaddress.fromBytes(addr1.getEncoded()).getString());
        }
    }
}
