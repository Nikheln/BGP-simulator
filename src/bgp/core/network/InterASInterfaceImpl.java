package bgp.core.network;

public class InterASInterfaceImpl implements InterASInterface {
	
	private final byte[] ownAddress;
	
	public InterASInterfaceImpl(byte[] ownAddress, byte[] neighbourAddress) throws IllegalArgumentException {
		if (ownAddress == null || ownAddress.length != 4) {
			throw new IllegalArgumentException("Own address should not be null and should contain 4 values");
		}
		this.ownAddress = ownAddress;
	}

	@Override
	public void sendData(byte[] content, byte[] recipient) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public byte[] receiveData() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] getOwnAddress() {
		return ownAddress;
	}

}
