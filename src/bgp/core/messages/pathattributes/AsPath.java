package bgp.core.messages.pathattributes;

import java.util.LinkedList;
import java.util.List;

import bgp.core.messages.NotificationMessage.UpdateMessageError;
import bgp.core.messages.notificationexceptions.UpdateMessageException;

/**
 * In its current state, the system only support AS_SEQUENCEs shorter than 255 hops
 * @author Niko
 *
 */
public class AsPath extends PathAttribute {
	
	// First node is the nearest AS, last one is the originating one
	private final LinkedList<Integer> idSequence;
	
	private final byte segmentType;

	public AsPath(List<Integer> idSequence) {
		super(ONE, ONE, ZERO, ZERO);
		
		this.segmentType = 2;
		this.idSequence = new LinkedList<>();
		for (Integer id : idSequence) {
			this.idSequence.add(id);
		}
	}
	
	protected AsPath(byte[] input) throws UpdateMessageException {
		super(input[0]);
		int headerLength = 3 + extended;
		
		segmentType = input[headerLength];
		if (segmentType != 1 && segmentType != 2) {
			throw new UpdateMessageException(UpdateMessageError.MALFORMED_AS_PATH);
		}
		idSequence = new LinkedList<>();
		for (int i = 0; i < input[headerLength + 1]; i++) {
			try {
				idSequence.add((input[headerLength + 2 + 2*i] << 8)
						+ (input[headerLength + 2 + 2*i + 1]));
			} catch (Exception e) {
				throw new UpdateMessageException(UpdateMessageError.MALFORMED_AS_PATH);
			}
		}
	}
	
	/**
	 * Prepend the given AS ID to the beginning of the list (usually own ID)
	 * 
	 * @param idToAppend
	 */
	public void appendId(int idToAppend) {
		this.idSequence.addFirst(idToAppend);
	}
	
	public LinkedList<Integer> getIdSequence() {
		return idSequence;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AsPath) {
			AsPath other = (AsPath)obj;
			return other.segmentType == this.segmentType
					&& this.idSequence.equals(other.idSequence);
		}
		return false;
	}
	
	@Override
	public byte getTypeCode() {
		return (byte) 2;
	}

	@Override
	public byte[] getTypeBody() {
		byte pathSegmentType = (byte) segmentType;
		int sequenceLength = idSequence.size();
		
		byte[] body = new byte[2 + 2*sequenceLength];
		int index = 0;
		
		body[index++] = pathSegmentType;
		body[index++] = (byte) (sequenceLength&0xFF);
		for (Integer id : idSequence) {
			body[index++] = (byte) (id >> 8);
			body[index++] = (byte) (id >> 0);
		}
		
		return body;
	}

}
