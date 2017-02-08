package bgp.core.network;

import bgp.core.network.packet.PacketReceiver;

/**
 * Classes implementing this interface provide a DHCP-like functionality and can serve Addresses from their designated subnet.
 * 
 * @author Niko
 *
 */
public interface AddressProvider {
	public Address reserveAddress(PacketReceiver receiver) throws IllegalArgumentException;
	public void freeAddress(Address address) throws IllegalArgumentException;
}
