package bgp.core;

import java.util.ArrayList;
import java.util.List;

public class BGPRouter implements PackageHandler {
	
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

}
