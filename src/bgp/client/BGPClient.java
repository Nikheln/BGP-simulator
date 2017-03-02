package bgp.client;

import bgp.client.messages.ClientMessage;
import bgp.client.messages.MessageHandlers.Pingable;
import bgp.client.messages.PingRequest;
import bgp.client.messages.PingResponse;
import bgp.core.BGPRouter;
import bgp.core.network.Address;
import bgp.core.network.AddressProvider;
import bgp.core.network.PacketEngine;
import bgp.core.network.packet.PacketReceiver;
import bgp.core.network.packet.PacketRouter;

public class BGPClient implements PacketReceiver, Pingable {
	
	protected final Address address;
	protected final PacketRouter ph;
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
		long sender = PacketEngine.extractSender(pkg);
		byte[] body = PacketEngine.extractBody(pkg);
		ClientMessage cm = ClientMessage.deserialize(body);
		
		if (cm instanceof PingRequest) {
			handlePing(sender, (PingRequest)cm);
		}
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

	@Override
	public void handlePing(long sender, PingRequest p) {
		byte[] token = p.getTokenBytes();
		byte[] packet = PacketEngine.buildPacket(address.getAddress(), sender, new PingResponse(token).serialize());
		
		ph.routePacket(packet);
	}

}
