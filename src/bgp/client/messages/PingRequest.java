package bgp.client.messages;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

public class PingRequest extends ClientMessage {
	
	private final byte[] body;
	
	public PingRequest() {
		body = new byte[8];
		try {
			SecureRandom.getInstanceStrong().nextBytes(body);
		} catch (NoSuchAlgorithmException e) {
		}
	}
	
	protected PingRequest(byte[] msg) {
		body = Arrays.copyOfRange(msg, HEADER_LENGTH, msg.length);
	}

	@Override
	public byte getType() {
		return 1;
	}

	@Override
	public byte[] getBody() {
		return body;
	}
	
	public byte[] getTokenBytes() {
		return getBody();
	}

}
