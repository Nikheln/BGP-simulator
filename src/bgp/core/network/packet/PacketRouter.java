package bgp.core.network.packet;

import bgp.core.network.ASConnection;

/**
 * Classes implementing this interface should be able to process IPv4 packages sent to them via {@link #routePacket(byte[])}.
 * @author Niko
 *
 */
public interface PacketRouter {
	/**
	 * Route an IPv4 package to the correct direction. Should be done in a separate thread.
	 * 
	 * @param pkg
	 */
	public void routePacket(byte[] pkg, ASConnection receivingConnection);
	
	public void routePacket(byte[] pkg);
}
