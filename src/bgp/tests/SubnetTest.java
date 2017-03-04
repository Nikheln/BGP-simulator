package bgp.tests;

import static org.junit.Assert.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import bgp.utils.Subnet;

public class SubnetTest {

	@Test
	public void testHashCode() {
		List<Subnet> subnetList = new ArrayList<>();
		for (int i = 0; i < 1000; i++) {
			String address = (int)(Math.random()*256) + "."
					+ (int)(Math.random()*256) + "."
					+ (int)(Math.random()*256) + "."
					+ (int)(Math.random()*256) + "/"
					+ (int)(Math.random()*23+10);
			Subnet s = Subnet.getSubnet(address);
			for (Subnet t : subnetList) {
				if (!s.equals(t)) {
					assertNotEquals(s.hashCode(), t.hashCode());
				}
			}
			subnetList.add(s);
		}
	}


	@Test
	public void testContainsAddressLong() {
		for (int i = 0; i < 100; i++) {
			long addressValue = (long)(Math.random() * 256*256*256*256);
			int bitmaskLength = (int)(Math.random() * 33);
			long bitmask = Subnet.getSubnetMask(bitmaskLength);
			long maskedAddress = addressValue & bitmask;
			Subnet s = Subnet.getSubnet(maskedAddress, bitmask);
			
			assertTrue(s.containsAddress(addressValue));
		}
	}

	@Test
	public void testContainsSubnet() {
		for (int i = 0; i < 100; i++) {
			long addressValue1 = (long)(Math.random() * 256*256*256*256);
			int bitmaskLength1 = (int)(Math.random() * 33);
			Subnet s1 = Subnet.getSubnet(addressValue1, Subnet.getSubnetMask(bitmaskLength1));
			long addressValue2 = (long)(Math.random() * 256*256*256*256);
			int bitmaskLength2 = (int)(Math.random() * 33);
			Subnet s2 = Subnet.getSubnet(addressValue2, Subnet.getSubnetMask(bitmaskLength2));
			
			assertEquals(bitmaskLength1 <= bitmaskLength2 && (addressValue1&s1.getBitmask()) == (addressValue2&s1.getBitmask()), s1.containsSubnet(s2));
		}
	}

	
	@Test
	public void testGetSubnetMask() {
		for (int i = 0; i < 33; i++) {
			long mask = Subnet.getSubnetMask(i);
			String maskStr = "";
			while (maskStr.length() < i) {
				maskStr += "1";
			}
			while (maskStr.length() < 32) {
				maskStr += "0";
			}
			long trueMask = new BigInteger(maskStr, 2).longValue();
			assertEquals(Long.toBinaryString(trueMask), Long.toBinaryString(mask));
		}
	}

}
