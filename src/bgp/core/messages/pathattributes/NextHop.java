package bgp.core.messages.pathattributes;

import java.util.Arrays;

public class NextHop extends PathAttribute {
	
	private final byte[] nextHop;
	
	/**
	 * 
	 * @param nextHop IP address of the next hop (this device)
	 * @throws IllegalArgumentException
	 */
	public NextHop(byte[] nextHop) throws IllegalArgumentException {
		super(ONE, ONE, ZERO, ZERO);
		if (nextHop.length != 4) {
			throw new IllegalArgumentException("IP address should be 4 octets longs");
		}
		this.nextHop = Arrays.copyOf(nextHop, 4);
	}
	
	protected NextHop(byte[] input, boolean deserialization) throws IllegalArgumentException {
		super(input[0]);
		if (input.length != 3 + extended + 4) {
			throw new IllegalArgumentException("Input package is of incorrect size");
		}
		this.nextHop = Arrays.copyOfRange(input, input.length-4, input.length);
	}

	@Override
	public byte getTypeCode() {
		return (byte) 3;
	}

	@Override
	public byte[] getTypeBody() {
		return nextHop;
	}

}
