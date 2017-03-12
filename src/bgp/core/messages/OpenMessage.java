package bgp.core.messages;

import bgp.core.messages.NotificationMessage.OpenMessageError;
import bgp.core.messages.notificationexceptions.OpenMessageException;

public class OpenMessage extends BGPMessage {
	
	private static final byte VERSION = 4;
	
	final int asId;
	final int holdTime;
	final long bgpId;
	
	/**
	 * Build a new OPEN message.
	 * 
	 * @param asId This 2-octet unsigned integer indicates the Autonomous System
         number of the sender.
	 * @param holdTime Upon receipt of an OPEN message, a BGP speaker MUST
	     calculate the value of the Hold Timer by using the smaller of its
	     configured Hold Time and the Hold Time received in the OPEN message.
	     The Hold Time MUST be either zero or at least three seconds.
	 * @param bgpId This 4-octet unsigned integer indicates the BGP Identifier of
         the sender.  A given BGP speaker sets the value of its BGP
         Identifier to an IP address that is assigned to that BGP
         speaker.  The value of the BGP Identifier is determined upon
         startup and is the same for every local interface and BGP peer.
	 */
	public OpenMessage(int asId, long holdTime, long bgpId) {
		this.asId = asId;
		this.holdTime = (int)(holdTime/1000);
		this.bgpId = bgpId;
	}
	
	protected OpenMessage(byte[] messageContent) throws OpenMessageException {
		if (messageContent[HEADER_LENGTH] != VERSION) {
			throw new OpenMessageException(OpenMessageError.UNSUPPORTED_VERSION_NUM);
		}
		if (messageContent[HEADER_LENGTH+9] != 0) {
			throw new OpenMessageException(OpenMessageError.UNSUPPORTED_OPTIONAL_PARAM);
		}
		asId = (int)(((messageContent[HEADER_LENGTH+1]&0xFF) << 8) + (messageContent[HEADER_LENGTH+2]&0x00FF))&0xFFFF;
		holdTime = (int)(((messageContent[HEADER_LENGTH+3]&0xFF) << 8) + (messageContent[HEADER_LENGTH+4]&0x00FF))&0xFFFF;
		long bgpIdTemp = 0;
		for (int i = 0; i <= 4; i++) {
			bgpIdTemp += (long)(messageContent[HEADER_LENGTH+5+i] << ((3-i)*8));
		}
		bgpId = bgpIdTemp;
	}

	@Override
	protected byte getType() {
		return (byte) 1;
	}

	@Override
	protected byte[] getBody() {
		byte[] body = new byte[10];
		body[0] = VERSION;
		body[1] = (byte) (asId >> 8);
		body[2] = (byte) (asId);
		body[3] = (byte) (holdTime >> 8);
		body[4] = (byte) (holdTime);
		for (int i = 0; i < 4; i++) {
			body[5+i] = (byte) (bgpId >> ((3-i)*8));
		}
		// No optional parameters
		body[9] = 0;
		return body;
	}

	public int getASId() {
		return asId;
	}

	public int getHoldTime() {
		return holdTime;
	}

	public long getBgpId() {
		return bgpId;
	}
	
	@Override
	public String toString() {
		return "AS id: " + asId
				+ "\nHold time: " + holdTime
				+ "\nBGP id: " + bgpId;
	}
	

}
