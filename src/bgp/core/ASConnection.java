package bgp.core;

import java.io.IOException;
import java.util.TimerTask;

import bgp.core.fsm.StateMachine;
import bgp.core.messages.KeepaliveMessage;
import bgp.core.network.Address;
import bgp.core.network.InterASInterface;
import bgp.core.network.PacketProcessor;

public class ASConnection {
	
	private BGPRouter handler;
	
	
	private InterASInterface adapter;
	
	private StateMachine fsm;
	
	private boolean hasReceivedKeepalive;
	
	private int retryCounter;
	
	private Address neighbourAddress;
	
	public ASConnection(Address ownAddress, BGPRouter handler) {
		this.adapter = new InterASInterface(ownAddress, handler);
		this.handler = handler;
	}
	
	/**
	 * Start connecting. Initialize the state machine, initialize KEEPALIVE tasks
	 */
	public void start() {
		this.fsm = new StateMachine();
		this.retryCounter = 0;
		
		// Start checking that KEEPALIVE messages have come
		handler.registerKeepaliveTask(new TimerTask() {

			@Override
			public void run() {
				if (hasReceivedKeepalive) {
					hasReceivedKeepalive = false;
				} else {
					// No KEEPALIVE message received in allotted time
				}
			}
			
		}, Consts.DEFAULT_HOLD_DOWN_TIME);
		
		// Start sending KEEPALIVE messages
		handler.registerKeepaliveTask(new TimerTask() {

			@Override
			public void run() {
				KeepaliveMessage m = new KeepaliveMessage();
				byte[] packet = PacketProcessor.buildPacket(adapter.getOwnAddress(), neighbourAddress, m.serialize());
				handler.routePacket(packet);
			}
			
		}, Consts.DEFAULT_KEEPALIVE_INTERVAL);
	}
	
	public void setNeighbourAddress(Address address) {
		this.neighbourAddress = address;
	}
	
	public Address getNeighbourAddress() { 
		return neighbourAddress;
	}
	
	public void raiseKeepaliveFlag() {
		this.hasReceivedKeepalive = true;
	}
	
	protected InterASInterface getAdapter() {
		return adapter;
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
