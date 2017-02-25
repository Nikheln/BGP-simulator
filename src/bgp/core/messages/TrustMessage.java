package bgp.core.messages;

/**
 * Class for transferring trust information between nodes.
 * Used for both requests and responses.
 * 
 * Flags (1 octet)
 * --------------
 * 1. bit: TYPE: 0 for request, 1 for response
 * --------------
 * 
 * Reviewed neighbour ID (2 octets)
 * 
 * Trust of given neighbour (1 octet)
 * - Only if TYPE == 1
 * - Represented as a value in range -128..127, default trust is 0
 * 
 * @author Niko
 *
 */
public class TrustMessage extends BGPMessage {
	
	private final int tmType;
	private final int targetId;
	private final byte trust;
	
	public TrustMessage(int targetId) {
		this.tmType = 0;
		this.targetId = targetId;
		this.trust = 0;
	}
	
	public TrustMessage(int targetId, byte trust) {
		this.tmType = 1;
		this.targetId = targetId;
		this.trust = trust;
	}
	
	protected TrustMessage(byte[] messageContent) {
		int index = HEADER_LENGTH;
		this.tmType = messageContent[index++]&0x80;
		this.targetId = (((messageContent[index++]&0xFF) << 8) + (messageContent[index++]&0xFF))&0xFFFF;
		this.trust = (tmType == 1 ? messageContent[index++] : 0);
	}

	@Override
	protected byte getType() {
		return (byte) 5;
	}

	@Override
	protected byte[] getBody() {
		byte[] body = new byte[tmType == 0 ? 3 : 4];
		
		body[0] = (byte) ((tmType << 7)&0xFF);
		body[1] = (byte) ((targetId >>> 8)&0xFF);
		body[2] = (byte) ((targetId >>> 0)&0xFF);
		if (tmType == 1) {
			body[3] = trust;
		}
		
		return body;
	}

	public int getTargetId() {
		return targetId;
	}

	public byte getTrust() {
		return trust;
	}
	
	public boolean isRequest() {
		return tmType == 0;
	}
}
