package bgp.tests;

import static org.junit.Assert.*;

import org.junit.Test;

import bgp.core.network.Address;

public class AddressTest {

	@Test
	public void testEqualsObject() {
		assertTrue(Address.getAddress(0x0A00002FL).equals(Address.getAddress("10.0.0.47")));
		assertFalse(Address.getAddress(0xFFFF0000L).equals(Address.getAddress(new byte[]{100,126,25,1})));
	}

	@Test
	public void testToString() {
		Address ones = Address.getAddress(0xFFFFFFFFL);
		assertEquals("255.255.255.255", ones.toString());

		Address zeroes = Address.getAddress(0x0L);
		assertEquals("0.0.0.0", zeroes.toString());
	}

	@Test
	public void testGetAddress() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetLongByteArray() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetLongString() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetAddressString() {
		for (int i = 0; i < 100; i++) {
			String address = (int)(Math.random()*256) + "."
					+ (int)(Math.random()*256) + "."
					+ (int)(Math.random()*256) + "."
					+ (int)(Math.random()*256);
			
			assertEquals(address, Address.getAddress(address).toString());
		}
	}

	@Test
	public void testGetAddressLong() {
		for (int i = 0; i < 100; i++) {
			long addressValue = (long)(Math.random() * 256*256*256*256);
			assertEquals(addressValue, Address.getAddress(addressValue).getAddress());
		}
	}

	@Test
	public void testGetAddressByteArray() {
		fail("Not yet implemented");
	}

}
