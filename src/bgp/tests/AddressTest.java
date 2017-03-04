package bgp.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import bgp.utils.Address;

@RunWith(Parameterized.class)
public class AddressTest {
	
	@Parameter(0)
	public String addressString;
	@Parameter(1)
	public long addressLong;
	
	@Parameters
	public static Collection<Object[]> generateData() {
		List<Object[]> data = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			String addString = "";
			long addLong = 0L;
			for (int j = 0; j < 4; j++) {
				int next = (int)(Math.random()*256);
				addString += next;
				if (j < 3) {
					addString += ".";
				}
				addLong = addLong*256 + next;
			}
			data.add(new Object[]{addString, addLong});
		}
		
		return data;
	}

	@Test
	public void testGetAddressLong() {
		assertEquals(addressLong, Address.getAddress(addressLong).getAddress());
	}

	@Test
	public void testEqualsObject() {
		assertTrue(Address.getAddress(addressString).equals(Address.getAddress(addressLong)));
		assertTrue(Address.getAddress(addressLong).equals(Address.getAddress(addressString)));
		assertFalse(Address.getAddress(addressLong).equals(Address.getAddress(addressLong + (int)((Math.random()-0.5)*10000))));
	}

	@Test
	public void testToString() {
		assertEquals(addressString, Address.getAddress(addressLong).toString());
	}

	@Test
	public void testGetAddressString() {
		assertEquals(addressString, Address.getAddress(addressString).toString());
	}

}
