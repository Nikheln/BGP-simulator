package bgp.utils;

public class Consts {
	/**
	 * MTU is set at 1111 1111 = 255 to make sure the length fits in one byte.
	 */
	public static final int MTU = 1 << 8 - 1;
	
	public static final int DEFAULT_PREF = 100;
	
	public static final int MAX_HOPS = 255;
	
	public static final int DEFAULT_KEEPALIVE_INTERVAL = 30000;
	
	public static final int DEFAULT_HOLD_DOWN_TIME = DEFAULT_KEEPALIVE_INTERVAL * 3;
	
}
