package bgp.core.messages.pathattributes;

import bgp.core.messages.NotificationMessage.UpdateMessageError;
import bgp.core.messages.notificationexceptions.UpdateMessageException;

public abstract class PathAttribute {
	
	public abstract byte getTypeCode();
	
	public abstract byte[] getTypeBody();
	
	protected static final byte ONE = (byte) 1;
	protected static final byte ZERO = (byte) 0;
	
	/* Whether the attribute is optional (if set to 1) or well-known (if set to 0).*/
	private final byte optional;
	
	/* Whether the attribute is transitive (if set to 1) or not (if set to 0).*/
	private final byte transitive;
	
	/*
	 * Whether the information contained in the optional transitive attribute
	 * is partial (if set to 1) or complete (if set to 0).  For well-known
	 * attributes and for optional non-transitive attributes, the Partial bit
     * MUST be set to 0.
	 */
	private final byte partial;
	
	/* Whether the Attribute Length is one octet (if set to 0) or two (1).
	 * If the Extended Length bit of the Attribute Flags octet is set
     * to 1, the third and fourth octets of the path attribute contain
     * the length of the attribute data in octets.
	 */
	protected final byte extended;
	
	/**
	 * 
	 * @param optional
	 * @param transitive For well-known (non-optional) attributes,
	 * 		the Transitive bit MUST be set to 1.
	 * @param partial For well-known attributes	and for optional
	 * 		non-transitive attributes, the Partial bit MUST be set to 0.
	 * @param extended
	 * @throws IllegalArgumentException
	 */
	protected PathAttribute(byte optional, byte transitive,
			byte partial, byte extended) throws IllegalArgumentException {
		if ((optional != 0 & optional != 1)
				|| (transitive != 0 && transitive != 1)
				|| (partial != 0 && partial != 1)
				|| (extended != 0 && extended != 1)) {
			throw new IllegalArgumentException("Only values 0 and 1 are permitted for flag bits");
		}
		this.optional = optional;
		this.transitive = transitive;
		this.partial = partial;
		this.extended = extended;
	}
	
	protected PathAttribute(byte flagByte) {
		this.optional = (byte) ((flagByte >> 7) & 1);
		this.transitive = (byte) ((flagByte >> 6) & 1);
		this.partial = (byte) ((flagByte >> 5) & 1);
		this.extended = (byte) ((flagByte >> 4) & 1);
	}
	
	private byte[] getBody() {
		byte typeCode = getTypeCode();
		byte[] typeBody = getTypeBody();
		int prefixLength = 3 + extended;
		
		int bodyLength = 2				// Flags and type code
				+ 1 + extended			// Length field(s)
				+ typeBody.length;		// Body
		
		byte[] body = new byte[bodyLength];
		
		body[0] = (byte) (optional << 7
				+ transitive << 6
				+ partial << 5
				+ extended << 4);
		
		body[1] = typeCode;
		
		if (extended == 1) {
			body[2] = (byte) (bodyLength >> 8);
			body[3] = (byte) bodyLength;
		} else {
			body[2] = (byte) bodyLength;
		}
		
		for (int i = 0; i < typeBody.length; i++) {
			body[prefixLength + i] = typeBody[i];
		}
		
		return body;
	}
	
	public static byte[] serialize(PathAttribute input) {
		return input.getBody();
	}
	
	public static PathAttribute deserialize(byte[] input) throws UpdateMessageException {
		if (input == null || input.length < 3) {
			throw new UpdateMessageException(UpdateMessageError.ATTRIBUTE_LENGTH_ERROR);
		}
		
		switch(input[1]) {
		case 1:
			// ORIGIN
			return new Origin(input);
		case 2:
			// AS_PATH
			return new AsPath(input);
		case 3:
			// NEXT_HOP
			return new NextHop(input, true);
		case 5:
			// LOCAL_PREF
			// Not implemented, only for IBGP communications
		case 6:
			// ATOMIC_AGGREGATE
			// Not implemented yet
		}
		throw new UpdateMessageException(UpdateMessageError.OPTIONAL_ATTRIBUTE_ERROR);
	}
	
}
