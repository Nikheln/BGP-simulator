package bgp.core.packet;

public interface PacketReceiver {
	
	public void receivePacket(byte[] pkg);
	public long getReceivedPacketCount();

}
