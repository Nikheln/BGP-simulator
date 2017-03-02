package bgp.client.messages;

import java.util.Arrays;

public class PingResponse extends ClientMessage {
	
	private final byte[] body;
	
	public PingResponse(byte[] token) {
		this.body = token;
	}
	
	protected PingResponse(byte[] msg, boolean internal) {
		this.body = Arrays.copyOfRange(msg, HEADER_LENGTH, msg.length);
	}

	@Override
	public byte getType() {
		return 2;
	}

	@Override
	public byte[] getBody() {
		return body;
	}
	
	public byte[] getTokenBytes() {
		return getBody();
	}

}
