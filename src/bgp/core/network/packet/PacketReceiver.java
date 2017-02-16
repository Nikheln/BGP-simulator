package bgp.core.network.packet;

import bgp.core.network.Address;

public interface PacketReceiver {
	
	public Address getAddress();
	public void receivePacket(byte[] pkg);
	public long getReceivedPacketCount();

}
