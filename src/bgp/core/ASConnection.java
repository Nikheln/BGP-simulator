package bgp.core;

import bgp.core.fsm.StateMachine;
import bgp.core.network.InterASInterface;
import bgp.core.network.InterASInterfaceImpl;

public class ASConnection {
	
	private final InterASInterface adapter;
	
	private final StateMachine fsm;
	
	public ASConnection(byte[] neighbourAddress) {
		this(GlobalState.getFreeNearbyAddress(neighbourAddress), neighbourAddress);
	}
	
	public ASConnection(byte[] ownAddress, byte[] neighbourAddress) {
		this.adapter = new InterASInterfaceImpl(ownAddress, neighbourAddress);
		this.fsm = new StateMachine();
	}

}
