package bgp.core;

import java.io.IOException;
import java.util.TimerTask;

import bgp.core.fsm.StateMachine;
import bgp.core.network.Address;
import bgp.core.network.InterASInterface;
import bgp.core.network.packet.PacketRouter;

public class ASConnection {
	
	private final InterASInterface adapter;
	
	private final StateMachine fsm;
	
	private boolean hasReceivedKeepalive;
	
	public ASConnection(Address ownAddress, Address neighbourAddress, BGPRouter handler) {
		this.adapter = new InterASInterface(ownAddress, neighbourAddress, handler);
		this.fsm = new StateMachine();
		
		handler.registerKeepaliveTask(new TimerTask() {

			@Override
			public void run() {
				if (hasReceivedKeepalive) {
					hasReceivedKeepalive = false;
				} else {
					// No KEEPALIVE message received in allotted time
				}
			}
			
		}, Consts.DEFAULT_KEEPALIVE_INTERVAL);
	}
	
	public void raiseKeepaliveFlag() {
		this.hasReceivedKeepalive = true;
	}
	
	protected void sendPacket(byte[] packet) {
		try {
			adapter.sendData(packet);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
