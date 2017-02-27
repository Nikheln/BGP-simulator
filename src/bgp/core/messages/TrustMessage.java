package bgp.core.messages;

import java.util.Arrays;

import bgp.core.trust.TrustEngine;

/**
 * Class for transferring trust information between nodes.
 * Used for both requests and responses.
 * 
 * Flags (1 octet)
 * --------------
 * 1. bit: TYPE: 0 for request, 1 for response
 * --------------
 * 
 * Reviewing second-order neighbour ID (2 octets)
 * Reviewed neighbour ID (2 octets)
 * 
 * if TYPE == 1
 * 	encrypted trust of given neighbour
 * 		represented as a value in range -128..127, default trust is 0
 * 
 * @author Niko
 *
 */
public class TrustMessage extends BGPMessage {
	
	public static final int REQUEST = 0;
	public static final int RESPONSE = 1;
	
	private final int tmType;
	private final int reviewerId;
	private final int targetId;
	private final byte[] payload;
	private final byte[] signature;
	
	public TrustMessage(int reviewerId, int targetId) {
		this(0, reviewerId, targetId, new byte[0], new byte[0]);
	}
	
	public TrustMessage(int reviewerId, int targetId, byte[] encryptedReview, byte[] signature) {
		this(1, reviewerId, targetId, encryptedReview, signature);
	}
	
	private TrustMessage(int tmType, int reviewerId, int targetId, byte[] payload, byte[] signature) {
		this.tmType = tmType;
		this.reviewerId = reviewerId;
		this.targetId = targetId;
		// Add padding if payload is too short
		this.payload = Arrays.copyOf(payload, payload.length);
		this.signature = Arrays.copyOf(signature, signature.length);
	}
	
	protected TrustMessage(byte[] messageContent) {
		int index = HEADER_LENGTH;
		this.tmType = (messageContent[index++] >> 7)&0x01;
		this.reviewerId = (((messageContent[index++]&0xFF) << 8) + (messageContent[index++]&0xFF))&0xFFFF;
		this.targetId = (((messageContent[index++]&0xFF) << 8) + (messageContent[index++]&0xFF))&0xFFFF;
		if (this.tmType == 1) {
			this.payload = Arrays.copyOfRange(messageContent, index, index + TrustEngine.ENCRYPTED_MESSAGE_LENGTH);
			this.signature = Arrays.copyOfRange(messageContent, index + TrustEngine.ENCRYPTED_MESSAGE_LENGTH, messageContent.length);	
		} else {
			this.payload = new byte[0];
			this.signature = new byte[0];
		}
	}

	@Override
	protected byte getType() {
		return (byte) 5;
	}

	@Override
	protected byte[] getBody() {
		byte[] body = new byte[5 + payload.length + signature.length];
		
		body[0] = (byte) ((tmType << 7)&0xFF);
		body[1] = (byte) ((reviewerId >>> 8)&0xFF);
		body[2] = (byte) ((reviewerId >>> 0)&0xFF);
		body[3] = (byte) ((targetId >>> 8)&0xFF);
		body[4] = (byte) ((targetId >>> 0)&0xFF);
		System.arraycopy(payload, 0, body, 5, payload.length);
		System.arraycopy(signature, 0, body, 5 + payload.length, signature.length);
		
		return body;
	}
	
	public int getReviewerId() {
		return reviewerId;
	}
	
	public int getTargetId() {
		return targetId;
	}

	public byte[] getPayload() {
		return payload;
	}
	
	public byte[] getSignature() {
		return signature;
	}
	
	public boolean isRequest() {
		return tmType == 0;
	}
}
