package bgp.core.messages.pathattributes;

public class Origin extends PathAttribute {
	
	private final int originValue;
	
	/**
	 * 
	 * @param originValue
	 * The Origin attribute can contain one of these three values
	 * 0. IGP
	 * 1. EGP
	 * 2. Incomplete
	 */
	public Origin(int originValue) throws IllegalArgumentException {
		super(ONE, ONE, ZERO, ZERO);
		if (originValue < 0 || originValue > 2) {
			throw new IllegalArgumentException("Origin value must be in range 0..2");
		}
		this.originValue = originValue;
	}
	
	protected Origin(byte[] input) throws IllegalArgumentException {
		super(input[0]);
		this.originValue = input[input.length-1];
		if (this.originValue < 0 || this.originValue > 2) {
			throw new IllegalArgumentException("Origin value must be in range 0..2");
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		return (obj instanceof Origin)
				&& ((Origin)obj).originValue == originValue;
	}

	@Override
	public byte getTypeCode() {
		return (byte) 1;
	}

	@Override
	public byte[] getTypeBody() {
		return new byte[]{(byte) originValue};
	}

}
