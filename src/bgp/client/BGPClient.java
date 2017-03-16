package bgp.client;

import bgp.client.messages.ClientMessage;
import bgp.client.messages.MessageHandlers.Pingable;
import bgp.client.messages.MessageHandlers.Pinger;
import bgp.client.messages.PingRequest;
import bgp.client.messages.PingResponse;
import bgp.core.BGPRouter;
import bgp.core.network.packet.PacketReceiver;
import bgp.core.network.packet.PacketRouter;
import bgp.simulation.Simulator;
import bgp.utils.Address;
import bgp.utils.AddressProvider;
import bgp.utils.PacketEngine;

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
		receivedPacketCount++;
		long sender = PacketEngine.extractSender(pkg);
		byte[] body = PacketEngine.extractBody(pkg);
		ClientMessage cm = ClientMessage.deserialize(body);
		
		if (cm instanceof PingRequest) {
			handlePing(sender, (PingRequest)cm);
		} else if (cm instanceof PingResponse && this instanceof Pinger) {
			((Pinger)this).receivePing((PingResponse)cm);
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
		Simulator.unregisterClient(this);
	}

	@Override
	public void handlePing(long sender, PingRequest p) {
		byte[] token = p.getTokenBytes();

		ph.routePacket(PacketEngine.buildPacket(address.getAddress(), sender, new PingResponse(token).serialize()));
	}

}
