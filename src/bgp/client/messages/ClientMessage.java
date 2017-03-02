package bgp.client.messages;

public abstract class ClientMessage {
	
	protected static final int HEADER_LENGTH = 3;

	public byte[] serialize() {
		byte type = getType();
		byte[] body = getBody();
		int bodyLen = body.length;
		
		byte[] msg = new byte[HEADER_LENGTH + body.length];
		
		msg[0] = (byte) ((bodyLen >>> 8)&0xFF);
		msg[1] = (byte) (bodyLen&0xFF);
		msg[2] = type;
		
		System.arraycopy(body, 0, msg, 3, bodyLen);
		
		return msg;
	}
	
	public abstract byte getType();
	
	public abstract byte[] getBody();
	
	public static ClientMessage deserialize(byte[] msg) {
		switch (msg[2]) {
		case 1:
			return new PingRequest(msg);
		case 2:
			return new PingResponse(msg);
		default:
			return null;
		}
	}
}
