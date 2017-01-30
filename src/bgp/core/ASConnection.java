package bgp.core;

import bgp.core.fsm.StateMachine;
import bgp.core.network.Address;
import bgp.core.network.InterASInterface;
import bgp.core.network.InterASInterfaceImpl;

public class ASConnection {
	
	private final InterASInterface adapter;
	
	private final StateMachine fsm;
	
	public ASConnection(Address ownAddress, Address neighbourAddress) {
		this.adapter = new InterASInterfaceImpl(ownAddress, neighbourAddress);
		this.fsm = new StateMachine();
	}

}
