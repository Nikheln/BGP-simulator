package bgp.core.messages;

import bgp.core.messages.NotificationMessage.MessageHeaderError;
import bgp.core.messages.notificationexceptions.MessageHeaderException;
import bgp.core.messages.notificationexceptions.NotificationException;

public abstract class BGPMessage {
	
	protected abstract byte getType();
	
	protected abstract byte[] getBody();
	
	private static final int MARKER_LENGTH = 16;
	
	protected static final int HEADER_LENGTH = MARKER_LENGTH + 3;
	
	public byte[] serialize() {
		final byte type = getType();
		final byte[] body = getBody();
		
		int messageLength = HEADER_LENGTH	// Fixed length header
				+ body.length;				// Message body
		
		final byte[] message = new byte[messageLength];
		
		// RFC4271:  This 16-octet field is included for compatibility; it MUST be set to all ones.
		for (int i = 0; i < MARKER_LENGTH; i++) {
			message[i] = ~0x0;
		}
		message[16] = (byte)(messageLength >> 8);
		message[17] = (byte) messageLength;
		message[18] = type;
		
		for (int i = 0; i < body.length; i++) {
			message[19+i] = body[i];
		}
		
		return message;
	}
	
	/**
	 * Build a message from a received byte stream.
	 * @param message Body of a possible BGP message without IP header
	 * @return A BGP message object
	 */
	public static BGPMessage deserialize(byte[] message) throws NotificationException {
		switch (message[18]) {
		case 1:
			// Open
			return new OpenMessage(message);
		case 2:
			// Update
			return new UpdateMessage(message);
		case 3:
			// Notification
			return new NotificationMessage(message);
		case 4:
			// Keepalive
			return new KeepaliveMessage(message);
		case 5:
			// Trust
			return new TrustMessage(message);
		default:
			throw new MessageHeaderException(MessageHeaderError.BAD_MESSAGE_TYPE);
		}
	}

}
