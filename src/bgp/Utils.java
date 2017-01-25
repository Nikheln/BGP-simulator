package bgp;

import java.util.Arrays;

public class Utils {
	
	/**
	 * Convert an IPv4 address in byte[]-form to a pretty String for printing purposes
	 *  
	 * @param address
	 * @return
	 * @throws InvalidParameterException
	 */
	public static String prettifyAddress(byte[] address) throws IllegalArgumentException {
		if (address.length != 4) {
			throw new IllegalArgumentException("Length of the address array should be 4, was " + address.length);
		}
		StringBuilder sb = new StringBuilder(14);
		
		for (int i = 0; i < 3; i++) {
			sb.append(new Byte(address[i]).intValue());
			sb.append(".");
		}
		sb.append(new Byte(address[3]).intValue());
		
		return sb.toString();
	}
	
	/**
	 * Parse an IPv4 address from a String of form AAA.BBB.CCC.DDD where AAA,...,DDD are integers in range 0..255
	 * 
	 * @param address
	 * @return
	 * @throws InvalidParameterException
	 */
	public static byte[] parseAddess(String address) throws IllegalArgumentException {
		if (!address.matches("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b")) {
			throw new IllegalArgumentException("Address should be of form AAA.BBB.CCC.DDD, was " + address);
		}
		
		byte[] result = new byte[4];
		
		String[] partedAddress = address.split(".");
		
		for (int i = 0; i < 4; i++) {
			Integer byteValue = Integer.parseInt(partedAddress[i]);
			if (byteValue == null || byteValue < 0 || byteValue >= 256) {
				throw new IllegalArgumentException("All numbers should be in range 0..255, " + i + ". number was " + byteValue);
			}
			result[i] = (byte)byteValue.intValue();
		}
		
		return result;
	}
	
	/**
	 * Check if a given value is in range min..max, UPPER BOUND EXCLUSIVE
	 * 
	 * @param min
	 * @param tested
	 * @param max
	 * @return
	 */
	public static boolean inRange(int min, int tested, int max) {
		return min <= tested && tested < max;
	}
	
	/**
	 * Check if given addresses are equal.
	 * 
	 * @param address1
	 * @param address2
	 * @return
	 */
	public static boolean addressesEqual(byte[] address1, byte[] address2) {
		return Arrays.equals(address1, address2);
	}
}
