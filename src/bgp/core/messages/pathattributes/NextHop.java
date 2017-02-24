package bgp.core.messages.pathattributes;

import java.util.Arrays;

import bgp.core.messages.NotificationMessage.UpdateMessageError;
import bgp.core.messages.notificationexceptions.UpdateMessageException;

public class NextHop extends PathAttribute {
	
	private final byte[] nextHop;
	
	/**
	 * 
	 * @param nextHop IP address of the next hop (this device)
	 * @throws IllegalArgumentException
	 */
	public NextHop(byte[] nextHop) throws UpdateMessageException {
		super(ONE, ONE, ZERO, ZERO);
		if (nextHop.length != 4) {
			throw new UpdateMessageException(UpdateMessageError.INVALID_NEXT_HOP_ATTRIBUTE);
		}
		this.nextHop = Arrays.copyOf(nextHop, 4);
	}
	
	protected NextHop(byte[] input, boolean deserialization) throws UpdateMessageException {
		super(input[0]);
		if (input.length != 3 + extended + 4) {
			throw new UpdateMessageException(UpdateMessageError.INVALID_NEXT_HOP_ATTRIBUTE);
		}
		this.nextHop = Arrays.copyOfRange(input, input.length-4, input.length);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof NextHop) {
			return Arrays.equals(((NextHop)obj).nextHop, nextHop);
		}
		return false;
	}

	@Override
	public byte getTypeCode() {
		return (byte) 3;
	}

	@Override
	public byte[] getTypeBody() {
		return nextHop;
	}

}
