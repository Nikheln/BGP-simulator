package bgp.core.network.packet;

import bgp.utils.Address;

public interface PacketReceiver {
	
	public Address getAddress();
	public void receivePacket(byte[] pkg);
	public long getReceivedPacketCount();
	public void shutdown();

}
