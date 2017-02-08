package bgp.core.network.packet;

public interface PacketReceiver {
	
	public void receivePacket(byte[] pkg);
	public long getReceivedPacketCount();

}
