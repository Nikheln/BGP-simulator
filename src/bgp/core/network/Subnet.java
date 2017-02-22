package bgp.core.network;

public class Subnet extends Address {
	
	private final long bitmask;
	private final int bitmaskLength;
	
	protected Subnet(long address, long bitmask) {
		super(address & bitmask & 0xFFFFFFFF);
		this.bitmask = bitmask & 0xFFFFFFFF;
		this.bitmaskLength = getBitmaskLength();
	}
	
	public boolean containsAddress(Address other) {
		return containsAddress(other.address);
	}
	
	public boolean containsAddress(long other) {
		return (other & bitmask) == this.address;
	}
	
	/**
	 * Checks if other is contained in this subnet
	 * (prefix in subnet and longer subnet mask OR equal subnets)
	 * @param other
	 * @return
	 */
	public boolean containsSubnet(Subnet other) {
		return (other.bitmask > this.bitmask && containsAddress(other.address))
				|| this.equals(other);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString());
		sb.append('/');
		sb.append(bitmaskLength);
		
		return sb.toString(); 
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof Subnet) {
			Subnet o = (Subnet) other;
			return o.address == this.address && o.bitmask == this.bitmask;
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return (int) (address*100 + bitmaskLength);
	}
	
	public int getBitmaskLength() {
		return Long.toBinaryString(this.bitmask).replace("0", "").length();
	}
	
	public long getBitmask() {
		return bitmask;
	}
	
	
	private static final String VALID_CIDR_REGEX = "\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\/[0-9]{1,2}\\b";
	
	
	public static Subnet getSubnet(long addressLong, long subnetMaskLong) throws IllegalArgumentException {
		return new Subnet(addressLong&VALID_IP_BITMASK, subnetMaskLong&VALID_IP_BITMASK);
	}
	
	public static Subnet getSubnet(byte[] addressBytes, byte[] subnetBytes) throws IllegalArgumentException {
		return getSubnet(getLong(addressBytes), getLong(subnetBytes));
	}
	
	public static Subnet getSubnet(String cidr) throws IllegalArgumentException {
		if (!cidr.matches(VALID_CIDR_REGEX)) {
			throw new IllegalArgumentException("Address must be in CIDR notation form: AAA.BBB.CCC.DDD/XX");
		}
		String[] parts = cidr.split("\\/");
		
		return getSubnet(getLong(parts[0]), getSubnetMask(Integer.parseInt(parts[1])));
	}
	
	public static long getSubnetMask(int length) {
		long mask = 0L;
		for (int i = 0; i < 32; i++) {
			mask = (i < length ? mask+1 : mask) << 1;
		}
		
		return mask >>> 1;
	} 
}

