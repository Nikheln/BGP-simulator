package bgp.utils;

public class Consts {
	/**
	 * MTU is set at 1111 1111 1111 1111 = 65535 to make sure the length fits in one byte.
	 */
	public static final int MTU = Short.MAX_VALUE;
	
	public static final int DEFAULT_PREF = 100;
	
	public static final int MAX_HOPS = 255;
	
	public static final int DEFAULT_KEEPALIVE_INTERVAL = 30000;
	
	public static final int DEFAULT_HOLD_DOWN_TIME = DEFAULT_KEEPALIVE_INTERVAL * 3;
	
}
