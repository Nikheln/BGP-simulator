package bgp.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import bgp.Utils;

public class GlobalState {
	
	private static final List<byte[]> usedAddresses = new ArrayList<>();
	
	public static void reserveAddress(byte[] address) throws IllegalStateException {
		if (!isAddressFree(address)) {
			throw new IllegalStateException("Address " + Utils.prettifyAddress(address) + " is already in use.");
		}
		
		usedAddresses.add(address);
	}
	
	public static void releaseAddress(byte[] address) throws IllegalArgumentException {
		int index = -1;
		for (int i = 0; i < usedAddresses.size(); i++) {
			if (Utils.addressesEqual(usedAddresses.get(i), address)) {
				index = i;
			}
		}
		
		if (index >= 0) {
			usedAddresses.remove(index);
		} else {
			throw new IllegalArgumentException("Address is not reserved");
			
		}
		
	}
	
	public static boolean isAddressFree(byte[] address) {
		for (byte[] usedAddress : usedAddresses) {
			if (Utils.addressesEqual(usedAddress, address)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Return a nearby address
	 * 
	 * @param otherAddress
	 * @return
	 * @throws IllegalArgumentException
	 */
	public static byte[] getFreeNearbyAddress(byte[] otherAddress) throws IllegalArgumentException {
		if (otherAddress == null) {
			throw new IllegalArgumentException("Other address should be non-null");
		} else if (otherAddress.length != 4) {
			throw new IllegalArgumentException("Other address should be of length 4, was " + otherAddress.length);
		}
		
		byte[] candidate = Arrays.copyOf(otherAddress, 4);
		
		while (!isAddressFree(candidate)) {
			int iterations = 0;
			int maxIterations = 255;
			while (!isAddressFree(candidate) && iterations <= maxIterations) {
				candidate[3] += 1;
			}
			if (!isAddressFree(candidate)) {
				candidate[2] += 1;
			}
		}
		
		return candidate;
	}

}
