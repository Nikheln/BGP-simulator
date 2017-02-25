package bgp.client;

import bgp.core.BGPRouter;
import bgp.core.network.Address;
import bgp.core.network.AddressProvider;
import bgp.core.network.packet.PacketReceiver;
import bgp.core.network.packet.PacketRouter;

public class BGPClient implements PacketReceiver {
	
	private final Address address;
	private final PacketRouter ph;
	private final AddressProvider ap;
	
	private long receivedPacketCount;
	
	public BGPClient(BGPRouter router) {
		this.address = router.reserveAddress(this);
		this.ph = router;
		this.ap = router;
	}
	
	/**
	 * Only increments the packet counter at the moment.
	 */
	@Override
	public void receivePacket(byte[] pkg) {
		receivedPacketCount++;
	}

	@Override
	public long getReceivedPacketCount() {
		return receivedPacketCount;
	}

	@Override
	public Address getAddress() {
		return address;
	}
	
	@Override
	public void shutdown() {
		ap.freeAddress(address);
	}

}
