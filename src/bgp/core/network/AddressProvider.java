package bgp.core.network;

/**
 * Classes implementing this interface provide a DHCP-like functionality and can serve Addresses in their designated subnet.
 * 
 * @author Niko
 *
 */
public interface AddressProvider {
	public Address reserveAddress() throws IllegalArgumentException;
	public void freeAddress(Address address) throws IllegalArgumentException;
}
