package org.libp2pj.multiaddr;

import io.ipfs.multiaddr.MultiAddress;
import org.apache.commons.codec.binary.Hex;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Anton Nashatyrev on 17.12.2018.
 */
public class MultiaddressTest {

    @Test
    public void test1() throws Exception {
        {
            String addr1S = "/ip4/127.0.0.1/tcp/12345";
            MultiAddress addr1 = new MultiAddress(addr1S);
            System.out.println(Hex.encodeHex(addr1.getBytes()));
            Assert.assertEquals("047f000001063039", Hex.encodeHexString(addr1.getBytes()));
            Assert.assertEquals(addr1S, addr1.toString());
            Assert.assertEquals(addr1S, new MultiAddress(addr1.getBytes()).toString());
        }

        {
            String addr1S = "/p2p-circuit";
            MultiAddress addr1 = new MultiAddress(addr1S);
            System.out.println(Hex.encodeHex(addr1.getBytes()));
            Assert.assertEquals("a202", Hex.encodeHexString(addr1.getBytes()));
            Assert.assertEquals(addr1S, addr1.toString());
            Assert.assertEquals(addr1S, new MultiAddress(addr1.getBytes()).toString());
        }

        {
            String addr1S = "/ip6/::1/tcp/38539";
            MultiAddress addr1 = new MultiAddress(addr1S);
            System.out.println(Hex.encodeHex(addr1.getBytes()));
            Assert.assertEquals("290000000000000000000000000000000106968b", Hex.encodeHexString(addr1.getBytes()));
            Assert.assertEquals("/ip6/0:0:0:0:0:0:0:1/tcp/38539", addr1.toString());
            Assert.assertEquals("/ip6/0:0:0:0:0:0:0:1/tcp/38539", new MultiAddress(addr1.getBytes()).toString());
        }
    }
}
