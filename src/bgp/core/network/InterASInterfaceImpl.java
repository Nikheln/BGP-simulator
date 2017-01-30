package bgp.core.network;

public class InterASInterfaceImpl implements InterASInterface {
	
	private final Address ownAddress;
	
	public InterASInterfaceImpl(Address ownAddress, Address neighbourAddress) throws IllegalArgumentException {
		if (ownAddress == null || neighbourAddress == null) {
			throw new IllegalArgumentException("Neither address can be null!");
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
	public Address getOwnAddress() {
		return ownAddress;
	}

}
