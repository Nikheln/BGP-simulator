package bgp.core.messages.pathattributes;

import java.util.LinkedList;
import java.util.List;

/**
 * In its current state, the system only support AS_SEQUENCEs shorter than 255 hops
 * @author Niko
 *
 */
public class AsPath extends PathAttribute {
	
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
	
	protected AsPath(byte[] input) throws IllegalArgumentException {
		super(input[0]);
		int headerLength = 3 + extended;
		
		segmentType = input[headerLength];
		if (segmentType != 1 && segmentType != 2) {
			throw new IllegalArgumentException("Segment type must be 1 (AS_SET) or 2 (AS_SEQUENCE)");
		}
		idSequence = new LinkedList<>();
		for (int i = 0; i < input[headerLength + 1]; i++) {
			try {
				idSequence.add((input[headerLength + 2 + 2*i] << 8)
						+ (input[headerLength + 2 + 2*i + 1]));
			} catch (Exception e) {
				throw new IllegalArgumentException("Parsing failed");
			}
		}
	}
	
	/**
	 * Prepend the given AS ID to the beginning of the list (usually own ID)
	 * 
	 * @param idToAppend
	 * @throws IllegalStateException If the value given is already in the list
	 */
	public void appendId(int idToAppend) throws IllegalStateException {
		if (idSequence.contains(idToAppend)) {
			throw new IllegalStateException("AS_PATH contains a loop");
		}
		this.idSequence.addFirst(idToAppend);
	}
	
	@Override
	public byte getTypeCode() {
		return (byte) 2;
	}

	@Override
	public byte[] getTypeBody() {
		byte pathSegmentType = (byte) segmentType;
		byte sequenceLength = (byte) idSequence.size();
		
		byte[] body = new byte[2 + 2*sequenceLength];
		int index = 0;
		
		body[index++] = pathSegmentType;
		body[index++] = sequenceLength;
		for (Integer id : idSequence) {
			body[index++] = (byte) (id >> 8);
			body[index++] = (byte) (id >> 0);
		}
		
		return body;
	}

}
