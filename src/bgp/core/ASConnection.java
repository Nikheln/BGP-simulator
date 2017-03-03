package bgp.core;

import java.io.IOException;
import java.util.TimerTask;

import bgp.core.fsm.State;
import bgp.core.fsm.StateMachine;
import bgp.core.messages.KeepaliveMessage;
import bgp.core.messages.NotificationMessage;
import bgp.core.messages.OpenMessage;
import bgp.core.network.Address;
import bgp.core.network.InterASInterface;
import bgp.core.network.PacketEngine;
import bgp.core.trust.TrustEngine;
import bgp.utils.Consts;

public class ASConnection {
	
	private final BGPRouter handler;
	private final InterASInterface adapter;
	private final StateMachine fsm;
	
	private boolean hasReceivedKeepalive;
	
	private int retryCounter;
	
	private TimerTask keepaliveChecking, keepaliveSending;
	
	private int neighbourId;
	private Address neighbourAddress;
	
	public ASConnection(Address ownAddress, BGPRouter handler) {
		this.adapter = new InterASInterface(ownAddress, handler, this);
		this.handler = handler;
		this.fsm = new StateMachine();
		this.fsm.changeState(State.IDLE);
	}
	
	/**
	 * Start connecting. Initialize the state machine and call {@link #sendOpenMessage(Address)}
	 * @param neighbourAddress
	 */
	public void start(Address neighbourAddress) {
		this.fsm.changeState(State.CONNECT);
		this.retryCounter = 0;
		sendOpenMessage(neighbourAddress);
	}
	
	/**
	 * Send an OPEN message to recipient, retry for 10 times,
	 * change state to OPEN_SENT after success
	 * @param recipient
	 */
	public void sendOpenMessage(Address recipient) {
		this.retryCounter++;
		OpenMessage m = new OpenMessage(handler.id,
				Consts.DEFAULT_HOLD_DOWN_TIME,
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
			} else {
				raiseNotification(NotificationMessage.getCeaseError());
			}
		}
		fsm.changeState(State.OPEN_SENT);
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
					byte[] packet = PacketEngine.buildPacket(adapter.getOwnAddress(), neighbourAddress, m.serialize());
					try {
						adapter.sendData(packet);
					} catch (IOException e) {
						// Error sending KEEPALIVE
						raiseNotification(NotificationMessage.getCeaseError());
					}
				}
				
			};

			// Start checking that KEEPALIVE messages have come
			handler.registerKeepaliveTask(keepaliveChecking, 200, m.getHoldTime());
			// Start sending KEEPALIVE messages
			handler.registerKeepaliveTask(keepaliveSending, 20, Consts.DEFAULT_KEEPALIVE_INTERVAL);
			
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
		
		byte[] message = PacketEngine.buildPacket(adapter.getOwnAddress(), neighbourAddress, m.serialize());
		sendPacket(message);
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
		} catch (Exception e) {
			// Failed closing the adapter
		}
		this.keepaliveSending.cancel();
		this.keepaliveChecking.cancel();
		this.fsm.changeState(State.IDLE);
	}

}
