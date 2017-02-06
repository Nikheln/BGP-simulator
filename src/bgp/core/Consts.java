package bgp.core;

public class Consts {
	/**
	 * MTU is set at 1111 1111 = 255 to make sure the length fits in one byte.
	 */
	public static final int MTU = 1 << 8 - 1;
}
