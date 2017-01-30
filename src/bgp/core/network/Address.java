package bgp.core.network;

public class Address {
	protected final long address;
	
	protected Address(long address) {
		this.address = address;
	}
	
	@Override
	public boolean equals(Object o) {
		long other = 0L;
		if (o instanceof Address) {
			other = ((Address) o).address;
		} else if (o instanceof byte[]) {
			other = (long) o;
		} else {
			return false;
		}
		return this.address == other;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(16);
		
		for (int i = 0; i < 4; i++) {
			sb.append(address >> (8*i));
			sb.append(".");
		}
		// Delete the last separator
		sb.deleteCharAt(15);
		
		return sb.toString();
	}

	
	/**
	 * 32 0's followed by 32 1's, NOR with this should be 0 on valid IP's 
	 */
	protected static final long VALID_IP_BITMAP = (~0x0L) >>> 32;
	protected static final String VALID_IP_REGEX = "\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b";
	
	protected static long getLong(byte[] input) throws IllegalArgumentException {
		if (input.length != 4) {
			throw new IllegalArgumentException("Byte array should contain 4 octets");
		}
		return input[0] << 24 + input[1] << 16 + input[2] << 8 + input[3];
	}
	
	protected static long getLong(String input) throws IllegalArgumentException {
		if (!input.matches(VALID_IP_REGEX)) {
			throw new IllegalArgumentException("Address should be of form AAA.BBB.CCC.DDD, was " + input);
		}
		byte[] addressBytes = new byte[4];
		
		String[] partedAddress = input.split("\\.");
		
		for (int i = 0; i < 4; i++) {
			Integer octetValue = Integer.parseInt(partedAddress[i]);
			if (octetValue == null || octetValue < 0 || octetValue >= 256) {
				throw new IllegalArgumentException("All numbers should be in range 0..255, " + i + ". number was " + octetValue);
			}
			addressBytes[i] = octetValue.byteValue();
		}
		
		return getLong(addressBytes);
	}
	
	/**
	 * Parse an IPv4 address from a String of form AAA.BBB.CCC.DDD where AAA,...,DDD are integers in range 0..255
	 * 
	 * @param address
	 * @return
	 * @throws InvalidParameterException
	 */
	public static Address getAddress(String addressString) throws IllegalArgumentException {
		return getAddress(getLong(addressString));
	}
	
	public static Address getAddress(long addressLong) throws IllegalArgumentException {
		if ((addressLong & ~VALID_IP_BITMAP) != 0) {
			throw new IllegalArgumentException("The address needs to be stored in the lowest 32 bits");
		}
		return new Address(addressLong);
	}
	
	public static Address getAddress(byte[] addressBytes) throws IllegalArgumentException {
		return getAddress(getLong(addressBytes));
	}

}
