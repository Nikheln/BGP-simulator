package bgp.core.network;

import bgp.utils.Address;

/**
 * Builds IPv4 packets.
 * @author Niko
 *
 */
public class PacketEngine {
	
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
		return buildPacket(from.address, to.address, payload);
	}
	
	public static byte[] buildPacket(long from, long to, byte[] payload) {
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
		
		result[12] = (byte) (from >> 24);
		result[13] = (byte) (from >> 16);
		result[14] = (byte) (from >> 8);
		result[15] = (byte) (from);
		
		result[16] = (byte) (to >> 24);
		result[17] = (byte) (to >> 16);
		result[18] = (byte) (to >> 8);
		result[19] = (byte) (to);
		
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
		packet[8] = (byte) ((packet[8] - 1)&0xFF);
		
		// Reset the values to avoid affecting the checcksum
		packet[10] = 0;
		packet[11] = 0;
		long checksum = calculateChecksum(packet);
		packet[10] = (byte) ((checksum >>> 8)&0xFF);
		packet[11] = (byte) (checksum&0xFF);
	}
	
	public static long calculateChecksum(byte[] packet) {
		// AND operations with 0xFF's are in place to avoid type casting to int
		long calculatedChecksum = 0;
		for (int i = 0; i < HEADER_LENGTH; i += 2) {
			calculatedChecksum = (calculatedChecksum + (((packet[i] << 8)&0xFF00) + (packet[i+1]&0x00FF)))
					&0xFFFFFFFF;
		}
		while ((calculatedChecksum&0xFFFFFFFF) > 0xFFFF) {
			calculatedChecksum = (((calculatedChecksum&0xFFFF0000) >>> 16)
					+ (calculatedChecksum&0x0000FFFF))&0xFFFFFFFF;
		}
		return (~calculatedChecksum) & 0xFFFF;
	}
	
	/**
	 * Validates the packet header by checking packet length, header checksum, IP version and header length
	 * @param packet
	 * @return true if packet header is correct, false otherwise
	 */
	public static boolean validatePacketHeader(byte[] packet) {
		if (packet.length < 20) {
			// Received packet is not long enough to hold header
			System.out.println("LENGTH");
			return false;
		}
		if (!verifyChecksum(packet)) {
			// Invalid checksum
			return false;
		}
		
		if (((packet[0]&0xF0)>>>4) != VERSION) {
			// Invalid IP version
			System.out.println("VERSION");
			return false;
		}
		
		if ((packet[0]&0x0F) < 5) {
			// IHL is less than 5
			System.out.println("IHL");
			return false;
		}
		
		if (((packet[0]&0x0F) << 2) > (((packet[2]&0xFF) << 8)&0xFF00) + (packet[3]&0xFF)) {
			// Datagram is too short to contain header
			System.out.println("PACKET LENGTH");
			return false;
		}
		
		return true;
	}
	
	public static boolean verifyChecksum(byte[] packet) {
		return calculateChecksum(packet) == 0;
		
	}
	
	public static byte[] extractBody(byte[] packet) {
		int headerOctets = (packet[0]&0x0F) << 2;
		int totalOctets = (((packet[2]&0xFF) << 8)&0xFF00) + (packet[3]&0xFF);
		int bodyOctets = totalOctets - headerOctets;
		
		byte[] result = new byte[bodyOctets];
		System.arraycopy(packet, headerOctets, result, 0, bodyOctets);
		
		return result;
	}
	
	public static long extractSender(byte[] packet) {
		return (((packet[12] << 24)&0xFF000000)
				+ ((packet[13] << 16)&0x00FF0000)
				+ ((packet[14] << 8)&0x0000FF00)
				+ ((packet[15])&0x000000FF));
	}
	
	public static long extractRecipient(byte[] packet) {
		return (((packet[16] << 24)&0xFF000000)
				+ ((packet[17] << 16)&0x00FF0000)
				+ ((packet[18] << 8)&0x0000FF00)
				+ ((packet[19])&0x000000FF));
	}
	
	public static int extractTTL(byte[] packet) {
		return packet[8]&0xFF;
	}
	
	public static void printPacket(byte[] packet) {
		for (int i = 0; i < packet.length; i++) {
			if ((i % 4) == 0) {
				System.out.println();
			}
			System.out.print(String.format("%9s", Long.toBinaryString(packet[i]&0xFF) + "x").replace(' ', '0').replace('x', ' '));
		}
		System.out.println();
	}
}
