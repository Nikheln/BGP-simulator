package bgp.core.messages;

/**
 * A KEEPALIVE message consists of only the message header and has a
   length of 19 octets.
   
 * @author Niko
 *
 */
public class KeepaliveMessage extends BGPMessage {

	protected KeepaliveMessage(byte[] messageContent) {
	}
	
	public KeepaliveMessage() {
		
	}
	
	@Override
	protected byte getType() {
		return (byte) 4;
	}

	@Override
	protected byte[] getBody() {
		return new byte[0];
	}

}
