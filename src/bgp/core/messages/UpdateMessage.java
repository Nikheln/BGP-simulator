package bgp.core.messages;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import bgp.core.network.Subnet;

/**
 *    
  +-----------------------------------------------------+
  |   Withdrawn Routes Length (2 octets)                |
  +-----------------------------------------------------+
  |   Withdrawn Routes (variable)                       |
  +-----------------------------------------------------+
  |   Total Path Attribute Length (2 octets)            |
  +-----------------------------------------------------+
  |   Path Attributes (variable)                        |
  +-----------------------------------------------------+
  |   Network Layer Reachability Information (variable) |
  +-----------------------------------------------------+
 * @author Niko
 *
 */
public class UpdateMessage extends BGPMessage {
	
	private final List<Subnet> withdrawnRoutes;
	
	public UpdateMessage(List<Subnet> withdrawnRoutes) {
		this.withdrawnRoutes = new ArrayList<>();
		this.withdrawnRoutes.addAll(withdrawnRoutes);
	}
	
	protected UpdateMessage(byte[] messageContent) {
		this.withdrawnRoutes = new ArrayList<>();
	}

	@Override
	protected byte getType() {
		return 2;
	}

	@Override
	protected byte[] getBody() {
		// Linked list used since the list is only appended and looped
		List<Byte> withdrawnRoutesBytes = new LinkedList<>();
		for (Subnet s : withdrawnRoutes) {
			int bml = s.getBitmaskLength();
			int octetCount = (int)(Math.ceil(bml/8.0));
			long ip = s.getAddress();
			
			withdrawnRoutesBytes.add((byte) bml);
			
			for (int i = 0; i < octetCount; i++) {
				withdrawnRoutesBytes.add((byte) (ip >> ((3-i)*8)));
			}
		}
		
		int withdrawnRoutesSize = withdrawnRoutesBytes.size();
		int bodyLength = 2				// Withdrawn octet length
				+ withdrawnRoutesSize	// Withdrawn routes
				+ 2						// Path attributes octet length
				+ 0						// Path attributes
				+ 0;					// NLRI
		byte[] body = new byte[bodyLength];
		
		int index = 0;
		
		body[index++] = (byte) (withdrawnRoutesSize >> 8);
		body[index++] = (byte) withdrawnRoutesSize;
		
		for (Byte b : withdrawnRoutesBytes) {
			body[index++] = b;
		}
		
		// TODO Auto-generated method stub
		return body;
	}

}
