package bgp.core.network;

import java.io.IOException;
import java.util.TimerTask;

import bgp.core.BGPRouter;
import bgp.core.messages.KeepaliveMessage;
import bgp.core.messages.NotificationMessage;
import bgp.core.messages.OpenMessage;
import bgp.core.network.fsm.State;
import bgp.core.network.fsm.StateMachine;
import bgp.core.trust.TrustEngine;
import bgp.simulation.SimulatorState;
import bgp.utils.Address;
import bgp.utils.Consts;
import bgp.utils.PacketEngine;

public class ASConnection {
	
	private final BGPRouter handler;
	private final InterRouterInterface adapter;
	private final StateMachine fsm;
	
	private boolean hasReceivedKeepalive;
	
	private int retryCounter;
	
	private TimerTask keepaliveChecking, keepaliveSending, retrying;
	
	private int neighbourId;
	private Address ownAddress;
	private Address neighbourAddress;
	
	public ASConnection(Address ownAddress, BGPRouter handler) {
		if (ownAddress == null) {
			throw new IllegalArgumentException("Address can not be null!");
		}
		if (!SimulatorState.isAddressFree(ownAddress)) {
			throw new IllegalStateException("Own address is already reserved");
		}
		
		SimulatorState.reserveAddress(ownAddress);
		
		
		this.adapter = new InterRouterInterface(handler, this);
		this.handler = handler;
		this.fsm = new StateMachine();
		this.fsm.changeState(State.IDLE);
		this.ownAddress = ownAddress;
	}
	
	/**
	 * Start connecting. Initialize the state machine and call {@link #startSendingOpenMessages(Address)}
	 * @param neighbourAddress
	 */
	public void start(Address neighbourAddress) {
		this.fsm.changeState(State.CONNECT);
		this.retryCounter = 0;

		this.retrying = new TimerTask() {
			
			@Override
			public void run() {
				if (retryCounter < 10
						&& (fsm.getCurrentState() == State.OPEN_SENT
						 || fsm.getCurrentState() == State.CONNECT)) {
					retryCounter++;
					OpenMessage m = new OpenMessage(handler.id,
							Consts.DEFAULT_HOLD_DOWN_TIME,
							ownAddress.getAddress());
					byte[] message = PacketEngine.buildPacket(ownAddress, neighbourAddress, m.serialize());
					
					sendPacket(message);
				} else if (retryCounter >= 10) {
					retrying.cancel();
					closeConnection();
				}
			}
		};
		
		fsm.changeState(State.OPEN_SENT);
		
		handler.registerKeepaliveTask(retrying, 0, 10000);
		
	}
	
	/**
	 * Handle a received OPEN message: start sending and checking
	 * for KEEPALIVE messages and change state to OPEN_CONFIRM
	 * @param m
	 */
	public void handleOpenMessage(OpenMessage m) {
		if (fsm.getCurrentState().equals(State.OPEN_SENT)
				|| fsm.getCurrentState().equals(State.CONNECT)) {
			neighbourId = m.getASId();
			neighbourAddress = Address.getAddress(m.getBgpId());
			this.keepaliveChecking = new TimerTask() {

				@Override
				public void run() {
					if (hasReceivedKeepalive) {
						if (fsm.getCurrentState().equals(State.OPEN_CONFIRM)) {
							fsm.changeState(State.ESTABLISHED);
							retrying.cancel();
							handler.sendRoutingInformation(neighbourId);
						}
						hasReceivedKeepalive = false;
					} else {
						raiseNotification(NotificationMessage.getHoldTimeExpiredError());
					}
				}
				
			};
			
			this.keepaliveSending = new TimerTask() {

				@Override
				public void run() {
					KeepaliveMessage m = new KeepaliveMessage();
					byte[] packet = PacketEngine.buildPacket(ownAddress, neighbourAddress, m.serialize());
					try {
						adapter.sendData(packet);
					} catch (IOException e) {
						e.printStackTrace();
						// Error sending KEEPALIVE
						raiseNotification(NotificationMessage.getCeaseError());
					}
				}
				
			};

			// Start sending KEEPALIVE messages
			handler.registerKeepaliveTask(keepaliveSending, 20, Consts.DEFAULT_KEEPALIVE_INTERVAL);
			// Start checking that KEEPALIVE messages have come
			handler.registerKeepaliveTask(keepaliveChecking, 500, m.getHoldTime()*1000);
			
			fsm.changeState(State.OPEN_CONFIRM);
		}
	}
	
	public Address getNeighbourAddress() { 
		return neighbourAddress;
	}
	
	public void raiseKeepaliveFlag() {
		this.hasReceivedKeepalive = true;
	}
	
	public boolean isKeepaliveReceived() {
		return hasReceivedKeepalive;
	}
	
	public Address getOwnAddress() {
		return ownAddress;
	}
	
	public State getCurrentState() {
		return fsm.getCurrentState();
	}
	
	public InterRouterInterface getAdapter() {
		return adapter;
	}
	
	public void sendPacket(byte[] packet) {
		try {
			adapter.sendData(packet);
		} catch (IOException e) {
		}
	}
	
	/**
	 * Send a NOTIFICATION message to other end,
	 * possibly change its direct trust, and close the connection
	 * 
	 * @param m
	 */
	public void raiseNotification(NotificationMessage m) {
		// Penalize neighbour in some error cases
		TrustEngine t = handler.getTrustEngine();
		switch (m.getErrorType()) {
		case HOLD_TIMER_EXPIRED:
			t.changeDirectTrust(neighbourId, -10);
			break;
		case MESSAGE_HEADER_ERROR:
			t.changeDirectTrust(neighbourId, -15);
			break;
		case UPDATE_MESSAGE_ERROR:
			t.changeDirectTrust(neighbourId, -20);
			break;
		default:
			break;
		}
		try {
			byte[] message = PacketEngine.buildPacket(ownAddress, neighbourAddress, m.serialize());
			sendPacket(message);
		} catch (Exception e) {
			
		}
		closeConnection();
	}
	
	/**
	 * Close the connection: shut down the adapter,
	 * stop sending and checking for KEEPALIVEs and change state to IDLE,
	 * tell the router to stop using this adapter
	 */
	public void closeConnection() {
		handler.removeConnection(this);
		try {
			adapter.close();
			SimulatorState.releaseAddress(ownAddress);
		} catch (Exception e) {
			// Failed closing the adapter or freeing the address, might be already down
		}
		
		if (this.keepaliveChecking != null) {
			this.keepaliveChecking.cancel();
		}
		
		if (this.keepaliveSending != null) {
			this.keepaliveSending.cancel();
		}
		
		this.fsm.changeState(State.IDLE);
	}

}
