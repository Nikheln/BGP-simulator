package bgp.core;

import java.io.IOException;
import java.util.TimerTask;

import bgp.core.fsm.State;
import bgp.core.fsm.StateMachine;
import bgp.core.messages.KeepaliveMessage;
import bgp.core.messages.OpenMessage;
import bgp.core.network.Address;
import bgp.core.network.InterASInterface;
import bgp.core.network.PacketEngine;

public class ASConnection {
	
	private final BGPRouter handler;
	private final InterASInterface adapter;
	private final StateMachine fsm;
	
	private boolean hasReceivedKeepalive;
	
	private int retryCounter;
	
	private Address neighbourAddress;
	
	public ASConnection(Address ownAddress, BGPRouter handler) {
		this.adapter = new InterASInterface(ownAddress, handler);
		this.handler = handler;
		this.fsm = new StateMachine();
		this.fsm.changeState(State.IDLE);
	}
	
	/**
	 * Start connecting. Initialize the state machine, initialize KEEPALIVE tasks
	 */
	public void start(Address neighbourAddress) {
		this.fsm.changeState(State.CONNECT);
		this.retryCounter = 0;
		sendOpenMessage(neighbourAddress);
	}
	
	public void sendOpenMessage(Address recipient) {
		this.retryCounter++;
		OpenMessage m = new OpenMessage(handler.id,
				SimulatorState.testingMode ? 300 : Consts.DEFAULT_HOLD_DOWN_TIME,
				adapter.getOwnAddress().getAddress());
		byte[] message = PacketEngine.buildPacket(adapter.getOwnAddress(), recipient, m.serialize());
		try {
			adapter.sendData(message);
		} catch (IOException e) {
			if (retryCounter < 10) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
				}
				sendOpenMessage(recipient);
				return;
			}
		}
		fsm.changeState(State.OPEN_SENT);
	}
	
	public void handleOpenMessage(OpenMessage m) {
		if (fsm.getCurrentState().equals(State.OPEN_SENT)
				|| fsm.getCurrentState().equals(State.CONNECT)) {
			neighbourAddress = Address.getAddress(m.getBgpId());
			// Start checking that KEEPALIVE messages have come
			handler.registerKeepaliveTask(new TimerTask() {

				@Override
				public void run() {
					if (hasReceivedKeepalive) {
						if (fsm.getCurrentState().equals(State.OPEN_CONFIRM)) {
							fsm.changeState(State.ESTABLISHED);
						}
						hasReceivedKeepalive = false;
					} else {
						// No KEEPALIVE message received in allotted time
					}
				}
				
			}, m.getHoldTime(), m.getHoldTime());

			// Start sending KEEPALIVE messages
			handler.registerKeepaliveTask(new TimerTask() {

				@Override
				public void run() {
					KeepaliveMessage m = new KeepaliveMessage();
					byte[] packet = PacketEngine.buildPacket(adapter.getOwnAddress(), neighbourAddress, m.serialize());
					try {
						adapter.sendData(packet);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						// Error sending KEEPALIVE
					}
				}
				
			}, 20, SimulatorState.testingMode ? 100 : Consts.DEFAULT_KEEPALIVE_INTERVAL);
			
			fsm.changeState(State.OPEN_CONFIRM);
		}
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
	
	public State getCurrentState() {
		return fsm.getCurrentState();
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
