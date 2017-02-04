package bgp.core;

import java.util.ArrayList;
import java.util.List;

import bgp.core.network.Address;
import bgp.core.network.AddressProvider;

public class BGPRouter implements PackageHandler, AddressProvider {
	
	public final int id;
	
	private final List<ASConnection> connections;
	
	public BGPRouter(int id) {
		this.id = id;
		this.connections = new ArrayList<>();
	}

	@Override
	public void processPackage(byte[] pkg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Address reserveAddress() throws IllegalArgumentException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void freeAddress(Address address) throws IllegalArgumentException {
		// TODO Auto-generated method stub
		
	}

}
