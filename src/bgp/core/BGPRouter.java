package bgp.core;

public class BGPRouter implements PackageHandler {
	
	public final long id;
	
	public BGPRouter(long id) {
		this.id = id;
	}

	@Override
	public void processPackage(byte[] pkg) {
		// TODO Auto-generated method stub
		
	}

}
