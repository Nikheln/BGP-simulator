package bgp.tests;

import static org.junit.Assert.*;

import org.junit.Test;

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
		fail("Not yet implemented");
	}

	@Test
	public void testExtractRecipient() {
		fail("Not yet implemented");
	}

}
