package bgp.core.messages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import bgp.core.messages.pathattributes.PathAttribute;
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
	
	private final List<PathAttribute> pathAttributes;
	
	private final List<Subnet> NLRI;
	
	public UpdateMessage(List<Subnet> withdrawnRoutes, List<PathAttribute> pathAttributes, List<Subnet> NLRI) {
		this.withdrawnRoutes = new ArrayList<>();
		this.withdrawnRoutes.addAll(withdrawnRoutes);
		
		this.pathAttributes = new ArrayList<>();
		this.pathAttributes.addAll(pathAttributes);
		this.pathAttributes.sort((p1,p2)-> p2.getTypeCode()-p1.getTypeCode());
		
		this.NLRI = new ArrayList<>();
		this.NLRI.addAll(NLRI);
	}
	
	protected UpdateMessage(byte[] messageContent) {
		int index = 0;
		
		this.withdrawnRoutes = new ArrayList<>();
		int withdrawnRoutesOctets = messageContent[index++] << 8 + messageContent[index++];
		for (int i = 0; i < withdrawnRoutesOctets; i++) {
			int bml = messageContent[index++];
			int octetCount = (int)(Math.ceil(bml/8.0));
			
			this.withdrawnRoutes.add(bytesToSubnet(Arrays.copyOfRange(messageContent, 2 + i + 1, 2 + i + 1 + octetCount)));
			index += octetCount;
		}
		
		this.pathAttributes = new ArrayList<>();
		int pathAttributeOctets = messageContent[index++] << 8 + messageContent[index++];
		for (int i = 0; i < pathAttributeOctets; i++) {
			int startIndex = index;
			int pal = 2;
			if ((messageContent[index++] & 0b00010000) == 0) {
				// Extended
				index++; // Skip type code
				pal += (messageContent[index++] << 8) + (messageContent[index++]);
			} else {
				// Not extended
				index++; // Skip type code
				pal += (messageContent[index++]);
			}
			this.pathAttributes.add(PathAttribute.deserialize(Arrays.copyOfRange(messageContent, startIndex, startIndex + pal)));
			index = startIndex + pal;
		}
		
		this.NLRI = new ArrayList<>();
		
		while (index < messageContent.length) {
			int bml = messageContent[index];
			int octetCount = (int)(Math.ceil(bml/8.0));
			
			this.NLRI.add(bytesToSubnet(Arrays.copyOfRange(messageContent, index, index + 1 + octetCount)));
			index = index + octetCount + 1;
		}
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
			byte[] subnetBytes = subnetToBytes(s);
			for (byte b : subnetBytes) {
				withdrawnRoutesBytes.add(b);
			}
		}
		int withdrawnRoutesSize = withdrawnRoutesBytes.size();
		
		
		List<Byte> pathAttributeBytes = new LinkedList<>();
		for (PathAttribute b : pathAttributes) {
			byte[] paBody = b.getTypeBody();
			int paBodyLen = paBody.length;
			
			for (int i = 0; i < paBodyLen; i++) {
				pathAttributeBytes.add(paBody[i]);
			}
		}
		int pathAttributesSize = pathAttributeBytes.size();
		
		List<Byte> NLRIBytes = new LinkedList<>();
		for (Subnet s : NLRI) {
			byte[] subnetBytes = subnetToBytes(s);
			for (byte b : subnetBytes) {
				NLRIBytes.add(b);
			}
		}
		int NLRISize = NLRIBytes.size();
		
		
		int bodyLength = 2				// Withdrawn octet length
				+ withdrawnRoutesSize	// Withdrawn routes
				+ 2						// Path attributes octet length
				+ pathAttributesSize	// Path attributes
				+ NLRISize;				// NLRI
		byte[] body = new byte[bodyLength];
		
		int index = 0;
		
		body[index++] = (byte) (withdrawnRoutesSize >> 8);
		body[index++] = (byte) (withdrawnRoutesSize);
		
		Iterator<Byte> iter = withdrawnRoutesBytes.iterator();
		while (iter.hasNext()) {
			body[index++] = iter.next();
		}
		
		
		body[index++] = (byte) (pathAttributesSize >> 8);
		body[index++] = (byte) (pathAttributesSize);
		
		iter = pathAttributeBytes.iterator();
		while (iter.hasNext()) {
			body[index++] = iter.next();
		}
		
		iter = NLRIBytes.iterator();
		while (iter.hasNext()) {
			body[index++] = iter.next();
		}
		
		return body;
	}
	
	private byte[] subnetToBytes(Subnet s) {
		int bml = s.getBitmaskLength();
		int octetCount = (int)(Math.ceil(bml/8.0));
		long ip = s.getAddress();
		
		byte[] r = new byte[1 + octetCount];
		
		int index = 0;
		
		r[index++] = (byte) bml;
		for (int i = 0; i < octetCount; i++) {
			r[index++] = (byte) (ip >> ((3-i)*8));
		}
		
		return r;
	}
	
	private Subnet bytesToSubnet(byte[] b) {
		long addressBytes = 0;
		
		for (int i = 0; i+1 < b.length; i++) {
			addressBytes += (b[i+1] << (8*(3-i)));
		}
		
		return Subnet.getSubnet(addressBytes, Subnet.getSubnetMask(b[0]));
	}

}
