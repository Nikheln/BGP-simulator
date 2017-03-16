package bgp.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import bgp.core.messages.KeepaliveMessage;
import bgp.utils.Address;
import bgp.utils.PacketEngine;

public class PacketEngineTest {

	@Test
	public void testDecrementTTL() {
		fail("Not yet implemented");
	}

	@Test
	public void testValidatePacketHeader() {
		fail("Not yet implemented");
	}
	
	@Test
	public void testCalculateChecksum() {
		byte k = (byte)0xff;
		// Example packet from Wikipedia
		byte[] testPacket = new byte[]{
				(byte) (0x45&k), (byte) (0x00&k), (byte) (0x00&k), (byte) (0x73&k),
				(byte) (0x00&k), (byte) (0x00&k), (byte) (0x40&k), (byte) (0x00&k),
				(byte) (0x40&k), (byte) (0x11&k), (byte) (0x00&k), (byte) (0x00&k),
				(byte) (0xc0&k), (byte) (0xa8&k), (byte) (0x00&k), (byte) (0x01&k),
				(byte) (0xc0&k), (byte) (0xa8&k), (byte) (0x00&k), (byte) (0xc7&k)
		};
		assertEquals(PacketEngine.calculateChecksum(testPacket), 0xb861);
	}

	@Test
	public void testVerifyChecksum() {
		byte k = (byte)0xff;
		// Example packet from Wikipedia
		byte[] testPacket = new byte[]{
				(byte) (0x45&k), (byte) (0x00&k), (byte) (0x00&k), (byte) (0x73&k),
				(byte) (0x00&k), (byte) (0x00&k), (byte) (0x40&k), (byte) (0x00&k),
				(byte) (0x40&k), (byte) (0x11&k), (byte) (0xb8&k), (byte) (0x61&k),
				(byte) (0xc0&k), (byte) (0xa8&k), (byte) (0x00&k), (byte) (0x01&k),
				(byte) (0xc0&k), (byte) (0xa8&k), (byte) (0x00&k), (byte) (0xc7&k)
		};
		assertTrue(PacketEngine.verifyChecksum(testPacket));
	}

	@Test
	public void testExtractSender() {
		KeepaliveMessage m = new KeepaliveMessage();
		for (int i = 1; i < 256; i++) {
			for (int j = 1; j < 256; j = j + 3) {
				Address a = Address.getAddress(i + "." + j + ".0.1");
				byte[] msg = PacketEngine.buildPacket(a, a, m.serialize());
				assertEquals(a.address, PacketEngine.extractRecipient(msg));
				assertEquals(a.address, PacketEngine.extractSender(msg));
			}
		}
	}

	@Test
	public void testExtractRecipient() {
		fail("Not yet implemented");
	}

}
