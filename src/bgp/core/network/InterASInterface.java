package bgp.core.network;

public interface InterASInterface {
	
	public void sendData(byte[] content, byte[] recipient);
	
	public byte[] receiveData();
	
	public byte[] getOwnAddress();

}
