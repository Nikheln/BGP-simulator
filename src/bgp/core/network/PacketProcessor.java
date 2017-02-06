package bgp.core.network;

/**
 * Builds IPv4 packets.
 * @author Niko
 *
 */
public class PacketProcessor {
	
	private static final int HEADER_LENGTH = 20;
	/**
	 * Only IPv4 is supported in this simulation
	 */
	private static final byte VERSION = 4;
	/**
	 * No IPv4 packet options are supported => IHL = 5 always
	 */
	private static final byte IHL = 5;
	/**
	 * Only the default type of service is supported
	 */
	private static final byte DSCP = 0;
	/**
	 * Explicit Congestion Notification is not implemented.
	 */
	private static final byte ECN = 0;
	/**
	 * Start all packets with TTL 255;
	 */
	private static final byte DEFAULT_TTL = (byte) 255;
	/**
	 * Since there is no difference in implementation, mark all packets as UDP.
	 */
	private static final byte DEFAULT_PROTOCOL = (byte) 17;
	
	public static byte[] buildPacket(Address from, Address to, byte[] payload) {
		int totalLen = HEADER_LENGTH + payload.length;
		
		byte[] result = new byte[totalLen];
		
		result[0] = (VERSION << 4) + IHL;
		result[1] = (DSCP << 2) + ECN;
		result[2] = (byte) (totalLen >> 8);
		result[3] = (byte) (totalLen);
		
		// No fragmentation necessary, ID can be 0
		result[4] = 0;
		result[5] = 0;
		// 0: Reserved, must be zero, 1: Don't fragment, 0: More fragments
		result[6] = (0b010 << 5) + 0;
		result[7] = 0;
		
		result[8] = DEFAULT_TTL;
		result[9] = DEFAULT_PROTOCOL;
		// Checksum is calculated after all values are in place
		
		long fromLong = from.getAddress();
		result[12] = (byte) (fromLong >> 24);
		result[13] = (byte) (fromLong >> 16);
		result[14] = (byte) (fromLong >> 8);
		result[15] = (byte) (fromLong);
		
		long toLong = to.getAddress();
		result[16] = (byte) (toLong >> 24);
		result[17] = (byte) (toLong >> 16);
		result[18] = (byte) (toLong >> 8);
		result[19] = (byte) (toLong);
		
		long checksum = calculateChecksum(result);
		result[10] = (byte) (checksum >> 8);
		result[11] = (byte) (checksum);
		
		System.arraycopy(payload, 0, result, HEADER_LENGTH, payload.length);
		
		return result;
	}
	
	/**
	 * Decrements the TTL counter in the packet by one and recalculates the checksum.
	 * </br><b>PASSED PARAMETER ARRAY IS MUTATED!</b>
	 * 
	 * @param packet
	 * @throws IllegalArgumentException Exception is thrown if TTL is 0
	 */
	public static void decrementTTL(byte[] packet) throws IllegalArgumentException {
		if (packet[8] == 0) {
			throw new IllegalArgumentException("TTL is already zero");
		}
		
		packet[8]--;

		long checksum = calculateChecksum(packet);
		packet[10] = (byte) (checksum >> 8);
		packet[11] = (byte) (checksum);
	}
	
	private static long calculateChecksum(byte[] packet) {
		// AND operations with 0xFF's are in place to avoid type casting to int
		long sum = 0;
		for (int i = 0; i < HEADER_LENGTH; i++) {
			if (i == 10 || i == 11) {
				// Do nothing, checksum fields
			} else if (i % 2 == 0) {
				// Even packets are shifted left to simulate 16-bit calculations
				sum += (packet[i] & 0xFF) << 8;
			} else {
				sum += (packet[i] & 0xFF);
			}
		}
		return ~((sum & 0xFFFF) + (sum >> 16)) & 0xFFFF;
	}
	
	public static boolean verifyChecksum(byte[] packet) {
		long originalChecksum = (packet[10] >> 8) + packet[11];
		long calculatedChecksum = calculateChecksum(packet);
		
		return originalChecksum == calculatedChecksum;
		
	}
	
	public static byte[] extractBody(byte[] packet) {
		int headerOctets = (packet[0] & 0x0F) << 2;
		int totalOctets = (packet[2] << 8) + (packet[3]);
		int bodyOctets = totalOctets - headerOctets;
		
		byte[] result = new byte[bodyOctets];
		System.arraycopy(packet, headerOctets, result, 0, bodyOctets);
		
		return result;
	}
}
