package bgp.core;

import bgp.core.fsm.StateMachine;
import bgp.core.network.Address;
import bgp.core.network.InterASInterface;
import bgp.core.network.packet.PacketRouter;

public class ASConnection {
	
	private final InterASInterface adapter;
	
	private final StateMachine fsm;
	
	public ASConnection(Address ownAddress, Address neighbourAddress, PacketRouter handler) {
		this.adapter = new InterASInterface(ownAddress, neighbourAddress, handler);
		this.fsm = new StateMachine();
	}

}
