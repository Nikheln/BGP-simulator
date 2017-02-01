package bgp.core.messages;

public class KeepaliveMessage extends BGPMessage {

	protected KeepaliveMessage(byte[] messageContent) {
		
	}
	@Override
	protected byte getType() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected byte[] getBody() {
		// TODO Auto-generated method stub
		return null;
	}

}
